package com.example.pomodoro.shared.ui.components

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * El DatePicker de Material 3 devuelve la medianoche UTC del día elegido. Estos tests
 * fijan la conversión en una zona con desfase negativo (UTC−5, la del usuario), donde el
 * error de no convertir se manifestaba como una tarea guardada el día anterior.
 */
class PickerDateUtilsTest {

    private val defaultZone = TimeZone.getDefault()

    @Before
    fun useLimaTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima")) // UTC-5, sin horario de verano
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(defaultZone)
    }

    @Test
    fun `combinar fecha y hora conserva el dia elegido en UTC menos 5`() {
        // 5 de agosto de 2026, medianoche UTC: lo que entrega el DatePicker.
        val pickerMillis = utcMillis(2026, Calendar.AUGUST, 5)

        val result = PickerDateUtils.combineDateAndTime(pickerMillis, hour = 9, minute = 30)

        val local = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(2026, local.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, local.get(Calendar.MONTH))
        assertEquals(5, local.get(Calendar.DAY_OF_MONTH)) // antes daba 4
        assertEquals(9, local.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, local.get(Calendar.MINUTE))
        assertEquals(0, local.get(Calendar.SECOND))
        assertEquals(0, local.get(Calendar.MILLISECOND))
    }

    @Test
    fun `la medianoche local sigue cayendo en el mismo dia`() {
        val pickerMillis = utcMillis(2026, Calendar.JANUARY, 1)

        val result = PickerDateUtils.combineDateAndTime(pickerMillis, hour = 0, minute = 0)

        val local = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(2026, local.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, local.get(Calendar.MONTH))
        assertEquals(1, local.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, local.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `ida y vuelta conserva el dia`() {
        // Una tarea a las 23:45 hora local: convertir a lo que espera el picker y volver
        // debe devolver el mismo día, no el siguiente.
        val original = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.MARCH, 17, 23, 45, 0)
        }.timeInMillis

        val pickerMillis = PickerDateUtils.toPickerUtcMillis(original)
        val restored = PickerDateUtils.combineDateAndTime(
            pickerMillis,
            hour = PickerDateUtils.hourOf(original),
            minute = PickerDateUtils.minuteOf(original)
        )

        assertEquals(original, restored)
    }

    @Test
    fun `toPickerUtcMillis devuelve medianoche UTC del dia local`() {
        val localMorning = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JULY, 9, 8, 15, 0)
        }.timeInMillis

        val pickerMillis = PickerDateUtils.toPickerUtcMillis(localMorning)

        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = pickerMillis
        }
        assertEquals(2026, utc.get(Calendar.YEAR))
        assertEquals(Calendar.JULY, utc.get(Calendar.MONTH))
        assertEquals(9, utc.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, utc.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, utc.get(Calendar.MINUTE))
    }

    private fun utcMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day)
        }.timeInMillis
}
