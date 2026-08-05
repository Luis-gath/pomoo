package com.example.pomodoro.features.areas.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DeliverableGroupingTest {

    private val hora = 60L * 60 * 1000
    private val dia = 24 * hora
    private val ahora = 1_000_000_000_000L

    @Test
    fun `una fecha pasada esta vencida`() {
        assertEquals(DueBucket.VENCIDA, DeliverableGrouping.bucketOf(ahora - hora, ahora))
    }

    @Test
    fun `dentro de las proximas horas es hoy`() {
        assertEquals(DueBucket.HOY, DeliverableGrouping.bucketOf(ahora + 2 * hora, ahora))
    }

    @Test
    fun `dentro de tres dias es esta semana`() {
        assertEquals(DueBucket.ESTA_SEMANA, DeliverableGrouping.bucketOf(ahora + 3 * dia, ahora))
    }

    @Test
    fun `dentro de un mes es mas adelante`() {
        assertEquals(DueBucket.MAS_ADELANTE, DeliverableGrouping.bucketOf(ahora + 30 * dia, ahora))
    }
}
