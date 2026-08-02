package com.example.pomodoro.features.tasks.domain

import com.example.pomodoro.features.tasks.data.TaskPriority
import java.time.LocalDate
import javax.inject.Inject

data class ScheduleApplied(
    val created: Int,
    val weeks: Int,
    val template: WeeklyScheduleTemplate
)

/**
 * Aplica una rutina semanal creando las tareas correspondientes.
 *
 * No hay tabla ni proceso nuevos: las sesiones son tareas normales, así que aparecen en el
 * panel y en el calendario, disparan sus recordatorios y arrancan el Pomodoro con su propia
 * configuración igual que cualquier otra. Reaprovechar lo que ya existe evita duplicar la
 * mitad de la app para algo que es, en el fondo, "crea estas tareas por mí".
 *
 * Lo que **no** hace: reponerse solo cada semana. Al agotarse las semanas generadas hay que
 * volver a aplicar la rutina.
 */
class ApplyWeeklyScheduleUseCase @Inject constructor(
    private val createTask: CreateTaskUseCase
) {

    suspend operator fun invoke(
        template: WeeklyScheduleTemplate,
        weeks: Int,
        courseOrProject: String = "",
        withReminders: Boolean = true,
        from: LocalDate = LocalDate.now()
    ): ScheduleApplied {
        val sessions = WeeklyScheduleGenerator.generate(template, from, weeks)

        var created = 0
        sessions.forEach { session ->
            val task = createTask(
                title = session.title,
                courseOrProject = courseOrProject,
                dueDateTime = session.startMillis,
                priority = TaskPriority.MEDIUM,
                totalPomodoros = session.study.totalPomodoros,
                focusMinutes = session.study.focusMinutes,
                shortBreakMinutes = session.study.shortBreakMinutes,
                longBreakMinutes = session.study.longBreakMinutes,
                longBreakEvery = session.study.longBreakEvery,
                isNotificationEnabled = withReminders
            )
            if (task != null) created++
        }

        return ScheduleApplied(created = created, weeks = weeks, template = template)
    }
}
