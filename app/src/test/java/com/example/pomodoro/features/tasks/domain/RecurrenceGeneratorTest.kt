package com.example.pomodoro.features.tasks.domain

import com.example.pomodoro.features.tasks.data.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RecurrenceGeneratorTest {

    private val zona: ZoneId = ZoneId.systemDefault()

    private fun millis(fecha: LocalDateTime): Long =
        fecha.atZone(zona).toInstant().toEpochMilli()

    private fun fechas(instantes: List<Long>): List<LocalDateTime> =
        instantes.map { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), zona) }

    @Test
    fun `sin repeticion solo devuelve la fecha original`() {
        val inicio = LocalDateTime.of(2026, 3, 10, 9, 0)

        val resultado = RecurrenceGenerator.occurrences(millis(inicio), RepeatType.NONE, 5)

        assertEquals(listOf(inicio), fechas(resultado))
    }

    @Test
    fun `la repeticion diaria avanza un dia y conserva la hora`() {
        val inicio = LocalDateTime.of(2026, 3, 10, 9, 30)

        val resultado = RecurrenceGenerator.occurrences(millis(inicio), RepeatType.DAILY, 3)

        assertEquals(
            listOf(
                inicio,
                LocalDateTime.of(2026, 3, 11, 9, 30),
                LocalDateTime.of(2026, 3, 12, 9, 30)
            ),
            fechas(resultado)
        )
    }

    @Test
    fun `la repeticion semanal cae siempre en el mismo dia de la semana`() {
        val inicio = LocalDateTime.of(2026, 3, 10, 18, 0)

        val resultado = RecurrenceGenerator.occurrences(millis(inicio), RepeatType.WEEKLY, 3)

        assertEquals(
            listOf(
                inicio,
                LocalDateTime.of(2026, 3, 17, 18, 0),
                LocalDateTime.of(2026, 3, 24, 18, 0)
            ),
            fechas(resultado)
        )
    }

    @Test
    fun `la repeticion mensual conserva el dia del mes`() {
        val inicio = LocalDateTime.of(2026, 3, 15, 8, 0)

        val resultado = RecurrenceGenerator.occurrences(millis(inicio), RepeatType.MONTHLY, 3)

        assertEquals(
            listOf(
                inicio,
                LocalDateTime.of(2026, 4, 15, 8, 0),
                LocalDateTime.of(2026, 5, 15, 8, 0)
            ),
            fechas(resultado)
        )
    }

    @Test
    fun `una mensual del dia 31 se ajusta al ultimo dia de los meses cortos`() {
        val inicio = LocalDateTime.of(2026, 1, 31, 12, 0)

        val resultado = RecurrenceGenerator.occurrences(millis(inicio), RepeatType.MONTHLY, 4)

        // Febrero no tiene 31, pero marzo si: cada ocurrencia se calcula desde la fecha
        // original, de modo que el recorte de un mes corto no arrastra a los siguientes.
        assertEquals(
            listOf(
                inicio,
                LocalDateTime.of(2026, 2, 28, 12, 0),
                LocalDateTime.of(2026, 3, 31, 12, 0),
                LocalDateTime.of(2026, 4, 30, 12, 0)
            ),
            fechas(resultado)
        )
    }

    @Test
    fun `un numero de repeticiones menor que uno devuelve solo la original`() {
        val inicio = LocalDateTime.of(2026, 3, 10, 9, 0)

        val resultado = RecurrenceGenerator.occurrences(millis(inicio), RepeatType.DAILY, 0)

        assertEquals(listOf(inicio), fechas(resultado))
    }

    @Test
    fun `el numero de repeticiones se limita para no generar tareas sin freno`() {
        val inicio = LocalDateTime.of(2026, 3, 10, 9, 0)

        val resultado = RecurrenceGenerator.occurrences(millis(inicio), RepeatType.DAILY, 5_000)

        assertEquals(RecurrenceGenerator.MAX_OCCURRENCES, resultado.size)
    }

    @Test
    fun `cada tipo propone un horizonte distinto por defecto`() {
        // Catorce dias, ocho semanas y seis meses cubren un horizonte parecido sin
        // llenar el calendario de filas.
        assertEquals(1, RecurrenceGenerator.defaultCount(RepeatType.NONE))
        assertEquals(14, RecurrenceGenerator.defaultCount(RepeatType.DAILY))
        assertEquals(8, RecurrenceGenerator.defaultCount(RepeatType.WEEKLY))
        assertEquals(6, RecurrenceGenerator.defaultCount(RepeatType.MONTHLY))
    }
}
