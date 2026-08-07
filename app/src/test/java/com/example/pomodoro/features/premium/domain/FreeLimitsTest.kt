package com.example.pomodoro.features.premium.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeLimitsTest {

    @Test
    fun `sin premium se pueden crear areas hasta el tope`() {
        assertTrue(FreeLimits.canCreateArea(currentCount = 0, isPremium = false))
        assertTrue(FreeLimits.canCreateArea(currentCount = 2, isPremium = false))
    }

    @Test
    fun `sin premium el tope bloquea la siguiente area`() {
        assertFalse(FreeLimits.canCreateArea(currentCount = FreeLimits.MAX_FREE_AREAS, isPremium = false))
    }

    @Test
    fun `con premium no hay tope`() {
        assertTrue(FreeLimits.canCreateArea(currentCount = 50, isPremium = true))
    }

    @Test
    fun `quedan por crear las que faltan hasta el tope`() {
        assertEquals(3, FreeLimits.remainingAreas(currentCount = 0, isPremium = false))
        assertEquals(1, FreeLimits.remainingAreas(currentCount = 2, isPremium = false))
    }

    @Test
    fun `pasado el tope quedan cero y no un numero negativo`() {
        assertEquals(0, FreeLimits.remainingAreas(currentCount = 9, isPremium = false))
    }

    @Test
    fun `con premium no se muestra cuenta restante`() {
        assertNull(FreeLimits.remainingAreas(currentCount = 0, isPremium = true))
    }
}
