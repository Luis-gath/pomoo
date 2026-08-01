package com.example.pomodoro.features.timer.domain

data class UiState(
    val currentTimeMillis: Long = 0L, // Remaining time in millis
    val totalTimeMillis: Long = 1L,   // Total duration for progress calculation
    val mode: PomodoroMode = PomodoroMode.Focus,
    val isRunning: Boolean = false,
    val currentCycle: Int = 1,
    val sessionsToday: Int = 0,
    val sessionsTotal: Int = 0,
    val activeTaskTitle: String? = null,
    val activeTaskAudioUri: String? = null,
    val isTaskAudioPlaying: Boolean = false,
    // Progreso de tarea activa
    val activeTaskId: Int? = null,
    val activeTaskTotalPomodoros: Int = 0,
    val activeTaskCompletedPomodoros: Int = 0
) {
    val progress: Float
        get() = if (totalTimeMillis > 0) currentTimeMillis.toFloat() / totalTimeMillis else 0f
    
    // Progreso total de la tarea (0.0 a 1.0)
    val taskProgress: Float
        get() = if (activeTaskTotalPomodoros > 0) 
            activeTaskCompletedPomodoros.toFloat() / activeTaskTotalPomodoros 
        else 0f
}

