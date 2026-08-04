package com.example.pomodoro.features.tasks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class StudyPlanCodeCodecTest {

    @Test
    fun `horario conserva su configuracion al importar`() {
        val code = StudyPlanCodeCodec.encodeWeekly(
            WeeklyScheduleTemplate.IDIOMAS,
            weeks = 4,
            courseOrProject = "Inglés",
            withReminders = false
        )

        val imported = StudyPlanCodeCodec.decode(code).getOrThrow() as ImportedStudyPlan.Weekly

        assertEquals(WeeklyScheduleTemplate.IDIOMAS, imported.template)
        assertEquals(4, imported.weeks)
        assertEquals("Inglés", imported.courseOrProject)
        assertEquals(false, imported.withReminders)
    }

    @Test
    fun `habito conserva dias hora duracion y senal`() {
        val original = StudyHabitPlan(
            days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            time = LocalTime.of(7, 30),
            sessionLength = HabitSessionLength.LIGHT,
            weeks = 12,
            courseOrProject = "Anatomía",
            startCue = "Después del desayuno"
        )

        val imported = StudyPlanCodeCodec.decode(
            StudyPlanCodeCodec.encodeHabit(original)
        ).getOrThrow() as ImportedStudyPlan.Habit

        assertEquals(original, imported.plan)
    }

    @Test
    fun `extrae codigo desde un mensaje compartido`() {
        val code = StudyPlanCodeCodec.encodeWeekly(
            WeeklyScheduleTemplate.INGENIERIA, 2, "Cálculo", true
        )

        val imported = StudyPlanCodeCodec.decode("Te comparto mi plan:\n$code\nAbre Pomodoro")

        assertTrue(imported.isSuccess)
    }

    @Test
    fun `rechaza un codigo modificado`() {
        val code = StudyPlanCodeCodec.encodeWeekly(
            WeeklyScheduleTemplate.MEDICINA, 2, "Biología", true
        )
        val replacement = if (code.last() == '0') '1' else '0'
        val altered = code.dropLast(1) + replacement

        assertTrue(StudyPlanCodeCodec.decode(altered).isFailure)
    }

    @Test
    fun `rechaza texto sin codigo`() {
        assertTrue(StudyPlanCodeCodec.decode("horario desconocido").isFailure)
    }
}
