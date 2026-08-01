package com.example.pomodoro.features.timer.domain

import android.util.Log
import com.example.pomodoro.core.di.AppScope
import com.example.pomodoro.core.time.Clock
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.timer.data.SettingsRepository
import com.example.pomodoro.features.stats.data.StatsRepository
import com.example.pomodoro.features.tasks.data.TaskRepository
import com.example.pomodoro.features.tasks.domain.CompleteFocusForTaskUseCase
import com.example.pomodoro.features.tasks.domain.FocusCompletionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PomodoroSession"

/**
 * Máquina de estados de una sesión Pomodoro.
 *
 * Extraída de PomodoroForegroundService para poder testearse en la JVM: no conoce
 * el ciclo de vida de Android, ni notificaciones, ni reproducción de audio. El Service
 * observa [state] y [events] y traduce eso a efectos de plataforma.
 */
@Singleton
class PomodoroSessionController @Inject constructor(
    private val engine: PomodoroEngine,
    private val statsRepository: StatsRepository,
    private val taskRepository: TaskRepository,
    private val completeFocusForTask: CompleteFocusForTaskUseCase,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    @AppScope private val scope: CoroutineScope
) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    private var currentSettings = Settings()
    private var endTimeMillis: Long = 0L
    private var timerJob: Job? = null
    private var activeTask: TaskEntity? = null

    init {
        scope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                currentSettings = settings
                // Si está inactivo, reflejar de inmediato la nueva duración configurada.
                if (!_state.value.isRunning && _state.value.currentTimeMillis == 0L) {
                    val duration = engine.getDurationMillis(_state.value.mode, settings)
                    _state.update { it.copy(totalTimeMillis = duration, currentTimeMillis = duration) }
                }
            }
        }
        // Los contadores salen de Room, la única fuente de estadísticas.
        scope.launch {
            combine(
                statsRepository.getTodayFocusCount(),
                statsRepository.getTotalFocusCount()
            ) { today, total -> today to total }
                .collect { (today, total) ->
                    _state.update { it.copy(sessionsToday = today, sessionsTotal = total) }
                }
        }
    }

    // --- Control del temporizador ---

    fun start() {
        if (_state.value.isRunning) return

        val durationRemaining = if (_state.value.currentTimeMillis > 0) {
            _state.value.currentTimeMillis
        } else {
            durationFor(_state.value.mode)
        }
        endTimeMillis = clock.now() + durationRemaining

        _state.update {
            it.copy(
                isRunning = true,
                totalTimeMillis = if (it.totalTimeMillis <= 1L) durationRemaining else it.totalTimeMillis
            )
        }
        startTick()
    }

    fun pause() {
        timerJob?.cancel()
        timerJob = null
        _state.update { it.copy(isRunning = false) }
    }

    fun reset() {
        pause()
        val duration = durationFor(_state.value.mode)
        _state.update { it.copy(currentTimeMillis = duration, totalTimeMillis = duration) }
    }

    fun nextMode() {
        pause()

        val task = activeTask
        val (nextMode, nextCycle) = if (task != null) {
            engine.calculateNextModeForTask(_state.value.mode, _state.value.currentCycle, task)
        } else {
            engine.calculateNextMode(
                _state.value.mode,
                _state.value.currentCycle,
                currentSettings.longBreakEveryNCycles
            )
        }

        val duration = durationFor(nextMode)
        _state.update {
            it.copy(
                mode = nextMode,
                currentCycle = nextCycle,
                currentTimeMillis = duration,
                totalTimeMillis = duration
            )
        }

        val shouldAutoStart = task?.taskAutoStartNext ?: currentSettings.autoStartNext
        if (shouldAutoStart) {
            start()
        }
    }

    // --- Tarea activa ---

    /** Establece una tarea con su propia configuración Pomodoro. */
    fun setActiveTaskWithConfig(task: TaskEntity) {
        activeTask = task

        val duration = engine.getDurationMillisForTask(PomodoroMode.Focus, task)
        _state.update {
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
    }

    fun clearActiveTask() {
        activeTask = null
        _state.update {
            it.copy(
                activeTaskId = null,
                activeTaskTitle = null,
                activeTaskAudioUri = null,
                activeTaskTotalPomodoros = 0,
                activeTaskCompletedPomodoros = 0
            )
        }
    }

    fun setTaskAudioPlaying(playing: Boolean) {
        _state.update { it.copy(isTaskAudioPlaying = playing) }
    }

    // --- Interno ---

    private fun durationFor(mode: PomodoroMode): Long {
        val task = activeTask
        return if (task != null) {
            engine.getDurationMillisForTask(mode, task)
        } else {
            engine.getDurationMillis(mode, currentSettings)
        }
    }

    private fun startTick() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val remaining = endTimeMillis - clock.now()
                if (remaining <= 0) {
                    onTimerFinished()
                    break
                }
                _state.update { it.copy(currentTimeMillis = remaining) }
                delay(1000)
            }
        }
    }

    private suspend fun onTimerFinished() {
        val finishedMode = _state.value.mode
        _state.update { it.copy(isRunning = false, currentTimeMillis = 0) }
        _events.emit(SessionEvent.TimerFinished(finishedMode))

        var taskJustCompleted = false

        if (finishedMode == PomodoroMode.Focus) {
            val minutesFocused = activeTask?.focusMinutes ?: currentSettings.focusDurationMinutes
            statsRepository.recordFocusSession(minutesFocused)

            val task = activeTask
            if (task != null) {
                when (val result = completeFocusForTask(task.id)) {
                    is FocusCompletionResult.Success -> {
                        _state.update { it.copy(activeTaskCompletedPomodoros = result.completedPomodoros) }

                        if (result.isTaskComplete) {
                            _events.emit(SessionEvent.TaskCompleted(result.taskTitle))
                            statsRepository.recordTaskCompletion()
                            taskJustCompleted = true
                        } else {
                            // Refrescar la copia en memoria con lo persistido
                            activeTask = taskRepository.getTaskById(task.id)
                        }
                    }

                    is FocusCompletionResult.Error -> {
                        Log.e(TAG, "No se pudo completar el focus: ${result.message}")
                    }
                }
            }
        }

        // El siguiente modo se calcula todavía con la configuración de la tarea que terminó.
        nextMode()

        if (taskJustCompleted) {
            clearActiveTask()
        }
    }
}
