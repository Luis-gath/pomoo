package com.example.pomodoro.features.stats.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 7, 31)

    @Test
    fun `sin dias activos la racha es cero`() {
        assertEquals(0, StreakCalculator.currentStreak(emptyList(), today))
    }

    @Test
    fun `solo hoy cuenta como racha de uno`() {
        assertEquals(1, StreakCalculator.currentStreak(listOf(today), today))
    }

    @Test
    fun `dias consecutivos terminando hoy suman la racha completa`() {
        val dates = listOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, StreakCalculator.currentStreak(dates, today))
    }

    @Test
    fun `la racha sigue viva si el ultimo dia activo fue ayer`() {
        val dates = listOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, StreakCalculator.currentStreak(dates, today))
    }

    @Test
    fun `la racha se rompe si el ultimo dia activo fue hace dos dias`() {
        val dates = listOf(today.minusDays(2), today.minusDays(3))
        assertEquals(0, StreakCalculator.currentStreak(dates, today))
    }

    @Test
    fun `un hueco intermedio corta la racha en ese punto`() {
        // Hoy y ayer sí; falta anteayer; luego hay más días activos que ya no cuentan
        val dates = listOf(today, today.minusDays(1), today.minusDays(4), today.minusDays(5))
        assertEquals(2, StreakCalculator.currentStreak(dates, today))
    }
}
