package com.example.pomodoro.data.repository

import com.example.pomodoro.data.local.TaskDao
import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.data.model.TaskStatus
import com.example.pomodoro.receiver.TaskAlarmScheduler
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao,
    private val alarmScheduler: TaskAlarmScheduler
) {
    // --- Consultas básicas ---
    
    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()
    
    suspend fun getTaskById(id: Int): TaskEntity? = taskDao.getTaskById(id)
    
    fun getTaskByIdFlow(id: Int): Flow<TaskEntity?> = taskDao.getTaskByIdFlow(id)
    
    // --- Consultas para Dashboard ---
    
    fun getTodayTasks(): Flow<List<TaskEntity>> {
        val (startOfDay, endOfDay) = getTodayBounds()
        return taskDao.getTodayTasks(startOfDay, endOfDay)
    }
    
    fun getUpcomingTasks(): Flow<List<TaskEntity>> {
        val (_, endOfDay) = getTodayBounds()
        return taskDao.getUpcomingTasks(endOfDay)
    }
    
    fun getCompletedTasks(): Flow<List<TaskEntity>> = taskDao.getCompletedTasks()
    
    fun getBacklogTasks(): Flow<List<TaskEntity>> = taskDao.getBacklogTasks()
    
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskEntity>> = 
        taskDao.getTasksByStatus(status)
    
    fun getActiveTasks(): Flow<List<TaskEntity>> = taskDao.getActiveTasks()
    
    // --- Búsqueda ---
    
    fun searchTasks(query: String): Flow<List<TaskEntity>> = taskDao.searchTasks(query)
    
    // --- CRUD ---
    
    suspend fun insertOrUpdateTask(task: TaskEntity): Long {
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
    
    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
        alarmScheduler.cancel(task.id)
    }
    
    // --- Actualizaciones de estado ---
    
    suspend fun updateTaskStatus(taskId: Int, status: TaskStatus) {
        taskDao.updateTaskStatus(taskId, status)
        if (status == TaskStatus.DONE) {
            alarmScheduler.cancel(taskId)
        }
    }
    
    suspend fun markTaskDone(taskId: Int) {
        taskDao.markTaskDone(taskId)
        alarmScheduler.cancel(taskId)
    }
    
    suspend fun markTaskAsCompleted(task: TaskEntity, isCompleted: Boolean) {
        if (isCompleted) {
            taskDao.markTaskDone(task.id)
        } else {
            taskDao.updateTaskStatus(task.id, TaskStatus.TODO)
        }
        
        if (isCompleted) {
            alarmScheduler.cancel(task.id)
        } else if (task.isNotificationEnabled && task.dueDateTime != null && task.dueDateTime > System.currentTimeMillis()) {
            alarmScheduler.schedule(task)
        }
    }
    
    // --- Pomodoro Progress ---
    
    suspend fun updateCompletedPomodoros(taskId: Int, completed: Int) {
        taskDao.updateCompletedPomodoros(taskId, completed)
    }
    
    suspend fun incrementPomodoro(taskId: Int): Boolean {
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
    
    suspend fun resetTaskProgress(taskId: Int) {
        taskDao.resetTaskProgress(taskId)
    }
    
    // --- Duplicar tarea ---
    
    suspend fun duplicateTask(task: TaskEntity): Long {
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
    
    // --- Estadísticas ---
    
    fun getCompletedTasksCount(): Flow<Int> = taskDao.getCompletedTasksCount()
    
    fun getTotalCompletedPomodoros(): Flow<Int?> = taskDao.getTotalCompletedPomodoros()
    
    fun getPendingTasksCount(): Flow<Int> = taskDao.getPendingTasksCount()
    
    // --- Utilidades privadas ---
    
    private fun getTodayBounds(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis
        
        return Pair(startOfDay, endOfDay)
    }
    
    // Legacy compatibility
    fun getTasksForDay(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>> = 
        taskDao.getTodayTasks(startOfDay, endOfDay)
    // --- Scheduling ---
    
    fun getTasksBetweenDates(start: Long, end: Long): Flow<List<TaskEntity>> {
        return taskDao.getTasksBetweenDates(start, end)
    }

    suspend fun updateTaskSchedule(taskId: Int, dueTime: Long?) {
        taskDao.updateTaskSchedule(taskId, dueTime)
    }
}
