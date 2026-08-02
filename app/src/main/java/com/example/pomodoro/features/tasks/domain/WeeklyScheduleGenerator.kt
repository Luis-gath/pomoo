package com.example.pomodoro.features.tasks.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Una sesión concreta ya situada en el calendario. */
data class PlannedSession(
    val title: String,
    val startMillis: Long,
    val study: StudyTemplate
)

/**
 * Convierte una rutina semanal en sesiones con fecha y hora.
 *
 * Es lógica pura y sin Android para poder testearla con una zona horaria fija: el cálculo
 * de "el próximo lunes a las 18:00" es justo el tipo de operación donde un desfase horario
 * pasa desapercibido hasta que una sesión aparece el día equivocado.
 */
object WeeklyScheduleGenerator {

    /**
     * @param from primer día a considerar; cada bloque cae en su primera coincidencia
     *             a partir de esta fecha, incluida ella misma.
     * @param weeks cuántas semanas generar.
     */
    fun generate(
        template: WeeklyScheduleTemplate,
        from: LocalDate,
        weeks: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PlannedSession> {
        if (weeks <= 0) return emptyList()

        val sessions = mutableListOf<PlannedSession>()

        for (week in 0 until weeks) {
            template.blocks.forEach { block ->
                val date = from
                    .with(TemporalAdjusters.nextOrSame(block.day))
                    .plusWeeks(week.toLong())

                val startMillis = LocalDateTime.of(date, block.time)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()

                sessions += PlannedSession(
                    title = block.title,
                    startMillis = startMillis,
                    study = block.study
                )
            }
        }

        return sessions.sortedBy { it.startMillis }
    }
}
