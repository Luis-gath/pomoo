package com.example.pomodoro.features.timer.domain

/**
 * Hechos puntuales de una sesión Pomodoro. El Service los traduce a efectos de Android
 * (notificaciones, sonidos); la lógica de dominio no conoce esos detalles.
 */
sealed interface SessionEvent {
    data class TimerFinished(val mode: PomodoroMode) : SessionEvent
    data class TaskCompleted(val taskTitle: String) : SessionEvent
}
