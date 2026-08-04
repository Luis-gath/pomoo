package com.example.pomodoro.features.tasks.domain

import com.example.pomodoro.features.tasks.data.TaskPriority
import com.example.pomodoro.features.tasks.data.RepeatType
import java.util.UUID
import javax.inject.Inject

data class HabitApplied(
    val created: Int,
    val weeks: Int,
    val focusMinutesPerWeek: Int
)

/** Crea las oportunidades de práctica como tareas normales con recordatorio. */
class ApplyStudyHabitUseCase @Inject constructor(
    private val createTask: CreateTaskUseCase
) {
    suspend operator fun invoke(plan: StudyHabitPlan): HabitApplied {
        val sessions = StudyHabitGenerator.generate(plan)
        val seriesId = UUID.randomUUID().toString()
        var created = 0

        sessions.forEach { session ->
            val task = createTask(
                title = session.title,
                notes = session.notes,
                courseOrProject = plan.courseOrProject,
                dueDateTime = session.startMillis,
                priority = TaskPriority.MEDIUM,
                totalPomodoros = 1,
                focusMinutes = session.focusMinutes,
                shortBreakMinutes = 5,
                longBreakMinutes = 15,
                longBreakEvery = 4,
                includeFinalBreak = false,
                isNotificationEnabled = plan.withReminders,
                repeatType = RepeatType.WEEKLY,
                scheduleSeriesId = seriesId
            )
            if (task != null) created++
        }

        return HabitApplied(
            created = created,
            weeks = plan.weeks,
            focusMinutesPerWeek = plan.focusMinutesPerWeek
        )
    }
}
