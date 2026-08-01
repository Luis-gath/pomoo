package com.example.pomodoro.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pomodoro.features.stats.data.DailyStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getStatsForRange(startDate: String, endDate: String): Flow<List<DailyStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: DailyStatsEntity)

    @Query("UPDATE daily_stats SET focusCount = focusCount + 1, focusMinutes = focusMinutes + :minutes WHERE date = :date")
    suspend fun incrementFocusStats(date: String, minutes: Int)

    @Query("UPDATE daily_stats SET tasksCompleted = tasksCompleted + 1 WHERE date = :date")
    suspend fun incrementTaskCompleted(date: String)

    // Helper: Si no existe el registro, lo creamos primero (upsert manual si es necesario, 
    // pero insertOrUpdate suele bastar si traemos el objeto, modificamos y guardamos).
    // Para operaciones atómicas de incremento, es mejor usar las queries de arriba, 
    // pero requieren que la fila exista.
    
    // Transacción recomendada en Repository: 
    // 1. Check if exists. 
    // 2. If not, insert default. 
    // 3. Execute update.
    
    @Query("SELECT focusCount FROM daily_stats WHERE date = :date")
    fun getFocusCountForDate(date: String): Flow<Int?>

    /** Días con al menos un focus, del más reciente al más antiguo (para la racha). */
    @Query("SELECT date FROM daily_stats WHERE focusCount > 0 ORDER BY date DESC")
    fun getActiveDatesDesc(): Flow<List<String>>

    @Query("SELECT SUM(focusCount) FROM daily_stats")
    fun getTotalFocusCount(): Flow<Int?>
}
