package com.example.pomodoro.features.stats.domain

import java.time.LocalDate

/**
 * Calcula la racha de días consecutivos con actividad.
 *
 * La racha sigue viva si el último día activo es hoy o ayer; si el hueco es mayor,
 * la racha vale 0. Lógica pura para poder testearla sin base de datos.
 */
object StreakCalculator {

    fun currentStreak(activeDatesDesc: List<LocalDate>, today: LocalDate): Int {
        if (activeDatesDesc.isEmpty()) return 0

        val mostRecent = activeDatesDesc.first()
        var expected = when (mostRecent) {
            today -> today
            today.minusDays(1) -> today.minusDays(1)
            else -> return 0
        }

        var streak = 0
        for (date in activeDatesDesc) {
            when {
                date.isEqual(expected) -> {
                    streak++
                    expected = expected.minusDays(1)
                }
                date.isAfter(expected) -> Unit // duplicado o fecha futura: se ignora
                else -> return streak          // hueco: la racha termina aquí
            }
        }
        return streak
    }
}
