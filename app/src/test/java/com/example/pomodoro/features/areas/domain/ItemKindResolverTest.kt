package com.example.pomodoro.features.areas.domain

import com.example.pomodoro.features.areas.data.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemKindResolverTest {

    @Test
    fun `reconoce por tipo MIME`() {
        assertEquals(ItemKind.PDF, ItemKindResolver.resolve("application/pdf", null))
        assertEquals(ItemKind.FOTO, ItemKindResolver.resolve("image/jpeg", null))
        assertEquals(ItemKind.VIDEO, ItemKindResolver.resolve("video/mp4", null))
        assertEquals(ItemKind.AUDIO, ItemKindResolver.resolve("audio/mpeg", null))
    }

    @Test
    fun `ignora los parametros del tipo MIME`() {
        assertEquals(ItemKind.FOTO, ItemKindResolver.resolve("image/jpeg; charset=utf-8", null))
    }

    @Test
    fun `cae en la extension cuando el MIME es generico`() {
        // Muchas apps comparten con este MIME inútil: la extensión salva el caso.
        assertEquals(ItemKind.PDF, ItemKindResolver.resolve("application/octet-stream", "apuntes.pdf"))
        assertEquals(ItemKind.VIDEO, ItemKindResolver.resolve("application/octet-stream", "clase.mp4"))
        assertEquals(ItemKind.FOTO, ItemKindResolver.resolve(null, "pizarra.HEIC"))
    }

    @Test
    fun `lo desconocido es un archivo generico`() {
        assertEquals(ItemKind.ARCHIVO, ItemKindResolver.resolve(null, "practica.zip"))
        assertEquals(ItemKind.ARCHIVO, ItemKindResolver.resolve(null, null))
        assertEquals(ItemKind.ARCHIVO, ItemKindResolver.resolve(null, "sin_extension"))
    }

    @Test
    fun `detecta enlaces`() {
        assertTrue(ItemKindResolver.isLink("https://ejemplo.com/tema1"))
        assertTrue(ItemKindResolver.isLink("  http://ejemplo.com  "))
        assertFalse(ItemKindResolver.isLink("mira esto https://ejemplo.com"))
        assertFalse(ItemKindResolver.isLink("apuntes de clase"))
    }

    @Test
    fun `el titulo por defecto quita la extension`() {
        assertEquals("apuntes tema 3", ItemKindResolver.defaultTitle("apuntes tema 3.pdf", "Material"))
        assertEquals("Material", ItemKindResolver.defaultTitle(null, "Material"))
        assertEquals("Material", ItemKindResolver.defaultTitle("   ", "Material"))
    }
}
