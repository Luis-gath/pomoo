package com.example.pomodoro.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // --- Consultas ---

    @Query("SELECT * FROM tasks ORDER BY dueDateTime ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskByIdFlow(id: Int): Flow<TaskEntity?>

    // --- Inserciones y borrado ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

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
}
