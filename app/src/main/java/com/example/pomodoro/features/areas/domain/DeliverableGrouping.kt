package com.example.pomodoro.features.areas.domain

/** Franja de urgencia de una entrega. El orden del enum es el orden de la pantalla. */
enum class DueBucket { VENCIDA, HOY, ESTA_SEMANA, MAS_ADELANTE }

/**
 * Reparte las entregas en franjas de urgencia.
 *
 * Se agrupa por cercanía y no por fecha exacta porque lo que el usuario necesita saber de
 * un vistazo es qué le aprieta, no el calendario completo.
 */
object DeliverableGrouping {

    private const val DIA = 24L * 60 * 60 * 1000

    fun bucketOf(dueAt: Long, now: Long): DueBucket = when {
        dueAt < now -> DueBucket.VENCIDA
        dueAt < now + DIA -> DueBucket.HOY
        dueAt < now + 7 * DIA -> DueBucket.ESTA_SEMANA
        else -> DueBucket.MAS_ADELANTE
    }
}
