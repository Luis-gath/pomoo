package com.example.pomodoro.domain.usecases

import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.data.model.TaskPriority
import com.example.pomodoro.data.model.TaskStatus
import com.example.pomodoro.data.repository.TaskRepository

/**
 * Caso de uso para crear una nueva tarea
 */
class CreateTaskUseCase(private val repository: TaskRepository) {
    
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
        isNotificationEnabled: Boolean = false
    ): Long {
        val now = System.currentTimeMillis()
        val task = TaskEntity(
            title = title.trim(),
            notes = notes.trim(),
            courseOrProject = courseOrProject.trim(),
            dueDateTime = dueDateTime,
            priority = priority,
            status = TaskStatus.TODO,
            totalPomodoros = totalPomodoros.coerceIn(1, 12),
            completedPomodoros = 0,
            focusMinutes = focusMinutes.coerceIn(1, 90),
            shortBreakMinutes = shortBreakMinutes.coerceIn(1, 30),
            longBreakMinutes = longBreakMinutes.coerceIn(1, 60),
            longBreakEvery = longBreakEvery.coerceIn(2, 8),
            isNotificationEnabled = isNotificationEnabled,
            createdAt = now,
            updatedAt = now,
            timestamp = dueDateTime ?: now
        )
        
        return repository.insertOrUpdateTask(task)
    }
}
