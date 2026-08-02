package com.example.pomodoro.features.tasks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class WeeklyScheduleGeneratorTest {

    // Zona del usuario, UTC-5 y sin horario de verano.
    private val lima = ZoneId.of("America/Lima")

    @Test
    fun `genera una tanda por cada semana pedida`() {
        val sessions = WeeklyScheduleGenerator.generate(
            template = WeeklyScheduleTemplate.INGENIERIA,
            from = LocalDate.of(2026, 8, 3), // lunes
            weeks = 3,
            zone = lima
        )

        assertEquals(WeeklyScheduleTemplate.INGENIERIA.sessionsPerWeek * 3, sessions.size)
    }

    @Test
    fun `cero semanas no genera nada`() {
        val sessions = WeeklyScheduleGenerator.generate(
            WeeklyScheduleTemplate.INGENIERIA, LocalDate.of(2026, 8, 3), 0, lima
        )
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `cada sesion cae en el dia de la semana que marca su bloque`() {
        val sessions = WeeklyScheduleGenerator.generate(
            WeeklyScheduleTemplate.INGENIERIA, LocalDate.of(2026, 8, 3), 2, lima
        )

        val days = sessions.map {
            ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(it.startMillis), lima
            ).dayOfWeek
        }.toSet()

        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
            days
        )
    }

    @Test
    fun `respeta la hora local del bloque, no la UTC`() {
        // El bloque de los lunes es a las 18:00. En UTC-5 eso son las 23:00 UTC del mismo
        // día: si se calculara en UTC, la sesión saldría a las 13:00 hora local.
        val sessions = WeeklyScheduleGenerator.generate(
            WeeklyScheduleTemplate.INGENIERIA, LocalDate.of(2026, 8, 3), 1, lima
        )

        val monday = sessions.first {
            ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(it.startMillis), lima
            ).dayOfWeek == DayOfWeek.MONDAY
        }

        val local = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(monday.startMillis), lima
        )
        assertEquals(18, local.hour)
        assertEquals(0, local.minute)
    }

    @Test
    fun `si se empieza a mitad de semana el bloque cae en su proxima coincidencia`() {
        // Se arranca un jueves: el bloque del lunes debe caer en el lunes SIGUIENTE,
        // nunca en uno ya pasado.
        val thursday = LocalDate.of(2026, 8, 6)
        val sessions = WeeklyScheduleGenerator.generate(
            WeeklyScheduleTemplate.INGENIERIA, thursday, 1, lima
        )

        sessions.forEach {
            val date = ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(it.startMillis), lima
            ).toLocalDate()
            assertTrue("$date es anterior al inicio", !date.isBefore(thursday))
        }
    }

    @Test
    fun `las sesiones salen ordenadas en el tiempo`() {
        val sessions = WeeklyScheduleGenerator.generate(
            WeeklyScheduleTemplate.MEDICINA, LocalDate.of(2026, 8, 3), 2, lima
        )

        val times = sessions.map { it.startMillis }
        assertEquals(times.sorted(), times)
    }

    @Test
    fun `la rutina de examenes no programa nada en domingo`() {
        val sessions = WeeklyScheduleGenerator.generate(
            WeeklyScheduleTemplate.EXAMENES, LocalDate.of(2026, 8, 3), 1, lima
        )

        val days = sessions.map {
            ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(it.startMillis), lima
            ).dayOfWeek
        }
        assertTrue(days.none { it == DayOfWeek.SUNDAY })
    }
}
