package com.example.pomodoro.util

import java.util.Calendar

object TaskDurationCalculator {

    /**
     * Calcula la duración total en minutos de una sesión de tareas.
     * Reglas:
     * - Cada focus (N) dura [focusMinutes].
     * - Entre focus hay descansos (Short o Long).
     * - El descanso es Long si el índice del pomodoro completado es múltiplo de [longBreakEvery].
     * - Si [includeFinalBreak] es false, NO se cuenta el descanso después del último focus.
     */
    fun calculateTotalMinutes(
        totalPomodoros: Int,
        focusMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int,
        longBreakEvery: Int,
        includeFinalBreak: Boolean
    ): Int {
        if (totalPomodoros <= 0) return 0

        var totalMinutes = 0
        
        for (i in 1..totalPomodoros) {
            // 1. Agregar tiempo de Foco
            totalMinutes += focusMinutes

            // 2. Determinar si corresponde agregar descanso
            val isLastPomodoro = (i == totalPomodoros)
            
            if (!isLastPomodoro || includeFinalBreak) {
                // Determinar tipo de descanso
                // Si completamos "longBreakEvery" pomodoros, toca descanso largo.
                // Ejemplo: every=4. Al terminar el 4to, 8vo, 12vo -> Largo.
                val isLongBreak = (i % longBreakEvery == 0)
                
                totalMinutes += if (isLongBreak) longBreakMinutes else shortBreakMinutes
            }
        }
        
        return totalMinutes
    }

    /**
     * Calcula la fecha de fin basada en inicio y duración
     */
    fun calculateEndTime(startMillis: Long?, durationMinutes: Int): Long? {
        if (startMillis == null) return null
        return startMillis + (durationMinutes * 60 * 1000L)
    }
}
