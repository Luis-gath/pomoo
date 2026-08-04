package com.example.pomodoro.features.tasks.domain

import java.time.DayOfWeek

/** Texto legible que puede enviarse por cualquier aplicación sin depender de un backend. */
object StudyScheduleShareFormatter {

    fun weekly(
        template: WeeklyScheduleTemplate,
        weeks: Int,
        courseOrProject: String,
        withReminders: Boolean
    ): String = buildString {
        appendLine("Mi horario de estudio")
        courseOrProject.trim().takeIf { it.isNotBlank() }?.let { appendLine(it) }
        appendLine()
        appendLine("Plan: ${template.label}")
        appendLine("Duración: $weeks ${if (weeks == 1) "semana" else "semanas"}")
        appendLine("Carga: ${template.sessionsPerWeek} sesiones por semana · ${minutesText(template.focusMinutesPerWeek)} de enfoque")
        appendLine()

        template.blocks
            .groupBy { it.day }
            .toSortedMap(compareBy { it.value })
            .forEach { (day, blocks) ->
                blocks.forEach { block ->
                    appendLine("${day.spanishName()} ${block.time.asClockText()} · ${block.title}")
                }
            }

        appendLine()
        appendLine(if (withReminders) "Recordatorios activados" else "Sin recordatorios")
        append("Creado con Pomodoro")
    }

    fun habit(plan: StudyHabitPlan): String = buildString {
        appendLine("Mi hábito de estudio")
        plan.courseOrProject.trim().takeIf { it.isNotBlank() }?.let { appendLine(it) }
        appendLine()
        appendLine("Objetivo: ${plan.days.size} ${if (plan.days.size == 1) "día" else "días"} por semana durante ${plan.weeks} semanas")
        appendLine("Días: ${plan.days.sortedBy { it.value }.joinToString { it.spanishName() }}")
        appendLine("Hora: ${plan.time.asClockText()}")
        appendLine("Sesión: ${plan.sessionLength.focusMinutes} min · ${minutesText(plan.focusMinutesPerWeek)} de enfoque por semana")
        appendLine()
        appendLine("Compromiso: ${plan.implementationIntention()}.")
        appendLine("Si pierdo una sesión, retomo la siguiente sin duplicarla.")
        appendLine()
        appendLine(if (plan.withReminders) "Recordatorios activados" else "Sin recordatorios")
        append("Creado con Pomodoro")
    }

    private fun minutesText(minutes: Int): String {
        val hours = minutes / 60
        val remainder = minutes % 60
        return when {
            hours == 0 -> "$remainder min"
            remainder == 0 -> "$hours h"
            else -> "$hours h $remainder min"
        }
    }
}

internal fun DayOfWeek.spanishName(): String = when (this) {
    DayOfWeek.MONDAY -> "lunes"
    DayOfWeek.TUESDAY -> "martes"
    DayOfWeek.WEDNESDAY -> "miércoles"
    DayOfWeek.THURSDAY -> "jueves"
    DayOfWeek.FRIDAY -> "viernes"
    DayOfWeek.SATURDAY -> "sábado"
    DayOfWeek.SUNDAY -> "domingo"
}
