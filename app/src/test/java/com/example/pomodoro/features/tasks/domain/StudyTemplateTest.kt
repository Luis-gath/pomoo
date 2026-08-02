package com.example.pomodoro.features.tasks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyTemplateTest {

    @Test
    fun `todas las plantillas tienen valores dentro de los rangos que acepta una tarea`() {
        // Los mismos límites que aplica CreateTaskUseCase al validar.
        StudyTemplate.entries.forEach { t ->
            assertTrue("${t.name}: focus", t.focusMinutes in 1..90)
            assertTrue("${t.name}: descanso corto", t.shortBreakMinutes in 1..30)
            assertTrue("${t.name}: descanso largo", t.longBreakMinutes in 1..60)
            assertTrue("${t.name}: cada N", t.longBreakEvery in 2..8)
            assertTrue("${t.name}: total", t.totalPomodoros in 1..12)
        }
    }

    @Test
    fun `la plantilla clasica es el pomodoro estandar`() {
        val c = StudyTemplate.CLASICO
        assertEquals(25, c.focusMinutes)
        assertEquals(5, c.shortBreakMinutes)
        assertEquals(15, c.longBreakMinutes)
        assertEquals(4, c.longBreakEvery)
    }

    @Test
    fun `ingenieria concentra mas minutos de foco que idiomas`() {
        val ingenieria = StudyTemplate.INGENIERIA
        val idiomas = StudyTemplate.IDIOMAS

        assertTrue(ingenieria.focusMinutes > idiomas.focusMinutes)
        // Idiomas compensa con más repeticiones.
        assertTrue(idiomas.totalPomodoros >= ingenieria.totalPomodoros)
    }

    @Test
    fun `la duracion aproximada suma foco y descansos intermedios`() {
        // Lectura: 2 bloques de 45 minutos. El descanso entre ambos es el CORTO, porque el
        // largo solo toca al completar un múltiplo de longBreakEvery (aquí, tras el segundo),
        // y ese último no se cuenta al no incluir descanso final.
        assertEquals(45 + 15 + 45, StudyTemplate.LECTURA.approximateMinutes)
    }
}
