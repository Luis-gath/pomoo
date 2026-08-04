package com.example.pomodoro.features.tasks.domain

import java.time.DayOfWeek
import java.time.LocalTime

/** Un bloque fijo de la semana: qué día, a qué hora y con qué configuración de estudio. */
data class ScheduleBlock(
    val day: DayOfWeek,
    val time: LocalTime,
    val title: String,
    val study: StudyTemplate
)

/**
 * Rutinas semanales listas para aplicar.
 *
 * El propósito es arrancar el hábito sin tener que diseñar el horario desde cero: se elige
 * la rutina que encaja con lo que se estudia y quedan creadas las sesiones de las próximas
 * semanas, cada una con su hora y su recordatorio.
 *
 * El reparto de días no es decorativo. Las rutinas de memorización reparten sesiones cortas
 * a lo largo de toda la semana, porque el repaso espaciado rinde más; las de resolución de
 * problemas concentran menos días pero con bloques largos, porque interrumpir a mitad de un
 * ejercicio cuesta caro.
 */
enum class WeeklyScheduleTemplate(
    val label: String,
    val description: String,
    val blocks: List<ScheduleBlock>
) {
    INGENIERIA(
        label = "Ingeniería",
        description = "Tres bloques largos entre semana y un repaso el sábado",
        blocks = listOf(
            ScheduleBlock(DayOfWeek.MONDAY, LocalTime.of(18, 0), "Bloque de problemas", StudyTemplate.INGENIERIA),
            ScheduleBlock(DayOfWeek.WEDNESDAY, LocalTime.of(18, 0), "Bloque de problemas", StudyTemplate.INGENIERIA),
            ScheduleBlock(DayOfWeek.FRIDAY, LocalTime.of(18, 0), "Bloque de problemas", StudyTemplate.INGENIERIA),
            ScheduleBlock(DayOfWeek.SATURDAY, LocalTime.of(10, 0), "Repaso de la semana", StudyTemplate.LECTURA)
        )
    ),

    MEDICINA(
        label = "Medicina",
        description = "Sesiones cortas de lunes a viernes y repaso acumulado el domingo",
        blocks = listOf(
            ScheduleBlock(DayOfWeek.MONDAY, LocalTime.of(19, 0), "Memorización", StudyTemplate.MEDICINA),
            ScheduleBlock(DayOfWeek.TUESDAY, LocalTime.of(19, 0), "Memorización", StudyTemplate.MEDICINA),
            ScheduleBlock(DayOfWeek.WEDNESDAY, LocalTime.of(19, 0), "Memorización", StudyTemplate.MEDICINA),
            ScheduleBlock(DayOfWeek.THURSDAY, LocalTime.of(19, 0), "Memorización", StudyTemplate.MEDICINA),
            ScheduleBlock(DayOfWeek.FRIDAY, LocalTime.of(19, 0), "Memorización", StudyTemplate.MEDICINA),
            ScheduleBlock(DayOfWeek.SUNDAY, LocalTime.of(10, 0), "Repaso acumulado", StudyTemplate.MEDICINA)
        )
    ),

    IDIOMAS(
        label = "Idiomas",
        description = "Un poco cada día, que es lo que funciona con un idioma",
        blocks = listOf(
            ScheduleBlock(DayOfWeek.MONDAY, LocalTime.of(20, 0), "Práctica diaria", StudyTemplate.IDIOMAS),
            ScheduleBlock(DayOfWeek.TUESDAY, LocalTime.of(20, 0), "Práctica diaria", StudyTemplate.IDIOMAS),
            ScheduleBlock(DayOfWeek.WEDNESDAY, LocalTime.of(20, 0), "Práctica diaria", StudyTemplate.IDIOMAS),
            ScheduleBlock(DayOfWeek.THURSDAY, LocalTime.of(20, 0), "Práctica diaria", StudyTemplate.IDIOMAS),
            ScheduleBlock(DayOfWeek.FRIDAY, LocalTime.of(20, 0), "Práctica diaria", StudyTemplate.IDIOMAS),
            ScheduleBlock(DayOfWeek.SATURDAY, LocalTime.of(11, 0), "Repaso y conversación", StudyTemplate.IDIOMAS)
        )
    ),

    EXAMENES(
        label = "Época de exámenes",
        description = "Dos sesiones al día, mañana y tarde, seis días por semana",
        blocks = DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }.flatMap { day ->
            listOf(
                ScheduleBlock(day, LocalTime.of(9, 0), "Sesión de mañana", StudyTemplate.CLASICO),
                ScheduleBlock(day, LocalTime.of(17, 0), "Sesión de tarde", StudyTemplate.CLASICO)
            )
        }
    );

    /** Cuántas sesiones crea cada semana. */
    val sessionsPerWeek: Int get() = blocks.size

    /** Minutos de estudio a la semana, sumando todos los bloques. */
    val minutesPerWeek: Int get() = blocks.sumOf { it.study.approximateMinutes }

    /** Tiempo de enfoque real, sin contar descansos. */
    val focusMinutesPerWeek: Int
        get() = blocks.sumOf { it.study.focusMinutes * it.study.totalPomodoros }
}
