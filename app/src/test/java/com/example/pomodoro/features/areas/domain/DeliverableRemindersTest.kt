package com.example.pomodoro.features.areas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliverableRemindersTest {

    private val dia = 24L * 60 * 60 * 1000
    private val ahora = 1_000_000_000_000L

    @Test
    fun `una entrega lejana genera los tres avisos`() {
        val instantes = DeliverableReminders.instantsFor(dueAt = ahora + 30 * dia, now = ahora)

        assertEquals(3, instantes.size)
        assertEquals(listOf(0, 1, 2), instantes.map { it.offsetIndex })
    }

    @Test
    fun `los avisos van en orden cronologico y terminan en la fecha de entrega`() {
        val entrega = ahora + 30 * dia
        val instantes = DeliverableReminders.instantsFor(dueAt = entrega, now = ahora)

        assertEquals(instantes.sortedBy { it.triggerAt }, instantes)
        assertEquals(entrega, instantes.last().triggerAt)
    }

    @Test
    fun `si falta menos de una semana se omite el aviso semanal`() {
        val instantes = DeliverableReminders.instantsFor(dueAt = ahora + 2 * dia, now = ahora)

        assertEquals(2, instantes.size)
        assertTrue(instantes.none { it.offsetIndex == 0 })
    }

    @Test
    fun `si falta menos de un dia solo queda el aviso de la propia entrega`() {
        val instantes = DeliverableReminders.instantsFor(dueAt = ahora + 3 * 60 * 60 * 1000, now = ahora)

        assertEquals(1, instantes.size)
        assertEquals(2, instantes.single().offsetIndex)
    }

    @Test
    fun `una entrega ya pasada no genera avisos`() {
        val instantes = DeliverableReminders.instantsFor(dueAt = ahora - dia, now = ahora)

        assertTrue(instantes.isEmpty())
    }

    @Test
    fun `cada aviso de cada material tiene un codigo distinto`() {
        val codigos = listOf(
            DeliverableReminders.requestCode(itemId = 1, offsetIndex = 0),
            DeliverableReminders.requestCode(itemId = 1, offsetIndex = 1),
            DeliverableReminders.requestCode(itemId = 2, offsetIndex = 0)
        )

        assertEquals(codigos.size, codigos.toSet().size)
    }

    @Test
    fun `los codigos no invaden el rango que usan las tareas`() {
        // TaskReminderNotifier usa taskId, taskId+10000 y taskId+20000.
        val codigo = DeliverableReminders.requestCode(itemId = 0, offsetIndex = 0)

        assertTrue(codigo > 20_000 + 100_000)
    }
}
