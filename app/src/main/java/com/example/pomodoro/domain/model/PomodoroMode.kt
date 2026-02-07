package com.example.pomodoro.domain.model

enum class PomodoroMode(
    val title: String,
    val defaultDurationMinutes: Int
) {
    Focus("Enfoque", 25),
    ShortBreak("Descanso Corto", 5),
    LongBreak("Descanso Largo", 15);

    fun next(): PomodoroMode {
        return when (this) {
            Focus -> ShortBreak
            ShortBreak -> Focus
            LongBreak -> Focus
        }
    }
}
