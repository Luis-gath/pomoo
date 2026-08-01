package com.example.pomodoro.features.tasks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskDurationCalculatorTest {

    @Test
    fun `sin pomodoros la duracion es cero`() {
        val total = TaskDurationCalculator.calculateTotalMinutes(
            totalPomodoros = 0,
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            longBreakEvery = 4,
            includeFinalBreak = false
        )

        assertEquals(0, total)
    }

    @Test
    fun `pomodoros negativos devuelven cero`() {
        val total = TaskDurationCalculator.calculateTotalMinutes(
            totalPomodoros = -3,
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            longBreakEvery = 4,
            includeFinalBreak = false
        )

        assertEquals(0, total)
    }

    @Test
    fun `un solo pomodoro sin descanso final es solo el focus`() {
        val total = TaskDurationCalculator.calculateTotalMinutes(
            totalPomodoros = 1,
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            longBreakEvery = 4,
            includeFinalBreak = false
        )

        assertEquals(25, total)
    }

    @Test
    fun `cuatro pomodoros sin descanso final excluyen el ultimo descanso`() {
        // 4 focus (100) + 3 descansos cortos (15) = 115
        val total = TaskDurationCalculator.calculateTotalMinutes(
            totalPomodoros = 4,
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            longBreakEvery = 4,
            includeFinalBreak = false
        )

        assertEquals(115, total)
    }

    @Test
    fun `incluir descanso final agrega el descanso largo del cuarto pomodoro`() {
        // 115 del caso anterior + descanso largo (15) porque 4 % 4 == 0
        val total = TaskDurationCalculator.calculateTotalMinutes(
            totalPomodoros = 4,
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            longBreakEvery = 4,
            includeFinalBreak = true
        )

        assertEquals(130, total)
    }

    @Test
    fun `descanso largo intercalado cuando longBreakEvery es 2`() {
        // f(25) + corto(5) + f(25) + largo(15) + f(25) + corto(5) + f(25) sin descanso final = 125
        val total = TaskDurationCalculator.calculateTotalMinutes(
            totalPomodoros = 4,
            focusMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15,
            longBreakEvery = 2,
            includeFinalBreak = false
        )

        assertEquals(125, total)
    }

    @Test
    fun `calculateEndTime sin fecha de inicio devuelve null`() {
        assertNull(TaskDurationCalculator.calculateEndTime(startMillis = null, durationMinutes = 30))
    }

    @Test
    fun `calculateEndTime suma la duracion en milisegundos`() {
        val start = 1_000_000L

        val end = TaskDurationCalculator.calculateEndTime(startMillis = start, durationMinutes = 30)

        assertEquals(start + 30 * 60_000L, end)
    }
}
