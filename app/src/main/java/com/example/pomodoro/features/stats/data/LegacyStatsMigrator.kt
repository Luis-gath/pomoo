package com.example.pomodoro.features.stats.data

import android.util.Log
import com.example.pomodoro.core.database.StatsDao
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LegacyStatsMigrator"

/**
 * Importa una sola vez el historial de focus que vivía en DataStore (JSON) hacia Room,
 * que pasa a ser la única fuente de estadísticas.
 *
 * Los minutos no existían en el formato antiguo, así que las filas importadas quedan con
 * focusMinutes = 0; el conteo de sesiones y la racha sí se conservan.
 */
@Singleton
class LegacyStatsMigrator @Inject constructor(
    private val legacyStatsStore: LegacyStatsStore,
    private val statsDao: StatsDao
) {

    suspend fun migrateIfNeeded() {
        try {
            if (legacyStatsStore.isMigratedToRoom()) return

            val history = legacyStatsStore.rawHistory()
            for ((date, focusCount) in history) {
                if (focusCount <= 0) continue
                // No pisar datos que Room ya tenga: Room manda.
                if (statsDao.getStatsForDate(date) == null) {
                    statsDao.insertOrUpdate(
                        DailyStatsEntity(date = date, focusCount = focusCount, focusMinutes = 0)
                    )
                }
            }

            legacyStatsStore.markMigratedToRoom()
            Log.i(TAG, "Historial de estadísticas migrado a Room (${history.size} días)")
        } catch (e: Exception) {
            // Si falla, se reintenta en el siguiente arranque; nunca debe tumbar la app.
            Log.e(TAG, "No se pudo migrar el historial de estadísticas", e)
        }
    }
}
