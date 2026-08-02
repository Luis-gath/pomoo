package com.example.pomodoro.features.timer.domain

data class Settings(
    val focusDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val longBreakEveryNCycles: Int = 4,
    val autoStartNext: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val backgroundUri: String? = null,

    /** Tono de la interfaz, elegido en Ajustes. Se guarda por nombre del enum. */
    val toneName: String = "CALIDA",

    /** Modo enfoque serio: durante el foco la pantalla solo muestra el temporizador. */
    val focusModeEnabled: Boolean = false,

    /** Dentro del modo enfoque, activar No molestar. Requiere permiso del sistema. */
    val blockNotificationsInFocus: Boolean = true
)
