package com.example.pomodoro.features.tasks.data

import com.example.pomodoro.core.database.TaskDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmScheduler: TaskAlarmScheduler
) : TaskRepository {

    // --- Consultas ---

    override fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    override suspend fun getTaskById(id: Int): TaskEntity? = taskDao.getTaskById(id)

    override fun getTaskByIdFlow(id: Int): Flow<TaskEntity?> = taskDao.getTaskByIdFlow(id)

    // --- CRUD ---

    override suspend fun insertOrUpdateTask(task: TaskEntity): Long {
        val now = System.currentTimeMillis()
        val taskToSave = task.copy(updatedAt = now)

        val id = taskDao.insertTask(taskToSave)
        val taskWithId = if (task.id == 0) taskToSave.copy(id = id.toInt()) else taskToSave

        // Manejar notificaciones
        if (taskWithId.isNotificationEnabled && taskWithId.dueDateTime != null) {
            alarmScheduler.schedule(taskWithId)
        } else {
            alarmScheduler.cancel(taskWithId.id)
        }

        return if (task.id == 0) id else task.id.toLong()
    }

    override suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
        alarmScheduler.cancel(task.id)
    }

    override suspend fun deleteClassGroup(classGroupId: String) =
        taskDao.deleteByClassGroup(classGroupId)

    override suspend fun duplicateTask(task: TaskEntity): Long {
        val now = System.currentTimeMillis()
        val duplicated = task.copy(
            id = 0,
            title = "${task.title} (copia)",
            status = TaskStatus.TODO,
            completedPomodoros = 0,
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            isCompleted = false
        )
        return taskDao.insertTask(duplicated)
    }

    // --- Estado ---

    override suspend fun updateTaskStatus(taskId: Int, status: TaskStatus) {
        taskDao.updateTaskStatus(taskId, status)
        if (status == TaskStatus.DONE) {
            alarmScheduler.cancel(taskId)
        }
    }

    override suspend fun markTaskDone(taskId: Int) {
        taskDao.markTaskDone(taskId)
        alarmScheduler.cancel(taskId)
    }

    // --- Progreso Pomodoro ---

    override suspend fun updateCompletedPomodoros(taskId: Int, completed: Int) {
        taskDao.updateCompletedPomodoros(taskId, completed)
    }

    override suspend fun incrementPomodoro(taskId: Int): Boolean {
        val task = taskDao.getTaskById(taskId) ?: return false
        val newCompleted = (task.completedPomodoros + 1).coerceAtMost(task.totalPomodoros)

        taskDao.updateCompletedPomodoros(taskId, newCompleted)

        // Si completó todos los pomodoros, marcar como DONE
        if (newCompleted >= task.totalPomodoros) {
            taskDao.markTaskDone(taskId)
            return true // Indica que la tarea está completa
        }
        return false
    }

    override suspend fun resetTaskProgress(taskId: Int) {
        taskDao.resetTaskProgress(taskId)
    }
}
