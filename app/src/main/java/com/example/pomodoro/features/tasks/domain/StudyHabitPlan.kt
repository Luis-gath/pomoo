package com.example.pomodoro.features.tasks.domain

import java.time.DayOfWeek
import java.time.LocalTime

/** Tamaño de una sesión pensado para empezar con una meta sostenible. */
enum class HabitSessionLength(
    val label: String,
    val description: String,
    val focusMinutes: Int
) {
    LIGHT(
        label = "Inicio ligero",
        description = "15 min para reducir la fricción de empezar",
        focusMinutes = 15
    ),
    STEADY(
        label = "Constancia",
        description = "25 min para sostener el ritmo",
        focusMinutes = 25
    ),
    DEEP(
        label = "Enfoque profundo",
        description = "45 min cuando el hábito ya es estable",
        focusMinutes = 45
    )
}

/** Configuración completa de una rutina que se repetirá en un contexto estable. */
data class StudyHabitPlan(
    val days: Set<DayOfWeek>,
    val time: LocalTime,
    val sessionLength: HabitSessionLength,
    val weeks: Int,
    val courseOrProject: String = "",
    val startCue: String = "",
    val withReminders: Boolean = true
) {
    init {
        require(days.isNotEmpty()) { "Elige al menos un día" }
        require(weeks in 1..16) { "La duración debe estar entre 1 y 16 semanas" }
    }

    val sessions: Int get() = days.size * weeks
    val focusMinutesPerWeek: Int get() = days.size * sessionLength.focusMinutes

    /** Frase concreta de cuándo y cómo empezar, útil como intención de implementación. */
    fun implementationIntention(): String {
        val activity = courseOrProject.trim().ifBlank { "mi sesión de estudio" }
        val action = "estudiaré $activity durante ${sessionLength.focusMinutes} min"
        val cue = startCue.trim().trimEnd('.', ',')
        return if (cue.isBlank()) {
            "A las ${time.asClockText()}, $action"
        } else {
            "${cue.replaceFirstChar { it.uppercase() }}, $action"
        }
    }
}

internal fun LocalTime.asClockText(): String = "%02d:%02d".format(hour, minute)
