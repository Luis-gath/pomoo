package com.example.pomodoro.features.tasks.domain

import com.example.pomodoro.features.tasks.data.RepeatType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Calcula en qué fechas cae una tarea que se repite.
 *
 * Las ocurrencias se generan por adelantado, igual que hacen las rutinas semanales y los
 * hábitos: son tareas normales, así que aparecen en el calendario, disparan sus
 * recordatorios y arrancan el Pomodoro sin necesidad de un motor de recurrencia aparte.
 *
 * Es lógica pura y sin Android para poder comprobarla con tests, que es donde se ven los
 * casos que de otro modo solo aparecen meses después: los meses cortos y el cambio de hora.
 */
object RecurrenceGenerator {

    /**
     * Tope de seguridad. Cada ocurrencia es una fila y una alarma, así que una repetición
     * diaria «para siempre» llenaría la base y la cola de alarmas del sistema.
     */
    const val MAX_OCCURRENCES = 120

    /** Cuántas repeticiones proponer sin que el usuario tenga que pensarlo. */
    fun defaultCount(type: RepeatType): Int = when (type) {
        RepeatType.NONE -> 1
        RepeatType.DAILY -> 14
        RepeatType.WEEKLY -> 8
        RepeatType.MONTHLY -> 6
    }

    /**
     * Instantes en los que hay que crear la tarea, incluida la fecha original.
     *
     * Cada ocurrencia se calcula **desde la fecha original** y no sumando sobre la
     * anterior: así, una mensual que empieza un día 31 vuelve al 31 después de pasar por
     * febrero, en vez de quedarse anclada al día 28 para siempre.
     */
    fun occurrences(
        startMillis: Long,
        type: RepeatType,
        count: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Long> {
        val total = when (type) {
            RepeatType.NONE -> 1
            else -> count.coerceIn(1, MAX_OCCURRENCES)
        }

        val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMillis), zone)

        return (0 until total).map { index ->
            val step = index.toLong()
            val occurrence = when (type) {
                RepeatType.NONE -> start
                RepeatType.DAILY -> start.plusDays(step)
                RepeatType.WEEKLY -> start.plusWeeks(step)
                RepeatType.MONTHLY -> start.plusMonths(step)
            }
            // Se reconstruye desde la fecha local para conservar la hora del reloj cuando
            // hay cambio de horario de verano; sumar milisegundos la desplazaría una hora.
            occurrence.atZone(zone).toInstant().toEpochMilli()
        }
    }

    /** Fecha de la última ocurrencia, para poder decirle al usuario hasta cuándo llega. */
    fun lastOccurrence(
        startMillis: Long,
        type: RepeatType,
        count: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long = occurrences(startMillis, type, count, zone).last()
}
