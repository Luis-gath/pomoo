package com.example.pomodoro.features.stats.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONObject

private val Context.legacyStatsStore: DataStore<Preferences> by preferencesDataStore(name = "stats_v2")

/**
 * Almacén de estadísticas de la versión anterior: el historial vivía como un blob JSON
 * dentro de Preferences.
 *
 * Ya no se escribe aquí — la fuente única es Room. Solo se conserva la parte de lectura
 * para que [LegacyStatsMigrator] importe el historial una vez en los dispositivos que
 * vienen de la versión antigua. Cuando esa migración esté extendida entre los usuarios,
 * este fichero y el DataStore "stats_v2" pueden eliminarse por completo.
 */
class LegacyStatsStore(private val context: Context) {

    private object Keys {
        val HISTORY_JSON = stringPreferencesKey("history_json")
        val MIGRATED_TO_ROOM = booleanPreferencesKey("migrated_to_room")
    }

    /** Historial antiguo: fecha (YYYY-MM-DD) -> número de focus completados. */
    suspend fun rawHistory(): Map<String, Int> {
        val preferences = context.legacyStatsStore.data.first()
        return parseHistory(preferences[Keys.HISTORY_JSON] ?: "{}")
    }

    suspend fun isMigratedToRoom(): Boolean =
        context.legacyStatsStore.data.first()[Keys.MIGRATED_TO_ROOM] ?: false

    suspend fun markMigratedToRoom() {
        context.legacyStatsStore.edit { it[Keys.MIGRATED_TO_ROOM] = true }
    }

    private fun parseHistory(jsonStr: String): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getInt(key)
            }
        } catch (e: Exception) {
            Log.e("LegacyStatsStore", "Historial antiguo corrupto; se ignora", e)
        }
        return map
    }
}
