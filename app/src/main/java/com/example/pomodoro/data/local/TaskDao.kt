package com.example.pomodoro.data.local

import androidx.room.*
import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    
    // --- Consultas básicas ---
    
    @Query("SELECT * FROM tasks ORDER BY dueDateTime ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): TaskEntity?
    
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskByIdFlow(id: Int): Flow<TaskEntity?>
    
    // --- Consultas por estado ---
    
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueDateTime ASC, priority DESC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE status != 'DONE' ORDER BY dueDateTime ASC, priority DESC")
    fun getActiveTasks(): Flow<List<TaskEntity>>
    
    // --- Consultas para Dashboard ---
    
    /**
     * Tareas de hoy (con fecha límite hoy y no completadas)
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE dueDateTime >= :startOfDay 
        AND dueDateTime < :endOfDay 
        AND status != 'DONE'
        ORDER BY priority DESC, dueDateTime ASC
    """)
    fun getTodayTasks(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>
    
    /**
     * Tareas próximas (fecha futura o sin fecha, no completadas)
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE (dueDateTime >= :afterTime OR dueDateTime IS NULL)
        AND status != 'DONE'
        ORDER BY CASE WHEN dueDateTime IS NULL THEN 1 ELSE 0 END, dueDateTime ASC, priority DESC
    """)
    fun getUpcomingTasks(afterTime: Long): Flow<List<TaskEntity>>
    
    /**
     * Tareas completadas ordenadas por fecha de completado
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE status = 'DONE'
        ORDER BY completedAt DESC, updatedAt DESC
    """)
    fun getCompletedTasks(): Flow<List<TaskEntity>>
    
    /**
     * Tareas sin fecha límite (backlog)
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE dueDateTime IS NULL 
        AND status != 'DONE'
        ORDER BY priority DESC, createdAt DESC
    """)
    fun getBacklogTasks(): Flow<List<TaskEntity>>
    
    // --- Búsqueda ---
    
    @Query("""
        SELECT * FROM tasks 
        WHERE (title LIKE '%' || :query || '%' 
        OR notes LIKE '%' || :query || '%'
        OR courseOrProject LIKE '%' || :query || '%')
        ORDER BY status ASC, dueDateTime ASC
    """)
    fun searchTasks(query: String): Flow<List<TaskEntity>>
    
    // --- Calendar & Scheduling ---

    @Query("SELECT * FROM tasks WHERE dueDateTimeMillis >= :startMillis AND dueDateTimeMillis <= :endMillis ORDER BY dueDateTimeMillis ASC")
    fun getTasksBetweenDates(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET dueDateTimeMillis = :dueTime WHERE id = :taskId")
    suspend fun updateTaskSchedule(taskId: Int, dueTime: Long?)
    
    // --- Inserciones y actualizaciones ---
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    // --- Actualizaciones parciales ---
    
    @Query("UPDATE tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, status: TaskStatus, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE tasks SET completedPomodoros = :completed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCompletedPomodoros(id: Int, completed: Int, updatedAt: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE tasks 
        SET status = 'DONE', 
            completedAt = :completedAt, 
            updatedAt = :updatedAt,
            isCompleted = 1
        WHERE id = :id
    """)
    suspend fun markTaskDone(id: Int, completedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE tasks SET completedPomodoros = 0, status = 'TODO', updatedAt = :updatedAt WHERE id = :id")
    suspend fun resetTaskProgress(id: Int, updatedAt: Long = System.currentTimeMillis())
    
    // --- Estadísticas ---
    
    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'DONE'")
    fun getCompletedTasksCount(): Flow<Int>
    
    @Query("SELECT SUM(completedPomodoros) FROM tasks")
    fun getTotalCompletedPomodoros(): Flow<Int?>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE status != 'DONE'")
    fun getPendingTasksCount(): Flow<Int>
}
