package com.example.pomodoro.features.timer.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.pomodoro.core.audio.AudioSettingsDataStore
import com.example.pomodoro.core.feedback.SessionFeedback
import com.example.pomodoro.features.timer.data.SettingsRepository
import kotlinx.coroutines.flow.first
import com.example.pomodoro.features.timer.domain.PomodoroSessionController
import com.example.pomodoro.features.timer.domain.SessionEvent
import com.example.pomodoro.features.timer.domain.UiState
import com.example.pomodoro.core.notification.NotificationHelper
import com.example.pomodoro.core.audio.AudioPlayerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PomodoroService"

/**
 * Mantiene viva la sesión en primer plano y traduce el estado del dominio a efectos de
 * Android: notificación persistente, música de fondo y audio de la tarea.
 *
 * La máquina de estados vive en [PomodoroSessionController]; este Service no decide
 * nada sobre modos, ciclos ni estadísticas.
 */
@AndroidEntryPoint
class PomodoroForegroundService : Service() {

    @Inject lateinit var controller: PomodoroSessionController
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var audioPlayerManager: AudioPlayerManager
    @Inject lateinit var audioSettingsDataStore: AudioSettingsDataStore
    @Inject lateinit var sessionFeedback: SessionFeedback
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var taskPlayer: MediaPlayer? = null
    private var isTaskAudioPlaying = false

    override fun onCreate() {
        super.onCreate()
        // Se entra en primer plano de inmediato: al arrancarse con startForegroundService()
        // el sistema exige startForeground() dentro de unos segundos.
        startForegroundWithState(controller.state.value)

        observeStateForNotification()
        observeEvents()
        observeBackgroundMusic()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START, ACTION_RESUME -> controller.start()
            ACTION_PAUSE -> controller.pause()
            ACTION_RESET -> controller.reset()
            ACTION_NEXT -> controller.nextMode()
            ACTION_STOP -> stopSession()
            ACTION_TOGGLE_AUDIO -> toggleTaskAudio()
            // ACTION_UPDATE_AUDIO_SETTINGS no necesita acción: observeBackgroundMusic() ya reacciona.
        }
        return START_NOT_STICKY
    }

    // --- Observadores ---

    private fun observeStateForNotification() {
        scope.launch {
            controller.state.collect { state -> updateNotification(state) }
        }
    }

    private fun observeEvents() {
        scope.launch {
            controller.events.collect { event ->
                // Los avisos háptico y sonoro respetan los interruptores de Ajustes.
                val settings = settingsRepository.settingsFlow.first()

                when (event) {
                    is SessionEvent.TimerFinished -> {
                        notificationHelper.showCompleteNotification(event.mode)
                        sessionFeedback.onIntervalFinished(
                            vibrationEnabled = settings.vibrationEnabled,
                            soundEnabled = settings.soundEnabled
                        )
                    }

                    is SessionEvent.TaskCompleted -> {
                        notificationHelper.showTaskCompleteNotification(event.taskTitle)
                        sessionFeedback.onTaskCompleted(
                            vibrationEnabled = settings.vibrationEnabled,
                            soundEnabled = settings.soundEnabled
                        )
                        stopTaskAudio()
                    }
                }
            }
        }
    }

    private fun observeBackgroundMusic() {
        scope.launch {
            combine(
                audioSettingsDataStore.audioSettingsFlow,
                controller.state.map { it.isRunning }.distinctUntilChanged()
            ) { audioSettings, isRunning -> audioSettings to isRunning }
                .collect { (audioSettings, isRunning) ->
                    val customUri = if (audioSettings.selectedTrackId.startsWith("custom_")) {
                        audioSettings.customTracks
                            .find { it.id == audioSettings.selectedTrackId }?.uri
                    } else {
                        null
                    }

                    audioPlayerManager.playTrack(
                        trackId = audioSettings.selectedTrackId,
                        customUri = customUri,
                        volume = audioSettings.volume,
                        isMuted = audioSettings.isMuted
                    )

                    if (isRunning && audioSettings.isMusicEnabled) {
                        audioPlayerManager.resume()
                    } else {
                        audioPlayerManager.pause()
                    }
                }
        }
    }

    // --- Audio de la tarea ---

    private fun toggleTaskAudio() {
        val audioUri = controller.state.value.activeTaskAudioUri ?: return
        if (isTaskAudioPlaying) pauseTaskAudio() else startTaskAudio(audioUri)
    }

    private fun startTaskAudio(uriPath: String) {
        try {
            stopTaskAudio()
            taskPlayer = MediaPlayer().apply {
                setDataSource(uriPath)
                isLooping = true
                prepare()
                start()
            }
            isTaskAudioPlaying = true
            controller.setTaskAudioPlaying(true)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo reproducir el audio de la tarea", e)
        }
    }

    private fun pauseTaskAudio() {
        taskPlayer?.pause()
        isTaskAudioPlaying = false
        controller.setTaskAudioPlaying(false)
    }

    private fun stopTaskAudio() {
        taskPlayer?.stop()
        taskPlayer?.release()
        taskPlayer = null
        isTaskAudioPlaying = false
        controller.setTaskAudioPlaying(false)
    }

    // --- Notificación ---

    private fun startForegroundWithState(state: UiState) {
        val notification = notificationHelper.buildTimerNotification(
            state.mode,
            formatTime(state.currentTimeMillis),
            state.isRunning
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(state: UiState) {
        val notification = notificationHelper.buildTimerNotification(
            state.mode,
            formatTime(state.currentTimeMillis),
            state.isRunning
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / 1000) / 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun stopSession() {
        controller.pause()
        stopTaskAudio()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopTaskAudio()
        scope.cancel()
        audioPlayerManager.release()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RESET = "ACTION_RESET"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_TOGGLE_AUDIO = "ACTION_TOGGLE_AUDIO"
        const val ACTION_UPDATE_AUDIO_SETTINGS = "ACTION_UPDATE_AUDIO_SETTINGS"
    }
}
