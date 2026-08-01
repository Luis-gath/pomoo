package com.example.pomodoro.core.time

import javax.inject.Inject

/**
 * Fuente de tiempo inyectable. Existe para que la máquina de estados del Pomodoro
 * pueda testearse con tiempo virtual en lugar de depender de System.currentTimeMillis().
 */
interface Clock {
    fun now(): Long
}

class SystemClock @Inject constructor() : Clock {
    override fun now(): Long = System.currentTimeMillis()
}
