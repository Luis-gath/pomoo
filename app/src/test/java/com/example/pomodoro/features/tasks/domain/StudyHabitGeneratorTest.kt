package com.example.pomodoro.features.tasks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class StudyHabitGeneratorTest {

    private val lima = ZoneId.of("America/Lima")

    @Test
    fun `crea una sesion por dia y semana`() {
        val plan = plan(weeks = 8)

        val sessions = StudyHabitGenerator.generate(
            plan = plan,
            from = LocalDateTime.of(2026, 8, 3, 10, 0),
            zone = lima
        )

        assertEquals(24, sessions.size)
    }

    @Test
    fun `mantiene la misma hora local en toda la rutina`() {
        val sessions = StudyHabitGenerator.generate(
            plan = plan(),
            from = LocalDateTime.of(2026, 8, 3, 10, 0),
            zone = lima
        )

        sessions.forEach { session ->
            val local = ZonedDateTime.ofInstant(Instant.ofEpochMilli(session.startMillis), lima)
            assertEquals(19, local.hour)
            assertEquals(0, local.minute)
        }
    }

    @Test
    fun `si la hora de hoy paso inicia ese dia la semana siguiente`() {
        val mondayEvening = LocalDateTime.of(2026, 8, 3, 20, 0)
        val sessions = StudyHabitGenerator.generate(plan(), mondayEvening, lima)

        val firstMonday = sessions
            .map { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.startMillis), lima) }
            .first { it.dayOfWeek == DayOfWeek.MONDAY }

        assertEquals(10, firstMonday.dayOfMonth)
        assertTrue(sessions.all { it.startMillis >= mondayEvening.atZone(lima).toInstant().toEpochMilli() })
    }

    @Test
    fun `guarda la senal y una recuperacion sin castigo`() {
        val sessions = StudyHabitGenerator.generate(
            plan = plan(startCue = "Después de cenar"),
            from = LocalDateTime.of(2026, 8, 3, 10, 0),
            zone = lima
        )

        assertTrue(sessions.first().notes.contains("Después de cenar"))
        assertTrue(sessions.first().notes.contains("retoma la siguiente"))
    }

    @Test
    fun `el texto compartido muestra dias hora y carga real`() {
        val text = StudyScheduleShareFormatter.habit(plan(weeks = 8))

        assertTrue(text.contains("lunes, miércoles, viernes"))
        assertTrue(text.contains("Hora: 19:00"))
        assertTrue(text.contains("1 h 15 min de enfoque por semana"))
        assertFalse(text.contains("21 días"))
    }

    private fun plan(
        weeks: Int = 2,
        startCue: String = ""
    ) = StudyHabitPlan(
        days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        time = LocalTime.of(19, 0),
        sessionLength = HabitSessionLength.STEADY,
        weeks = weeks,
        courseOrProject = "Cálculo",
        startCue = startCue
    )
}
