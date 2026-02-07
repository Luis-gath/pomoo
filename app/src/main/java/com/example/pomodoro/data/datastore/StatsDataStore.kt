package com.example.pomodoro.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val Context.statsDataStore: DataStore<Preferences> by preferencesDataStore(name = "stats_v2")

class StatsDataStore(private val context: Context) {

    private object Keys {
        val HISTORY_JSON = stringPreferencesKey("history_json")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val LAST_ACTIVITY_DATE = stringPreferencesKey("last_activity_date")
        val TOTAL_SESSIONS = intPreferencesKey("total_sessions")
    }

    data class StatsState(
        val sessionsToday: Int,
        val sessionsTotal: Int,
        val currentStreak: Int,
        val weeklyHistory: List<Pair<String, Int>> // DayName -> Count
    )

    val statsFlow: Flow<StatsState> = context.statsDataStore.data.map { preferences ->
        val historyJsonStr = preferences[Keys.HISTORY_JSON] ?: "{}"
        val total = preferences[Keys.TOTAL_SESSIONS] ?: 0
        val streak = preferences[Keys.CURRENT_STREAK] ?: 0
        
        val historyMap = parseHistory(historyJsonStr)
        val todayDate = getTodayDate()
        val todayCount = historyMap[todayDate] ?: 0

        // Weekly Params
        val weeklyData = getLast7DaysData(historyMap)

        StatsState(
            sessionsToday = todayCount,
            sessionsTotal = total,
            currentStreak = streak,
            weeklyHistory = weeklyData
        )
    }

    suspend fun recordSession() {
        context.statsDataStore.edit { preferences ->
            val today = getTodayDate()
            val historyJsonStr = preferences[Keys.HISTORY_JSON] ?: "{}"
            val historyMap = parseHistory(historyJsonStr).toMutableMap()
            
            // Update Today
            val currentCount = historyMap[today] ?: 0
            historyMap[today] = currentCount + 1
            
            // Save History
            preferences[Keys.HISTORY_JSON] = mapToJson(historyMap)
            
            // Update Total
            val currentTotal = preferences[Keys.TOTAL_SESSIONS] ?: 0
            preferences[Keys.TOTAL_SESSIONS] = currentTotal + 1
            
            // Update Streak
            val lastDateStr = preferences[Keys.LAST_ACTIVITY_DATE]
            val currentStreak = preferences[Keys.CURRENT_STREAK] ?: 0
            
            if (lastDateStr == today) {
                // Already active today, streak doesn't change
            } else if (lastDateStr != null && isYesterday(lastDateStr)) {
                // Continued streak
                preferences[Keys.CURRENT_STREAK] = currentStreak + 1
            } else {
                // Reset streak (unless it's the first ever session)
                 if (lastDateStr == null) {
                     preferences[Keys.CURRENT_STREAK] = 1
                 } else {
                     // Missed a day
                     preferences[Keys.CURRENT_STREAK] = 1
                 }
            }
            preferences[Keys.LAST_ACTIVITY_DATE] = today
        }
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
            e.printStackTrace()
        }
        return map
    }

    private fun mapToJson(map: Map<String, Int>): String {
        val json = JSONObject()
        map.forEach { (key, value) ->
            json.put(key, value)
        }
        return json.toString()
    }

    private fun getTodayDate(): String {
        return LocalDate.now().toString() // YYYY-MM-DD
    }
    
    private fun isYesterday(dateStr: String): Boolean {
        try {
            val date = LocalDate.parse(dateStr)
            val yesterday = LocalDate.now().minusDays(1)
            return date.isEqual(yesterday)
        } catch (e: Exception) {
            return false
        }
    }

    private fun getLast7DaysData(history: Map<String, Int>): List<Pair<String, Int>> {
        val list = mutableListOf<Pair<String, Int>>()
        val today = LocalDate.now()
        // Last 7 days including today
        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.toString()
            // Format for display: "Mon", "Tue" etc. or simple day number. 
            // Let's use DayOfWeek short name EN. User asked for Spanish. 
            // Simple mapping for ease.
            val dayName = when(date.dayOfWeek.value) {
                1 -> "L"
                2 -> "M"
                3 -> "X"
                4 -> "J"
                5 -> "V"
                6 -> "S"
                7 -> "D"
                else -> "?"
            }
            list.add(dayName to (history[dateStr] ?: 0))
        }
        return list
    }
}
