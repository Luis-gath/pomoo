package com.example.pomodoro.features.areas.domain

/** Un aviso concreto: cuál de los adelantos es y en qué instante se dispara. */
data class ReminderInstant(
    val offsetIndex: Int,
    val triggerAt: Long
)

/**
 * Cuándo avisar de una entrega.
 *
 * Se avisa varias veces porque un único aviso el mismo día llega tarde para un trabajo
 * largo. Es lógica pura y sin Android a propósito: así se puede comprobar con tests en
 * lugar de tener que instalar la app y esperar a que salte una alarma.
 */
object DeliverableReminders {

    /** Adelantos respecto a la fecha de entrega, del más lejano al más próximo. */
    val OFFSETS_MILLIS: List<Long> = listOf(
        7L * 24 * 60 * 60 * 1000,
        24L * 60 * 60 * 1000,
        0L
    )

    /**
     * Base del espacio de códigos de las alarmas de entregas.
     *
     * Las tareas ya ocupan `taskId`, `taskId + 10000` y `taskId + 20000` en
     * TaskReminderNotifier. Sin una base separada, la alarma de un material podría
     * sobrescribir la de una tarea.
     */
    private const val REQUEST_BASE = 500_000

    /** Avisos que todavía tienen sentido: los que ya pasaron se descartan. */
    fun instantsFor(dueAt: Long, now: Long): List<ReminderInstant> =
        OFFSETS_MILLIS
            .mapIndexed { index, offset -> ReminderInstant(index, dueAt - offset) }
            .filter { it.triggerAt > now }

    fun requestCode(itemId: Int, offsetIndex: Int): Int =
        REQUEST_BASE + itemId * OFFSETS_MILLIS.size + offsetIndex
}
