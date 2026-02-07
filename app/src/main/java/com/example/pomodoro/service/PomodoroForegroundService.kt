package com.example.pomodoro.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.pomodoro.data.datastore.AudioSettingsDataStore
import com.example.pomodoro.data.datastore.SettingsDataStore
import com.example.pomodoro.data.datastore.StatsDataStore
import com.example.pomodoro.data.local.TaskDatabase
import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.domain.logic.PomodoroEngine
import com.example.pomodoro.util.AudioPlayerManager
import com.example.pomodoro.domain.model.PomodoroMode
import com.example.pomodoro.domain.model.Settings
import com.example.pomodoro.domain.model.UiState
import com.example.pomodoro.notification.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

import com.example.pomodoro.data.repository.StatsRepository

class PomodoroForegroundService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var statsDataStore: StatsDataStore
    private lateinit var audioSettingsDataStore: AudioSettingsDataStore
    private lateinit var audioPlayerManager: AudioPlayerManager
    private val pomodoroEngine = PomodoroEngine()
    
    // Repository
    private lateinit var statsRepository: StatsRepository

    // State
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentSettings = Settings()
    private var endTimeMillis: Long = 0L
    private var isBound = false
    
    private var taskPlayer: android.media.MediaPlayer? = null
    private var isTaskAudioPlaying = false
    
    // Tarea activa con configuración propia
    private var activeTask: TaskEntity? = null
    private lateinit var taskDatabase: TaskDatabase

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        settingsDataStore = SettingsDataStore(this)
        statsDataStore = StatsDataStore(this)
        audioSettingsDataStore = AudioSettingsDataStore(this)
        audioPlayerManager = AudioPlayerManager(this)
        taskDatabase = TaskDatabase.getInstance(this)
        statsRepository = StatsRepository(taskDatabase.statsDao, taskDatabase.taskDao) // Init Repository

        startObservingSettings()
        startObservingStats()
        startObservingAudioSettings()
    }

    private fun startObservingSettings() {
        scope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                currentSettings = settings
                // If IDLE (total time is 1 which is default init), update duration
                // Or if we need to react to changes. 
                // For simplicity, we update totalTime if not running or if just starting.
                if (!_uiState.value.isRunning && _uiState.value.currentTimeMillis == 0L) {
                     val duration = pomodoroEngine.getDurationMillis(_uiState.value.mode, settings)
                     _uiState.update { it.copy(totalTimeMillis = duration, currentTimeMillis = duration) }
                }
            }
        }
    }

    private fun startObservingStats() {
        scope.launch {
            statsDataStore.statsFlow.collect { stats ->
                _uiState.update { it.copy(sessionsToday = stats.sessionsToday, sessionsTotal = stats.sessionsTotal) }
            }
        }
    }

    private fun startObservingAudioSettings() {
        scope.launch {
            audioSettingsDataStore.audioSettingsFlow.collect { audioSettings ->
                // Find custom track URI if selected track is custom
                val customUri = if (audioSettings.selectedTrackId.startsWith("custom_")) {
                    audioSettings.customTracks.find { it.id == audioSettings.selectedTrackId }?.uri
                } else null
                
                audioPlayerManager.playTrack(
                    trackId = audioSettings.selectedTrackId,
                    customUri = customUri,
                    volume = audioSettings.volume,
                    isMuted = audioSettings.isMuted
                )
                // If not running or music disabled, ensure we pause/stop according to state
                if (!_uiState.value.isRunning || !audioSettings.isMusicEnabled) {
                     audioPlayerManager.pause()
                } else if (_uiState.value.isRunning && audioSettings.isMusicEnabled) {
                     audioPlayerManager.resume()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer() // Same as start mainly
            ACTION_STOP -> stopTimer()
            ACTION_RESET -> resetTimer()
            ACTION_NEXT -> nextMode()
            ACTION_TOGGLE_AUDIO -> toggleTaskAudio()
        }
        return START_NOT_STICKY
    }

    fun setActiveTask(title: String?, audioUri: String?) {
        _uiState.update { it.copy(activeTaskTitle = title, activeTaskAudioUri = audioUri) }
        stopTaskAudio()
    }

    /**
     * Establece una tarea con configuración Pomodoro propia
     */
    fun setActiveTaskWithConfig(task: TaskEntity) {
        activeTask = task
        
        // Actualizar UI state con información de la tarea
        val duration = pomodoroEngine.getDurationMillisForTask(PomodoroMode.Focus, task)
        _uiState.update { 
            it.copy(
                activeTaskId = task.id,
                activeTaskTitle = task.title,
                activeTaskAudioUri = task.audioUri,
                activeTaskTotalPomodoros = task.totalPomodoros,
                activeTaskCompletedPomodoros = task.completedPomodoros,
                mode = PomodoroMode.Focus,
                currentCycle = 1,
                currentTimeMillis = duration,
                totalTimeMillis = duration
            )
        }
        stopTaskAudio()
    }

    /**
     * Limpia la tarea activa
     */
    fun clearActiveTask() {
        activeTask = null
        _uiState.update { 
            it.copy(
                activeTaskId = null,
                activeTaskTitle = null,
                activeTaskAudioUri = null,
                activeTaskTotalPomodoros = 0,
                activeTaskCompletedPomodoros = 0
            )
        }
        stopTaskAudio()
    }

    private fun updateAudioStatus(playing: Boolean) {
        _uiState.update { it.copy(isTaskAudioPlaying = playing) }
    }

    fun toggleTaskAudio() {
        val audioUri = _uiState.value.activeTaskAudioUri
        if (audioUri == null) return

        if (isTaskAudioPlaying) {
            pauseTaskAudio()
        } else {
            startTaskAudio(audioUri)
        }
    }

    private fun startTaskAudio(uriPath: String) {
        try {
            stopTaskAudio()
            taskPlayer = android.media.MediaPlayer().apply {
                setDataSource(uriPath)
                isLooping = true
                prepare()
                start()
                updateAudioStatus(true)
            }
            isTaskAudioPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pauseTaskAudio() {
        taskPlayer?.pause()
        isTaskAudioPlaying = false
        updateAudioStatus(false)
    }

    private fun stopTaskAudio() {
        taskPlayer?.stop()
        taskPlayer?.release()
        taskPlayer = null
        isTaskAudioPlaying = false
        updateAudioStatus(false)
    }

    private fun startTimer() {
        if (_uiState.value.isRunning) return

        // Calculate endTime
        val durationRemaining = if (_uiState.value.currentTimeMillis > 0) _uiState.value.currentTimeMillis else pomodoroEngine.getDurationMillis(_uiState.value.mode, currentSettings)
        endTimeMillis = System.currentTimeMillis() + durationRemaining

        _uiState.update { it.copy(isRunning = true, totalTimeMillis = if (it.totalTimeMillis <= 1L) durationRemaining else it.totalTimeMillis) }

        startForegroundService()
        startTick()
        
        // Start Music
        scope.launch {
            val audioSettings = audioSettingsDataStore.audioSettingsFlow.first()
            if (audioSettings.isMusicEnabled) {
                 audioPlayerManager.resume()
            }
        }
    }

    private fun resumeTimer() {
        startTimer()
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
        updateNotification()
        // Here we could persist state to DataStore for process death survival
        audioPlayerManager.pause()
    }

    private fun resetTimer() {
        pauseTimer()
        val duration = if (activeTask != null) {
            pomodoroEngine.getDurationMillisForTask(_uiState.value.mode, activeTask!!)
        } else {
            pomodoroEngine.getDurationMillis(_uiState.value.mode, currentSettings)
        }
        _uiState.update { it.copy(currentTimeMillis = duration, totalTimeMillis = duration) }
        updateNotification()
        audioPlayerManager.stop()
    }

    private fun nextMode() {
        pauseTimer()
        
        val (nextMode, nextCycle) = if (activeTask != null) {
            pomodoroEngine.calculateNextModeForTask(
                _uiState.value.mode,
                _uiState.value.currentCycle,
                activeTask!!
            )
        } else {
            pomodoroEngine.calculateNextMode(
                _uiState.value.mode,
                _uiState.value.currentCycle,
                currentSettings.longBreakEveryNCycles
            )
        }
        
        val duration = if (activeTask != null) {
            pomodoroEngine.getDurationMillisForTask(nextMode, activeTask!!)
        } else {
            pomodoroEngine.getDurationMillis(nextMode, currentSettings)
        }
        
        _uiState.update { 
            it.copy(
                mode = nextMode, 
                currentCycle = nextCycle,
                currentTimeMillis = duration,
                totalTimeMillis = duration
            ) 
        }
        updateNotification()
        
        val shouldAutoStart = activeTask?.taskAutoStartNext ?: currentSettings.autoStartNext
        if (shouldAutoStart) {
            startTimer()
        }
    }

    private fun stopTimer() {
        pauseTimer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTick() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val remaining = endTimeMillis - System.currentTimeMillis()
                if (remaining <= 0) {
                    onTimerFinished()
                    break
                }
                _uiState.update { it.copy(currentTimeMillis = remaining) }
                updateNotification()
                delay(1000) // Update every second for notification, UI collects flow
            }
        }
    }

    private fun onTimerFinished() {
        _uiState.update { it.copy(isRunning = false, currentTimeMillis = 0) }
        notificationHelper.showCompleteNotification(_uiState.value.mode)
        
        // Update stats if it was Focus mode
        if (_uiState.value.mode == PomodoroMode.Focus) {
            scope.launch {
                statsDataStore.recordSession()
                
                // Record strict minutes based on configuration
                val minutesFocused = activeTask?.focusMinutes ?: currentSettings.focusDurationMinutes
                statsRepository.recordFocusSession(minutesFocused)
                
                // Incrementar progreso de tarea si existe
                activeTask?.let { task ->
                    val updatedPomodoros = task.completedPomodoros + 1
                    val updatedTask = task.copy(completedPomodoros = updatedPomodoros)
                    activeTask = updatedTask
                    
                    // Actualizar en Room
                    taskDatabase.taskDao.updateTask(updatedTask)
                    
                    // Actualizar UI state
                    _uiState.update { 
                        it.copy(activeTaskCompletedPomodoros = updatedPomodoros) 
                    }
                    
                    // Verificar si tarea completada
                    if (updatedPomodoros >= task.totalPomodoros) {
                        // Marcar tarea como completada
                        val completedTask = updatedTask.copy(
                            isCompleted = true,
                            completedAt = System.currentTimeMillis()
                        )
                        taskDatabase.taskDao.updateTask(completedTask)
                        
                        // Notificar tarea completada
                        notificationHelper.showTaskCompleteNotification(task.title)
                        
                        // Registrar en stats
                        statsRepository.recordTaskCompletion()
                        
                        // Limpiar tarea activa
                        clearActiveTask()
                        return@launch
                    }
                }
            }
        }

        // Auto transition to next mode
        nextMode() 
    }

    private fun startForegroundService() {
        val notification = notificationHelper.buildTimerNotification(
            _uiState.value.mode,
            formatTime(_uiState.value.currentTimeMillis),
            _uiState.value.isRunning
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, NotificationHelper.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        if (!isBound && !_uiState.value.isRunning) return // Don't spam notifications if paused and UI is open? Actually we want persistance.
        // If the service is running in foreground, we SHOULD update.
        
        val notification = notificationHelper.buildTimerNotification(
             _uiState.value.mode,
             formatTime(_uiState.value.currentTimeMillis),
             _uiState.value.isRunning
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / 1000) / 60
        return "%02d:%02d".format(minutes, seconds)
    }

    override fun onBind(intent: Intent): IBinder {
        isBound = true
        return binder
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        isBound = true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        // If timer is not running, we can stop service to save resources? 
        // Or keep it alive for state? User wants persistence.
        // If running, strictly keep alive. 
        // If paused, we can decide.
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        audioPlayerManager.release()
    }

    inner class LocalBinder : Binder() {
        fun getService(): PomodoroForegroundService = this@PomodoroForegroundService
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
