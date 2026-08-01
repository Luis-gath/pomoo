package com.example.pomodoro.features.tasks.domain

import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskPriority
import com.example.pomodoro.features.tasks.data.TaskStatus
import com.example.pomodoro.features.tasks.data.TaskRepository
import javax.inject.Inject

/**
 * Crea una tarea aplicando las validaciones de rango y calculando la duración total
 * y la hora de fin. Es el único camino de creación: antes el ViewModel construía la
 * entidad a mano y se saltaba estas validaciones.
 */
class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {

    suspend operator fun invoke(
        title: String,
        notes: String = "",
        courseOrProject: String = "",
        dueDateTime: Long? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        totalPomodoros: Int = 4,
        focusMinutes: Int = 25,
        shortBreakMinutes: Int = 5,
        longBreakMinutes: Int = 15,
        longBreakEvery: Int = 4,
        includeFinalBreak: Boolean = false,
        isNotificationEnabled: Boolean = false
    ): TaskEntity? {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "El título de la tarea no puede estar vacío" }

        val safeTotalPomodoros = totalPomodoros.coerceIn(1, 12)
        val safeFocusMinutes = focusMinutes.coerceIn(1, 90)
        val safeShortBreak = shortBreakMinutes.coerceIn(1, 30)
        val safeLongBreak = longBreakMinutes.coerceIn(1, 60)
        val safeLongBreakEvery = longBreakEvery.coerceIn(2, 8)

        val computedDuration = TaskDurationCalculator.calculateTotalMinutes(
            totalPomodoros = safeTotalPomodoros,
            focusMinutes = safeFocusMinutes,
            shortBreakMinutes = safeShortBreak,
            longBreakMinutes = safeLongBreak,
            longBreakEvery = safeLongBreakEvery,
            includeFinalBreak = includeFinalBreak
        )

        val now = System.currentTimeMillis()
        val startMillis = dueDateTime ?: now
        val endMillis = TaskDurationCalculator.calculateEndTime(dueDateTime, computedDuration)

        val task = TaskEntity(
            title = cleanTitle,
            notes = notes.trim(),
            courseOrProject = courseOrProject.trim(),
            dueDateTime = dueDateTime,
            dueDateTimeMillis = dueDateTime,
            startDateTimeMillis = dueDateTime,
            endDateTimeMillis = endMillis,
            computedDurationMinutes = computedDuration,
            includeFinalBreak = includeFinalBreak,
            reminderMinutesBefore = null,
            priority = priority,
            status = TaskStatus.TODO,
            totalPomodoros = safeTotalPomodoros,
            completedPomodoros = 0,
            focusMinutes = safeFocusMinutes,
            shortBreakMinutes = safeShortBreak,
            longBreakMinutes = safeLongBreak,
            longBreakEvery = safeLongBreakEvery,
            isNotificationEnabled = isNotificationEnabled,
            createdAt = now,
            updatedAt = now,
            timestamp = startMillis
        )

        val id = repository.insertOrUpdateTask(task)
        return repository.getTaskById(id.toInt())
    }
}
