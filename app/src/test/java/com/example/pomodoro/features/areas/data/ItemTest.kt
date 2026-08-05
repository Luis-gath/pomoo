package com.example.pomodoro.features.areas.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemTest {

    private val ahora = 1_000_000_000_000L

    @Test
    fun `un material marcado como entrega con fecha y sin completar es entrega pendiente`() {
        val item = Item(
            areaId = 1,
            kind = ItemKind.FOTO,
            title = "Entrega",
            mark = ItemMark.ENTREGA,
            dueAt = ahora,
            completedAt = null
        )

        assertTrue(item.isPendingDeliverable)
    }

    @Test
    fun `un material entrega con fecha pero ya completado no es pendiente`() {
        val item = Item(
            areaId = 1,
            kind = ItemKind.FOTO,
            title = "Entrega completada",
            mark = ItemMark.ENTREGA,
            dueAt = ahora,
            completedAt = ahora + 1000
        )

        assertFalse(item.isPendingDeliverable)
    }

    @Test
    fun `un material marcado como entrega sin fecha no es pendiente`() {
        val item = Item(
            areaId = 1,
            kind = ItemKind.FOTO,
            title = "Entrega sin fecha",
            mark = ItemMark.ENTREGA,
            dueAt = null,
            completedAt = null
        )

        assertFalse(item.isPendingDeliverable)
    }

    @Test
    fun `un material con fecha pero marca distinta de entrega no es pendiente`() {
        val item = Item(
            areaId = 1,
            kind = ItemKind.FOTO,
            title = "Importante",
            mark = ItemMark.IMPORTANTE,
            dueAt = ahora,
            completedAt = null
        )

        assertFalse(item.isPendingDeliverable)
    }
}
