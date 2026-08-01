package com.example.pomodoro.features.stats.data

import com.example.pomodoro.features.tasks.data.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Contrato de acceso a estadísticas diarias. Implementación concreta en [StatsRepositoryImpl].
 */
interface StatsRepository {

    fun getStatsForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyStatsEntity>>

    fun getTotalFocusCount(): Flow<Int>

    /** Focus completados hoy. Sustituye al contador que vivía en LegacyStatsStore. */
    fun getTodayFocusCount(): Flow<Int>

    /** Racha de días consecutivos con al menos un focus. */
    fun getCurrentStreak(): Flow<Int>

    suspend fun recordFocusSession(minutes: Int)

    suspend fun recordTaskCompletion()

    fun getTopTasksByPomodoros(): Flow<List<TaskEntity>>
}
