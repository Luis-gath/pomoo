package com.example.pomodoro.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import com.example.pomodoro.domain.model.PomodoroMode
import com.example.pomodoro.domain.model.UiState
import com.example.pomodoro.service.PomodoroForegroundService
import com.example.pomodoro.data.datastore.SettingsDataStore
import com.example.pomodoro.data.datastore.AudioSettingsDataStore
import com.example.pomodoro.data.datastore.AudioSettings
import com.example.pomodoro.data.model.CustomTrack
import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.data.local.TaskDatabase
import com.example.pomodoro.util.AudioPlayerManager

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.example.pomodoro.util.TaskDurationCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val database = TaskDatabase.getInstance(application)

    // Derived UI State for Active Task Header
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeTaskUiState: StateFlow<ActiveTaskUiState> = _uiState
        .map { it.activeTaskId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id != null) {
                database.taskDao.getTaskByIdFlow(id)
            } else {
                flowOf(null)
            }
        }
        .combine(_uiState) { task, state ->
            val hasTask = state.activeTaskId != null
            val title = task?.title ?: state.activeTaskTitle ?: ""
            val course = task?.courseOrProject
            
            // Recalculate ETA based on remaining work if task is present
            // Logic: current time + remaining duration? 
            // Or just use the static endDateTimeMillis from task?
            // Static might be outdated if user paused. 
            // Better: calculate ETA dynamic if we have duration calculator.
            // For now, let's use the property from TaskEntity if available, 
            // assuming it's updated when timer runs? No, DB isn't updated every second.
            // Let's rely on stored ETV for simplicity or calculate simplistic one.
            
            val eta = if (task?.endDateTimeMillis != null && task.endDateTimeMillis > System.currentTimeMillis()) {
                 val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                 "Fin: " + dateFormat.format(Date(task.endDateTimeMillis))
            } else null

            val modeText = when (state.mode) {
                 PomodoroMode.Focus -> "ENFOQUE"
                 PomodoroMode.ShortBreak -> "DESCANSO CORTO"
                 PomodoroMode.LongBreak -> "DESCANSO LARGO"
            }
            
            val statusText = if (hasTask) {
                when (state.mode) {
                    PomodoroMode.Focus -> {
                        if (!state.isRunning) "En pausa • Pomodoro ${state.activeTaskCompletedPomodoros + 1}/${state.activeTaskTotalPomodoros}"
                        else "Pomodoro ${state.activeTaskCompletedPomodoros + 1}/${state.activeTaskTotalPomodoros} • $modeText"
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

    private val audioSettingsDataStore = AudioSettingsDataStore(application)
    val audioSettings: StateFlow<AudioSettings> = audioSettingsDataStore.audioSettingsFlow
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), AudioSettings())
        
    // Predefined tracks from raw resources
    private val predefinedTracks = AudioPlayerManager(application).getAvailableTracks()

    private var pomodoroService: PomodoroForegroundService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PomodoroForegroundService.LocalBinder
            pomodoroService = binder.getService()
            isBound = true
            
            // Observe Service State
            viewModelScope.launch {
                pomodoroService?.uiState?.collect { serviceState ->
                    _uiState.value = serviceState
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            pomodoroService = null
        }
    }


    private val statsRepository = com.example.pomodoro.data.repository.StatsRepository(database.statsDao, database.taskDao)

    init {
        // Start and bind service immediately
        val intent = Intent(application, PomodoroForegroundService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
             application.startForegroundService(intent) 
        } else {
             application.startService(intent)
        }
        
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // Observe service state to detect finished sessions logic if not exposed directly.
        // Or better verify where completion logic happens. It usually happens in Service.
        // For accurate tracking, Service should probably notify completion
        // or we rely on uiState changes (Running -> Paused with 0 time? No, usually auto transitions)
        
        // Actually best place is in Service `finishTimer()` logic or similar.
    }

    // Combined list: predefined + custom tracks
    fun getAllTracks(customTracks: List<CustomTrack>): List<Pair<String, String>> {
        val combined = predefinedTracks.toMutableList()
        customTracks.forEach { custom ->
            combined.add(custom.id to custom.name)
        }
        return combined
    }

    // Legacy property for compatibility
    val availableTracks: List<Pair<String, String>> get() = predefinedTracks

    fun toggleTimer() {
        val intent = Intent(getApplication(), PomodoroForegroundService::class.java).apply {
            action = if (_uiState.value.isRunning) PomodoroForegroundService.ACTION_PAUSE else PomodoroForegroundService.ACTION_RESUME
        }
        getApplication<Application>().startService(intent)
    }

    fun resetTimer() {
        val intent = Intent(getApplication(), PomodoroForegroundService::class.java).apply {
            action = PomodoroForegroundService.ACTION_RESET
        }
        getApplication<Application>().startService(intent)
    }

    fun nextMode() {
        val intent = Intent(getApplication(), PomodoroForegroundService::class.java).apply {
            action = PomodoroForegroundService.ACTION_NEXT
        }
        getApplication<Application>().startService(intent)
    }

    fun setActiveTask(title: String?, audioUri: String?) {
        pomodoroService?.setActiveTask(title, audioUri)
    }

    /**
     * Establece una tarea con configuración Pomodoro propia
     */
    fun setActiveTaskWithConfig(task: TaskEntity) {
        pomodoroService?.setActiveTaskWithConfig(task)
    }

    /**
     * Limpia la tarea activa
     */
    fun clearActiveTask() {
        pomodoroService?.clearActiveTask()
    }

    fun toggleTaskAudio() {
        val intent = Intent(getApplication(), PomodoroForegroundService::class.java).apply {
            action = PomodoroForegroundService.ACTION_TOGGLE_AUDIO
        }
        getApplication<Application>().startService(intent)
    }

    // --- Background Music Controls ---
    
    fun setMusicEnabled(enabled: Boolean) {
        viewModelScope.launch {
            audioSettingsDataStore.updateMusicEnabled(enabled)
            // Actualizar servicio si es necesario
            if (enabled && _uiState.value.isRunning) {
                // Si activamos música y el timer corre, intentar reproducir
                val intent = Intent(getApplication(), PomodoroForegroundService::class.java).apply {
                    action = PomodoroForegroundService.ACTION_UPDATE_AUDIO_SETTINGS
                }
                getApplication<Application>().startService(intent)
            } else if (!enabled) {
                // Si desactivamos, detener
                val intent = Intent(getApplication(), PomodoroForegroundService::class.java).apply {
                    action = PomodoroForegroundService.ACTION_UPDATE_AUDIO_SETTINGS
                }
                getApplication<Application>().startService(intent)
            }
        }
    }

    fun setMuted(muted: Boolean) {
        viewModelScope.launch {
            audioSettingsDataStore.updateMuted(muted)
            // Notificar servicio instantáneamente
            val intent = Intent(getApplication(), PomodoroForegroundService::class.java).apply {
                action = PomodoroForegroundService.ACTION_UPDATE_AUDIO_SETTINGS
            }
            getApplication<Application>().startService(intent)
        }
    }
    
    fun setVolume(volume: Float) {
        viewModelScope.launch {
            audioSettingsDataStore.updateVolume(volume)
             // Notificar servicio instantáneamente
            val intent = Intent(getApplication(), PomodoroForegroundService::class.java).apply {
                action = PomodoroForegroundService.ACTION_UPDATE_AUDIO_SETTINGS
            }
            getApplication<Application>().startService(intent)
        }
    }
    
    fun selectTrack(trackId: String) {
        viewModelScope.launch {
            audioSettingsDataStore.updateSelectedTrack(trackId)
            // Cambiar pista en vivo si está sonando
            if (_uiState.value.isRunning) {
                 val intent = Intent(getApplication(), PomodoroForegroundService::class.java).apply {
                    action = PomodoroForegroundService.ACTION_UPDATE_AUDIO_SETTINGS
                }
                getApplication<Application>().startService(intent)
            }
        }
    }
    
    fun importAudio(uri: Uri) {
        viewModelScope.launch {
            try {
                // Persist permissions
                val contentResolver = getApplication<Application>().contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                
                // Get filename
                val name = getFileName(uri) ?: "Audio Importado"
                val id = "custom_${UUID.randomUUID()}"
                
                // Save track
                val customTrack = CustomTrack(id, name, uri.toString())
                audioSettingsDataStore.addCustomTrack(customTrack)
                
                // Auto-select
                selectTrack(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun deleteCustomTrack(trackId: String) {
        viewModelScope.launch {
            // Si la borrada es la actual, volver a default
            if (audioSettings.value.selectedTrackId == trackId) {
                audioSettingsDataStore.updateSelectedTrack("lofi_beat")
            }
            audioSettingsDataStore.removeCustomTrack(trackId)
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
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

    fun updateBackground(uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            val settingsDataStore = SettingsDataStore(getApplication())
            if (uri == null) {
                // Clear background
                settingsDataStore.settingsFlow.collect { current ->
                    settingsDataStore.updateSettings(current.copy(backgroundUri = null))
                    return@collect
                }
                return@launch
            }

            // Copy file to internal storage
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val filename = "background_image.jpg"
                val outputStream = getApplication<Application>().openFileOutput(filename, Context.MODE_PRIVATE)

                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                
                val file = getApplication<Application>().getFileStreamPath(filename)
                val fileUri = Uri.fromFile(file).toString()

                val current = settingsDataStore.settingsFlow.firstOrNull() 
                if (current != null) {
                     settingsDataStore.updateSettings(current.copy(backgroundUri = fileUri))
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
    }
}
