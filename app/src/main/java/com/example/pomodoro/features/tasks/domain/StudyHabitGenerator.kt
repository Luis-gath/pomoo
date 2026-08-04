package com.example.pomodoro.features.tasks.domain

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Una oportunidad concreta para repetir el hábito. */
data class PlannedHabitSession(
    val title: String,
    val notes: String,
    val startMillis: Long,
    val focusMinutes: Int
)

/**
 * Genera exactamente una sesión por día elegido y semana.
 *
 * Si la hora de hoy ya pasó, esa serie comienza la semana siguiente. Así nunca se crea una
 * tarea vencida y cada día conserva el mismo contexto horario durante todo el plan.
 */
object StudyHabitGenerator {

    fun generate(
        plan: StudyHabitPlan,
        from: LocalDateTime = LocalDateTime.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<PlannedHabitSession> {
        val subject = plan.courseOrProject.trim()
        val title = if (subject.isBlank()) "Sesión de hábito" else "Estudiar $subject"
        val notes = buildString {
            append("Plan de hábito: ")
            append(plan.implementationIntention())
            append(". Si pierdes una sesión, retoma la siguiente sin duplicarla.")
        }

        return plan.days.flatMap { day ->
            var first = LocalDateTime.of(
                from.toLocalDate().with(TemporalAdjusters.nextOrSame(day)),
                plan.time
            )
            if (first.isBefore(from)) first = first.plusWeeks(1)

            (0 until plan.weeks).map { week ->
                val dateTime = first.plusWeeks(week.toLong())
                PlannedHabitSession(
                    title = title,
                    notes = notes,
                    startMillis = dateTime.atZone(zone).toInstant().toEpochMilli(),
                    focusMinutes = plan.sessionLength.focusMinutes
                )
            }
        }.sortedBy { it.startMillis }
    }
}
