package com.example.pomodoro.features.tasks.data

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a tareas. La implementación concreta vive en [TaskRepositoryImpl];
 * depender de esta interfaz permite sustituirla por fakes en tests y cambiar la fuente
 * de datos sin tocar ViewModels ni casos de uso.
 *
 * Solo expone lo que la app usa hoy: la clasificación en hoy / próximas / completadas se
 * hace en memoria en el ViewModel a partir de [getAllTasks], así que no hacen falta
 * consultas específicas por cada filtro.
 */
interface TaskRepository {

    // --- Consultas ---

    fun getAllTasks(): Flow<List<TaskEntity>>

    suspend fun getTaskById(id: Int): TaskEntity?

    fun getTaskByIdFlow(id: Int): Flow<TaskEntity?>

    // --- CRUD ---

    suspend fun insertOrUpdateTask(task: TaskEntity): Long

    suspend fun deleteTask(task: TaskEntity)

    /** Quita el horario completo de un curso. */
    suspend fun deleteClassGroup(classGroupId: String)

    suspend fun duplicateTask(task: TaskEntity): Long

    // --- Estado ---

    suspend fun updateTaskStatus(taskId: Int, status: TaskStatus)

    suspend fun markTaskDone(taskId: Int)

    // --- Progreso Pomodoro ---

    suspend fun updateCompletedPomodoros(taskId: Int, completed: Int)

    suspend fun incrementPomodoro(taskId: Int): Boolean

    suspend fun resetTaskProgress(taskId: Int)
}
