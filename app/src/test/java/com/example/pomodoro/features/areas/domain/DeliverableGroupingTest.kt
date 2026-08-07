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

    // Las fronteras son donde de verdad puede haber un hueco o un solape: un `<` de más
    // o de menos no lo detecta ninguna comprobación del interior de las franjas.

    @Test
    fun `justo en la hora de entrega todavia no esta vencida`() {
        assertEquals(DueBucket.HOY, DeliverableGrouping.bucketOf(ahora, ahora))
    }

    @Test
    fun `un milisegundo antes de la hora de entrega ya esta vencida`() {
        assertEquals(DueBucket.VENCIDA, DeliverableGrouping.bucketOf(ahora - 1, ahora))
    }

    @Test
    fun `exactamente un dia despues ya no es hoy`() {
        assertEquals(DueBucket.ESTA_SEMANA, DeliverableGrouping.bucketOf(ahora + dia, ahora))
    }

    @Test
    fun `un milisegundo antes de las veinticuatro horas sigue siendo hoy`() {
        assertEquals(DueBucket.HOY, DeliverableGrouping.bucketOf(ahora + dia - 1, ahora))
    }

    @Test
    fun `exactamente siete dias despues ya es mas adelante`() {
        assertEquals(DueBucket.MAS_ADELANTE, DeliverableGrouping.bucketOf(ahora + 7 * dia, ahora))
    }

    @Test
    fun `un milisegundo antes de los siete dias sigue siendo esta semana`() {
        assertEquals(DueBucket.ESTA_SEMANA, DeliverableGrouping.bucketOf(ahora + 7 * dia - 1, ahora))
    }
}
