package com.example.pomodoro.data.repository

import com.example.pomodoro.data.local.StatsDao
import com.example.pomodoro.data.local.TaskDao
import com.example.pomodoro.data.model.DailyStatsEntity
import com.example.pomodoro.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StatsRepository(
    private val statsDao: StatsDao,
    private val taskDao: TaskDao
) {

    // --- Daily Stats Operations ---

    fun getStatsForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyStatsEntity>> {
        val startStr = startDate.toString()
        val endStr = endDate.toString()
        return statsDao.getStatsForRange(startStr, endStr)
    }
    
    fun getTotalFocusCount(): Flow<Int> = statsDao.getTotalFocusCount().map { it ?: 0 }
    
    fun getTotalFocusMinutes(): Flow<Int> = statsDao.getTotalFocusMinutes().map { it ?: 0 }

    suspend fun recordFocusSession(minutes: Int) {
        val today = LocalDate.now().toString()
        val existing = statsDao.getStatsForDate(today)
        if (existing == null) {
            statsDao.insertOrUpdate(DailyStatsEntity(date = today, focusCount = 1, focusMinutes = minutes))
        } else {
            statsDao.incrementFocusStats(today, minutes)
        }
    }

    suspend fun recordTaskCompletion() {
        val today = LocalDate.now().toString()
        val existing = statsDao.getStatsForDate(today)
        if (existing == null) {
            statsDao.insertOrUpdate(DailyStatsEntity(date = today, tasksCompleted = 1))
        } else {
            statsDao.incrementTaskCompleted(today)
        }
    }
    
    // --- Top Lists Operations (Delegated to TaskDao filters logic usually, or in-memory) ---
    // Assuming TaskDao has basic getAllTasks, we filter here or add specialized queries later.
    // For now simple in-memory sort from TaskDao flows if needed or adding specificDAO queries.
    
    // Top Tasks by Pomodoros
    fun getTopTasksByPomodoros(): Flow<List<TaskEntity>> {
        return taskDao.getAllTasks().map { list ->
            list.filter { it.completedPomodoros > 0 }
                .sortedByDescending { it.completedPomodoros }
                .take(5)
        }
    }
}
