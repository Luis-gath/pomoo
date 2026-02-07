package com.example.pomodoro.domain.model

data class Settings(
    val focusDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val longBreakEveryNCycles: Int = 4,
    val autoStartNext: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val backgroundUri: String? = null
)
