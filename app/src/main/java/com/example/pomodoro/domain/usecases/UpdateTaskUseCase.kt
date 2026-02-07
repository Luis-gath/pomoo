package com.example.pomodoro.domain.usecases

import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.data.repository.TaskRepository

/**
 * Caso de uso para actualizar una tarea existente
 */
class UpdateTaskUseCase(private val repository: TaskRepository) {
    
    suspend operator fun invoke(task: TaskEntity): Long {
        val updatedTask = task.copy(
            updatedAt = System.currentTimeMillis()
        )
        return repository.insertOrUpdateTask(updatedTask)
    }
    
    /**
     * Actualiza campos específicos de la tarea
     */
    suspend fun updateFields(
        taskId: Int,
        title: String? = null,
        notes: String? = null,
        courseOrProject: String? = null,
        dueDateTime: Long? = null,
        totalPomodoros: Int? = null
    ): Boolean {
        val existing = repository.getTaskById(taskId) ?: return false
        
        val updated = existing.copy(
            title = title?.trim() ?: existing.title,
            notes = notes?.trim() ?: existing.notes,
            courseOrProject = courseOrProject?.trim() ?: existing.courseOrProject,
            dueDateTime = dueDateTime ?: existing.dueDateTime,
            totalPomodoros = totalPomodoros?.coerceIn(1, 12) ?: existing.totalPomodoros,
            updatedAt = System.currentTimeMillis()
        )
        
        repository.insertOrUpdateTask(updated)
        return true
    }
}
