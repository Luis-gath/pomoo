package com.example.pomodoro.features.timer.domain

import com.example.pomodoro.features.tasks.data.TaskEntity
class PomodoroEngine {

    fun calculateNextMode(
        currentMode: PomodoroMode,
        currentCycle: Int,
        longBreakEveryN: Int
    ): Pair<PomodoroMode, Int> {
        return when (currentMode) {
            PomodoroMode.Focus -> {
                if (currentCycle % longBreakEveryN == 0) {
                    Pair(PomodoroMode.LongBreak, currentCycle)
                } else {
                    Pair(PomodoroMode.ShortBreak, currentCycle)
                }
            }
            PomodoroMode.ShortBreak -> {
                Pair(PomodoroMode.Focus, currentCycle + 1)
            }
            PomodoroMode.LongBreak -> {
                Pair(PomodoroMode.Focus, 1)
            }
        }
    }

    fun getDurationMillis(mode: PomodoroMode, settings: com.example.pomodoro.features.timer.domain.Settings): Long {
        val minutes = when (mode) {
            PomodoroMode.Focus -> settings.focusDurationMinutes
            PomodoroMode.ShortBreak -> settings.shortBreakDurationMinutes
            PomodoroMode.LongBreak -> settings.longBreakDurationMinutes
        }
        return minutes * 60 * 1000L
    }

    // ===== Métodos para tarea con configuración propia =====

    /**
     * Obtiene la duración en millis usando la configuración de la tarea
     */
    fun getDurationMillisForTask(mode: PomodoroMode, task: TaskEntity): Long {
        val minutes = when (mode) {
            PomodoroMode.Focus -> task.focusMinutes
            PomodoroMode.ShortBreak -> task.shortBreakMinutes
            PomodoroMode.LongBreak -> task.longBreakMinutes
        }
        return minutes * 60 * 1000L
    }

    /**
     * Calcula el siguiente modo usando la configuración de la tarea
     */
    fun calculateNextModeForTask(
        currentMode: PomodoroMode,
        currentCycle: Int,
        task: TaskEntity
    ): Pair<PomodoroMode, Int> {
        return calculateNextMode(currentMode, currentCycle, task.longBreakEvery)
    }

    /**
     * Verifica si la tarea debe marcarse como completada
     */
    fun isTaskComplete(task: TaskEntity): Boolean {
        return task.completedPomodoros >= task.totalPomodoros
    }
}

