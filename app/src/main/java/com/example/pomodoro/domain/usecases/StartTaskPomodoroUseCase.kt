package com.example.pomodoro.domain.usecases

import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.data.model.TaskStatus
import com.example.pomodoro.data.repository.TaskRepository

/**
 * Caso de uso para iniciar un Pomodoro con la configuración de una tarea
 */
class StartTaskPomodoroUseCase(private val repository: TaskRepository) {
    
    /**
     * Prepara la tarea para iniciar un pomodoro
     * @return La tarea actualizada con status DOING, o null si no existe
     */
    suspend operator fun invoke(taskId: Int): TaskEntity? {
        val task = repository.getTaskById(taskId) ?: return null
        
        // Si la tarea ya está completada, no hacer nada
        if (task.status == TaskStatus.DONE) {
            return task
        }
        
        // Cambiar estado a DOING
        if (task.status != TaskStatus.DOING) {
            repository.updateTaskStatus(taskId, TaskStatus.DOING)
        }
        
        return repository.getTaskById(taskId)
    }
    
    /**
     * Obtiene la configuración Pomodoro de la tarea
     */
    suspend fun getTaskConfig(taskId: Int): TaskPomodoroConfig? {
        val task = repository.getTaskById(taskId) ?: return null
        
        return TaskPomodoroConfig(
            taskId = task.id,
            taskTitle = task.title,
            audioUri = task.audioUri,
            totalPomodoros = task.totalPomodoros,
            completedPomodoros = task.completedPomodoros,
            focusMinutes = task.focusMinutes,
            shortBreakMinutes = task.shortBreakMinutes,
            longBreakMinutes = task.longBreakMinutes,
            longBreakEvery = task.longBreakEvery,
            autoStartNext = task.taskAutoStartNext
        )
    }
}

/**
 * Configuración Pomodoro extraída de una tarea
 */
data class TaskPomodoroConfig(
    val taskId: Int,
    val taskTitle: String,
    val audioUri: String?,
    val totalPomodoros: Int,
    val completedPomodoros: Int,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val longBreakEvery: Int,
    val autoStartNext: Boolean
)
