package com.example.pomodoro.features.premium.domain

/**
 * Qué incluye la versión gratuita.
 *
 * Los límites viven aquí y no repartidos por las pantallas para que la respuesta a «qué
 * se llevan pagando» sea un único sitio que leer, y para que la pantalla premium no pueda
 * prometer algo distinto de lo que el código aplica.
 *
 * El criterio: lo gratuito tiene que servir de verdad —el temporizador, las tareas, las
 * entregas y el horario no se tocan— y lo de pago es lo que necesita quien ya usa la app
 * en serio.
 */
object FreeLimits {

    /** Tres áreas cubren a quien prueba la app; quien lleva seis cursos ya está dentro. */
    const val MAX_FREE_AREAS = 3

    /** Días de historial de estadísticas sin pagar. */
    const val FREE_STATS_DAYS = 7

    fun canCreateArea(currentCount: Int, isPremium: Boolean): Boolean =
        isPremium || currentCount < MAX_FREE_AREAS

    /** Cuántas áreas más caben, para poder avisar antes de llegar al tope. */
    fun remainingAreas(currentCount: Int, isPremium: Boolean): Int? =
        if (isPremium) null else (MAX_FREE_AREAS - currentCount).coerceAtLeast(0)
}
