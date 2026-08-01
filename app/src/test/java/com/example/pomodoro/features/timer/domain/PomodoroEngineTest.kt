package com.example.pomodoro.features.timer.domain

import com.example.pomodoro.features.tasks.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PomodoroEngineTest {

    private val engine = PomodoroEngine()

    // --- calculateNextMode ---

    @Test
    fun `focus intermedio lleva a descanso corto sin avanzar el ciclo`() {
        val (mode, cycle) = engine.calculateNextMode(
            currentMode = PomodoroMode.Focus,
            currentCycle = 2,
            longBreakEveryN = 4
        )

        assertEquals(PomodoroMode.ShortBreak, mode)
        assertEquals(2, cycle)
    }

    @Test
    fun `focus en multiplo de longBreakEveryN lleva a descanso largo`() {
        val (mode, cycle) = engine.calculateNextMode(
            currentMode = PomodoroMode.Focus,
            currentCycle = 4,
            longBreakEveryN = 4
        )

        assertEquals(PomodoroMode.LongBreak, mode)
        assertEquals(4, cycle)
    }

    @Test
    fun `descanso corto vuelve a focus e incrementa el ciclo`() {
        val (mode, cycle) = engine.calculateNextMode(
            currentMode = PomodoroMode.ShortBreak,
            currentCycle = 2,
            longBreakEveryN = 4
        )

        assertEquals(PomodoroMode.Focus, mode)
        assertEquals(3, cycle)
    }

    @Test
    fun `descanso largo vuelve a focus y reinicia el ciclo a 1`() {
        val (mode, cycle) = engine.calculateNextMode(
            currentMode = PomodoroMode.LongBreak,
            currentCycle = 8,
            longBreakEveryN = 4
        )

        assertEquals(PomodoroMode.Focus, mode)
        assertEquals(1, cycle)
    }

    @Test
    fun `un ciclo completo con longBreakEveryN 2 alterna corto y largo`() {
        // Ciclo 1: Focus -> ShortBreak (1 % 2 != 0)
        val (m1, c1) = engine.calculateNextMode(PomodoroMode.Focus, 1, 2)
        assertEquals(PomodoroMode.ShortBreak, m1)

        // ShortBreak -> Focus, ciclo 2
        val (m2, c2) = engine.calculateNextMode(m1, c1, 2)
        assertEquals(PomodoroMode.Focus, m2)
        assertEquals(2, c2)

        // Ciclo 2: Focus -> LongBreak (2 % 2 == 0)
        val (m3, _) = engine.calculateNextMode(m2, c2, 2)
        assertEquals(PomodoroMode.LongBreak, m3)
    }

    // --- getDurationMillis (ajustes globales) ---

    @Test
    fun `duracion global usa el campo correspondiente a cada modo`() {
        val settings = Settings(
            focusDurationMinutes = 30,
            shortBreakDurationMinutes = 7,
            longBreakDurationMinutes = 20
        )

        assertEquals(30 * 60_000L, engine.getDurationMillis(PomodoroMode.Focus, settings))
        assertEquals(7 * 60_000L, engine.getDurationMillis(PomodoroMode.ShortBreak, settings))
        assertEquals(20 * 60_000L, engine.getDurationMillis(PomodoroMode.LongBreak, settings))
    }

    // --- getDurationMillisForTask (configuración por tarea) ---

    @Test
    fun `duracion por tarea ignora los ajustes globales`() {
        val task = taskWith(focusMinutes = 50, shortBreakMinutes = 10, longBreakMinutes = 25)

        assertEquals(50 * 60_000L, engine.getDurationMillisForTask(PomodoroMode.Focus, task))
        assertEquals(10 * 60_000L, engine.getDurationMillisForTask(PomodoroMode.ShortBreak, task))
        assertEquals(25 * 60_000L, engine.getDurationMillisForTask(PomodoroMode.LongBreak, task))
    }

    @Test
    fun `calculateNextModeForTask usa el longBreakEvery de la tarea`() {
        val task = taskWith(longBreakEvery = 2)

        val (mode, _) = engine.calculateNextModeForTask(
            currentMode = PomodoroMode.Focus,
            currentCycle = 2,
            task = task
        )

        assertEquals(PomodoroMode.LongBreak, mode)
    }

    // --- isTaskComplete ---

    @Test
    fun `tarea incompleta cuando faltan pomodoros`() {
        assertFalse(engine.isTaskComplete(taskWith(totalPomodoros = 4, completedPomodoros = 3)))
    }

    @Test
    fun `tarea completa al alcanzar el total de pomodoros`() {
        assertTrue(engine.isTaskComplete(taskWith(totalPomodoros = 4, completedPomodoros = 4)))
    }

    @Test
    fun `tarea completa si excede el total de pomodoros`() {
        assertTrue(engine.isTaskComplete(taskWith(totalPomodoros = 4, completedPomodoros = 5)))
    }

    private fun taskWith(
        totalPomodoros: Int = 4,
        completedPomodoros: Int = 0,
        focusMinutes: Int = 25,
        shortBreakMinutes: Int = 5,
        longBreakMinutes: Int = 15,
        longBreakEvery: Int = 4
    ) = TaskEntity(
        title = "Tarea de prueba",
        totalPomodoros = totalPomodoros,
        completedPomodoros = completedPomodoros,
        focusMinutes = focusMinutes,
        shortBreakMinutes = shortBreakMinutes,
        longBreakMinutes = longBreakMinutes,
        longBreakEvery = longBreakEvery
    )
}
