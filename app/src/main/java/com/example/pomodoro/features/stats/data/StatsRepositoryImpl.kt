package com.example.pomodoro.features.stats.data

import com.example.pomodoro.core.database.StatsDao
import com.example.pomodoro.core.database.TaskDao
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.stats.domain.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val statsDao: StatsDao,
    private val taskDao: TaskDao
) : StatsRepository {

    // --- Daily Stats Operations ---

    override fun getStatsForRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailyStatsEntity>> = statsDao.getStatsForRange(startDate.toString(), endDate.toString())

    override fun getTotalFocusCount(): Flow<Int> = statsDao.getTotalFocusCount().map { it ?: 0 }

    override fun getTodayFocusCount(): Flow<Int> =
        statsDao.getFocusCountForDate(LocalDate.now().toString()).map { it ?: 0 }

    override fun getCurrentStreak(): Flow<Int> =
        statsDao.getActiveDatesDesc().map { dates ->
            StreakCalculator.currentStreak(
                activeDatesDesc = dates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() },
                today = LocalDate.now()
            )
        }

    override suspend fun recordFocusSession(minutes: Int) {
        val today = LocalDate.now().toString()
        val existing = statsDao.getStatsForDate(today)
        if (existing == null) {
            statsDao.insertOrUpdate(DailyStatsEntity(date = today, focusCount = 1, focusMinutes = minutes))
        } else {
            statsDao.incrementFocusStats(today, minutes)
        }
    }

    override suspend fun recordTaskCompletion() {
        val today = LocalDate.now().toString()
        val existing = statsDao.getStatsForDate(today)
        if (existing == null) {
            statsDao.insertOrUpdate(DailyStatsEntity(date = today, tasksCompleted = 1))
        } else {
            statsDao.incrementTaskCompleted(today)
        }
    }

    // --- Top Lists ---

    override fun getTopTasksByPomodoros(): Flow<List<TaskEntity>> =
        taskDao.getAllTasks().map { list ->
            list.filter { it.completedPomodoros > 0 }
                .sortedByDescending { it.completedPomodoros }
                .take(5)
        }
}
