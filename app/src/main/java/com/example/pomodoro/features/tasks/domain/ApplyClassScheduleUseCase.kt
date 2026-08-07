package com.example.pomodoro.features.tasks.domain

import com.example.pomodoro.features.tasks.data.RepeatType
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskPriority
import com.example.pomodoro.features.tasks.data.TaskRepository
import com.example.pomodoro.features.tasks.data.TaskStatus
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ClassScheduleApplied(
    val created: Int,
    val courseName: String,
    val classGroupId: String
)

/**
 * Da de alta el horario de un curso creando sus clases.
 *
 * Las clases son tareas con [TaskEntity.classGroupId] relleno. Eso las hace aparecer en el
 * calendario y permite arrancarles el Pomodoro como a cualquier otra, pero también permite
 * distinguirlas para que no engrosen la lista de pendientes: una clase es un sitio donde
 * estar, no algo por hacer.
 *
 * No pasa por CreateTaskUseCase porque ahí la duración se deriva del número de pomodoros,
 * y en una clase la duración la manda el horario: de 8 a 10 son dos horas, haya los
 * pomodoros que haya.
 */
class ApplyClassScheduleUseCase @Inject constructor(
    private val repository: TaskRepository
) {

    suspend operator fun invoke(
        plan: ClassSchedulePlan,
        from: LocalDate = LocalDate.now()
    ): ClassScheduleApplied {
        val sessions = ClassScheduleGenerator.generate(plan, from)
        val groupId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        sessions.forEach { session ->
            val end = session.startMillis + session.durationMinutes * 60_000L
            repository.insertOrUpdateTask(
                TaskEntity(
                    title = plan.courseName,
                    courseOrProject = plan.courseName,
                    areaId = plan.areaId,
                    dueDateTime = session.startMillis,
                    dueDateTimeMillis = session.startMillis,
                    startDateTimeMillis = session.startMillis,
                    endDateTimeMillis = end,
                    timestamp = session.startMillis,
                    computedDurationMinutes = session.durationMinutes,
                    priority = TaskPriority.MEDIUM,
                    status = TaskStatus.TODO,
                    repeatType = RepeatType.WEEKLY,
                    classGroupId = groupId,
                    isNotificationEnabled = plan.withReminders,
                    // Los pomodoros son orientativos: caben tantos como quepan en la clase.
                    totalPomodoros = (session.durationMinutes / 30).coerceIn(1, 12),
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        return ClassScheduleApplied(
            created = sessions.size,
            courseName = plan.courseName,
            classGroupId = groupId
        )
    }

    /** Quita el horario entero de un curso: todas sus clases comparten identificador. */
    suspend fun removeGroup(classGroupId: String) =
        repository.deleteClassGroup(classGroupId)
}
