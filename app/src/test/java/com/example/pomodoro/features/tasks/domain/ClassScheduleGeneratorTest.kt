package com.example.pomodoro.features.tasks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ClassScheduleGeneratorTest {

    private val zona: ZoneId = ZoneId.systemDefault()

    /** Lunes 3 de agosto de 2026. */
    private val lunes = LocalDate.of(2026, 8, 3)

    private fun fechas(sesiones: List<ClassSession>): List<LocalDateTime> =
        sesiones.map { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.startMillis), zona) }

    private fun plan(
        days: Set<DayOfWeek>,
        start: LocalTime = LocalTime.of(8, 0),
        end: LocalTime = LocalTime.of(10, 0),
        weeks: Int = 2
    ) = ClassSchedulePlan(
        courseName = "Anatomía",
        days = days,
        startTime = start,
        endTime = end,
        weeks = weeks
    )

    @Test
    fun `genera una sesion por dia elegido y por semana`() {
        val sesiones = ClassScheduleGenerator.generate(
            plan(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)),
            from = lunes
        )

        assertEquals(4, sesiones.size)
    }

    @Test
    fun `las sesiones caen en los dias pedidos y en orden`() {
        val sesiones = ClassScheduleGenerator.generate(
            plan(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)),
            from = lunes
        )

        assertEquals(
            listOf(
                LocalDateTime.of(2026, 8, 3, 8, 0),   // lunes
                LocalDateTime.of(2026, 8, 5, 8, 0),   // miércoles
                LocalDateTime.of(2026, 8, 10, 8, 0),  // lunes siguiente
                LocalDateTime.of(2026, 8, 12, 8, 0)
            ),
            fechas(sesiones)
        )
    }

    @Test
    fun `la duracion sale de la hora de inicio y de fin`() {
        val sesiones = ClassScheduleGenerator.generate(
            plan(setOf(DayOfWeek.MONDAY), start = LocalTime.of(8, 0), end = LocalTime.of(9, 30)),
            from = lunes
        )

        assertEquals(90, sesiones.first().durationMinutes)
    }

    @Test
    fun `si el dia elegido ya paso esta semana empieza en la siguiente`() {
        // Se pide los lunes pero partiendo de un miércoles.
        val miercoles = LocalDate.of(2026, 8, 5)

        val sesiones = ClassScheduleGenerator.generate(
            plan(setOf(DayOfWeek.MONDAY), weeks = 1),
            from = miercoles
        )

        assertEquals(
            listOf(LocalDateTime.of(2026, 8, 10, 8, 0)),
            fechas(sesiones)
        )
    }

    @Test
    fun `el mismo dia de hoy si cuenta`() {
        val sesiones = ClassScheduleGenerator.generate(
            plan(setOf(DayOfWeek.MONDAY), weeks = 1),
            from = lunes
        )

        assertEquals(listOf(LocalDateTime.of(2026, 8, 3, 8, 0)), fechas(sesiones))
    }

    @Test
    fun `sin dias elegidos no genera nada`() {
        val sesiones = ClassScheduleGenerator.generate(plan(emptySet()), from = lunes)

        assertTrue(sesiones.isEmpty())
    }

    @Test
    fun `una hora de fin anterior a la de inicio se considera invalida`() {
        val sesiones = ClassScheduleGenerator.generate(
            plan(setOf(DayOfWeek.MONDAY), start = LocalTime.of(10, 0), end = LocalTime.of(8, 0)),
            from = lunes
        )

        assertTrue(sesiones.isEmpty())
    }

    @Test
    fun `el numero de semanas se limita para no llenar el calendario`() {
        val sesiones = ClassScheduleGenerator.generate(
            plan(setOf(DayOfWeek.MONDAY), weeks = 500),
            from = lunes
        )

        assertEquals(ClassScheduleGenerator.MAX_WEEKS, sesiones.size)
    }

    @Test
    fun `el plan resume cuantas sesiones y cuantas horas semanales supone`() {
        val p = plan(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), weeks = 4)

        assertEquals(12, p.totalSessions)
        assertEquals(360, p.minutesPerWeek)
    }
}
