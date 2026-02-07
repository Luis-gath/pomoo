package com.example.pomodoro.domain.usecases

import com.example.pomodoro.data.repository.TaskRepository

/**
 * Caso de uso para marcar una tarea como completada (DONE)
 */
class MarkTaskDoneUseCase(private val repository: TaskRepository) {
    
    /**
     * Marca la tarea como completada
     */
    suspend operator fun invoke(taskId: Int): Boolean {
        val task = repository.getTaskById(taskId) ?: return false
        repository.markTaskDone(taskId)
        return true
    }
    
    /**
     * Reinicia el progreso de una tarea (vuelve a TODO con 0 pomodoros)
     */
    suspend fun resetProgress(taskId: Int): Boolean {
        val task = repository.getTaskById(taskId) ?: return false
        repository.resetTaskProgress(taskId)
        return true
    }
    
    /**
     * Alterna el estado de completado de una tarea
     */
    suspend fun toggle(taskId: Int): Boolean {
        val task = repository.getTaskById(taskId) ?: return false
        repository.markTaskAsCompleted(task, !task.isDone)
        return true
    }
}
