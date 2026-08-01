package com.example.pomodoro.features.tasks.domain

import com.example.pomodoro.features.tasks.data.TaskRepository
import javax.inject.Inject

/**
 * Caso de uso para completar un ciclo de Focus para una tarea
 */
class CompleteFocusForTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    
    /**
     * Incrementa el contador de pomodoros completados para la tarea
     * @return Result con el nuevo estado: Pomodoros actuales, total, y si la tarea quedó completada
     */
    suspend operator fun invoke(taskId: Int): FocusCompletionResult {
        val task = repository.getTaskById(taskId)
            ?: return FocusCompletionResult.Error("Tarea no encontrada")
        
        val newCompleted = (task.completedPomodoros + 1).coerceAtMost(task.totalPomodoros)
        repository.updateCompletedPomodoros(taskId, newCompleted)
        
        val isTaskComplete = newCompleted >= task.totalPomodoros
        
        if (isTaskComplete) {
            repository.markTaskDone(taskId)
        }
        
        return FocusCompletionResult.Success(
            completedPomodoros = newCompleted,
            totalPomodoros = task.totalPomodoros,
            isTaskComplete = isTaskComplete,
            taskTitle = task.title
        )
    }
    
}

sealed class FocusCompletionResult {
    data class Success(
        val completedPomodoros: Int,
        val totalPomodoros: Int,
        val isTaskComplete: Boolean,
        val taskTitle: String
    ) : FocusCompletionResult() {
        val progress: Float get() = completedPomodoros.toFloat() / totalPomodoros
        val remainingPomodoros: Int get() = totalPomodoros - completedPomodoros
    }
    
    data class Error(val message: String) : FocusCompletionResult()
}
