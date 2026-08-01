package com.example.pomodoro.features.timer.presentation

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.core.audio.AudioSettings
import com.example.pomodoro.core.audio.AudioSettingsDataStore
import com.example.pomodoro.features.timer.data.SettingsDataStore
import com.example.pomodoro.core.audio.CustomTrack
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskRepository
import com.example.pomodoro.features.timer.domain.PomodoroSessionController
import com.example.pomodoro.features.timer.domain.PomodoroMode
import com.example.pomodoro.features.timer.domain.Settings
import com.example.pomodoro.features.timer.domain.UiState
import com.example.pomodoro.features.timer.service.PomodoroForegroundService
import com.example.pomodoro.core.audio.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

private const val TAG = "PomodoroViewModel"

data class ActiveTaskUiState(
    val hasActiveTask: Boolean = false,
    val title: String = "",
    val course: String? = null,
    val mode: PomodoroMode = PomodoroMode.Focus,
    val isRunning: Boolean = false,
    val completedPomodoros: Int = 0,
    val totalPomodoros: Int = 0,
    val secondsLeft: Long = 0,
    val statusText: String = "",
    val progress: Float = 0f,
    val etaString: String? = null
)

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    application: Application,
    private val controller: PomodoroSessionController,
    private val taskRepository: TaskRepository,
    private val audioSettingsDataStore: AudioSettingsDataStore,
    private val settingsDataStore: SettingsDataStore,
    audioPlayerManager: AudioPlayerManager
) : AndroidViewModel(application) {

    /** El estado vive en el controller (singleton); el ViewModel solo lo expone. */
    val uiState: StateFlow<UiState> = controller.state

    /** Errores que la interfaz debe mostrar al usuario (import de audio, fondo, etc.). */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Ajustes globales expuestos a la UI: las pantallas ya no instancian el DataStore. */
    val settings: StateFlow<Settings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    // Derived UI State for Active Task Header
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeTaskUiState: StateFlow<ActiveTaskUiState> = uiState
        .map { it.activeTaskId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id != null) taskRepository.getTaskByIdFlow(id) else flowOf(null)
        }
        .combine(uiState) { task, state ->
            val hasTask = state.activeTaskId != null
            val title = task?.title ?: state.activeTaskTitle ?: ""
            val course = task?.courseOrProject

            val eta = if (task?.endDateTimeMillis != null &&
                task.endDateTimeMillis > System.currentTimeMillis()
            ) {
                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                "Fin: " + dateFormat.format(Date(task.endDateTimeMillis))
            } else {
                null
            }

            val modeText = when (state.mode) {
                PomodoroMode.Focus -> "ENFOQUE"
                PomodoroMode.ShortBreak -> "DESCANSO CORTO"
                PomodoroMode.LongBreak -> "DESCANSO LARGO"
            }

            val statusText = if (hasTask) {
                when (state.mode) {
                    PomodoroMode.Focus -> {
                        if (!state.isRunning) {
                            "En pausa • Pomodoro ${state.activeTaskCompletedPomodoros + 1}/${state.activeTaskTotalPomodoros}"
                        } else {
                            "Pomodoro ${state.activeTaskCompletedPomodoros + 1}/${state.activeTaskTotalPomodoros} • $modeText"
                        }
                    }
                    else -> {
                        val min = (state.currentTimeMillis / 1000) / 60
                        val sec = (state.currentTimeMillis / 1000) % 60
                        val timeStr = "%02d:%02d".format(min, sec)
                        "$modeText • faltan $timeStr"
                    }
                }
            } else {
                "Sin tarea activa"
            }

            ActiveTaskUiState(
                hasActiveTask = hasTask,
                title = title,
                course = course,
                mode = state.mode,
                isRunning = state.isRunning,
                completedPomodoros = state.activeTaskCompletedPomodoros,
                totalPomodoros = state.activeTaskTotalPomodoros,
                secondsLeft = state.currentTimeMillis / 1000,
                statusText = statusText,
                progress = state.taskProgress,
                etaString = eta
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveTaskUiState())

    val audioSettings: StateFlow<AudioSettings> = audioSettingsDataStore.audioSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioSettings())

    // Predefined tracks from raw resources
    private val predefinedTracks = audioPlayerManager.getAvailableTracks()

    init {
        // Arranca el Service para que la sesión sobreviva en primer plano.
        startService(null)
    }

    fun consumeErrorMessage() {
        _errorMessage.value = null
    }

    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            settingsDataStore.updateSettings(settings)
        }
    }

    // Combined list: predefined + custom tracks
    fun getAllTracks(customTracks: List<CustomTrack>): List<Pair<String, String>> {
        val combined = predefinedTracks.toMutableList()
        customTracks.forEach { custom ->
            combined.add(custom.id to custom.name)
        }
        return combined
    }

    // --- Control del temporizador ---
    // Se envía por Intent para garantizar que el Service esté en primer plano
    // antes de que el temporizador avance; el Service delega en el controller.

    fun toggleTimer() {
        startService(
            if (uiState.value.isRunning) PomodoroForegroundService.ACTION_PAUSE
            else PomodoroForegroundService.ACTION_RESUME
        )
    }

    fun nextMode() = startService(PomodoroForegroundService.ACTION_NEXT)

    fun toggleTaskAudio() = startService(PomodoroForegroundService.ACTION_TOGGLE_AUDIO)

    // --- Tarea activa ---
    // Van directas al controller: antes se perdían si el Service aún no estaba enlazado.

    fun setActiveTaskWithConfig(task: TaskEntity) {
        controller.setActiveTaskWithConfig(task)
    }

    fun clearActiveTask() {
        controller.clearActiveTask()
    }

    // --- Background Music Controls ---

    fun setMusicEnabled(enabled: Boolean) {
        viewModelScope.launch { audioSettingsDataStore.updateMusicEnabled(enabled) }
    }

    fun setMuted(muted: Boolean) {
        viewModelScope.launch { audioSettingsDataStore.updateMuted(muted) }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch { audioSettingsDataStore.updateVolume(volume) }
    }

    fun selectTrack(trackId: String) {
        viewModelScope.launch { audioSettingsDataStore.updateSelectedTrack(trackId) }
    }

    fun importAudio(uri: Uri) {
        viewModelScope.launch {
            try {
                // Persist permissions
                val contentResolver = getApplication<Application>().contentResolver
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                val name = getFileName(uri) ?: "Audio Importado"
                val id = "custom_${UUID.randomUUID()}"

                audioSettingsDataStore.addCustomTrack(CustomTrack(id, name, uri.toString()))

                // Auto-select
                selectTrack(id)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo importar el audio $uri", e)
                _errorMessage.value = "No se pudo importar el audio seleccionado."
            }
        }
    }

    fun updateBackground(uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (uri == null) {
                    val current = settingsDataStore.settingsFlow.first()
                    settingsDataStore.updateSettings(current.copy(backgroundUri = null))
                    return@launch
                }

                // Copy file to internal storage
                val app = getApplication<Application>()
                val filename = "background_image.jpg"
                app.contentResolver.openInputStream(uri)?.use { input ->
                    app.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
                        input.copyTo(output)
                    }
                }

                val fileUri = Uri.fromFile(app.getFileStreamPath(filename)).toString()
                val current = settingsDataStore.settingsFlow.first()
                settingsDataStore.updateSettings(current.copy(backgroundUri = fileUri))
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo actualizar el fondo", e)
                _errorMessage.value = "No se pudo actualizar la imagen de fondo."
            }
        }
    }

    private fun startService(action: String?) {
        val app = getApplication<Application>()
        val intent = Intent(app, PomodoroForegroundService::class.java).apply {
            if (action != null) this.action = action
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            getApplication<Application>().contentResolver
                .query(uri, null, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            result = cursor.getString(index)
                        }
                    }
                }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
