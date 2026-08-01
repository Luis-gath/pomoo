package com.example.pomodoro.features.timer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.pomodoro.features.timer.domain.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) : SettingsRepository {

    private object Keys {
        val FOCUS_DURATION = intPreferencesKey("focus_duration")
        val SHORT_BREAK_DURATION = intPreferencesKey("short_break_duration")
        val LONG_BREAK_DURATION = intPreferencesKey("long_break_duration")
        val LONG_BREAK_EVERY_N = intPreferencesKey("long_break_every_n")
        val AUTO_START = booleanPreferencesKey("auto_start")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val BACKGROUND_URI = stringPreferencesKey("background_uri")
    }

    override val settingsFlow: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
            focusDurationMinutes = preferences[Keys.FOCUS_DURATION] ?: 25,
            shortBreakDurationMinutes = preferences[Keys.SHORT_BREAK_DURATION] ?: 5,
            longBreakDurationMinutes = preferences[Keys.LONG_BREAK_DURATION] ?: 15,
            longBreakEveryNCycles = preferences[Keys.LONG_BREAK_EVERY_N] ?: 4,
            autoStartNext = preferences[Keys.AUTO_START] ?: false,
            soundEnabled = preferences[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = preferences[Keys.VIBRATION_ENABLED] ?: true,
            keepScreenOn = preferences[Keys.KEEP_SCREEN_ON] ?: false,
            backgroundUri = preferences[Keys.BACKGROUND_URI]
        )
    }

    override suspend fun updateSettings(settings: Settings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.FOCUS_DURATION] = settings.focusDurationMinutes
            preferences[Keys.SHORT_BREAK_DURATION] = settings.shortBreakDurationMinutes
            preferences[Keys.LONG_BREAK_DURATION] = settings.longBreakDurationMinutes
            preferences[Keys.LONG_BREAK_EVERY_N] = settings.longBreakEveryNCycles
            preferences[Keys.AUTO_START] = settings.autoStartNext
            preferences[Keys.SOUND_ENABLED] = settings.soundEnabled
            preferences[Keys.VIBRATION_ENABLED] = settings.vibrationEnabled
            preferences[Keys.KEEP_SCREEN_ON] = settings.keepScreenOn
            if (settings.backgroundUri != null) {
                preferences[Keys.BACKGROUND_URI] = settings.backgroundUri
            } else {
                preferences.remove(Keys.BACKGROUND_URI)
            }
        }
    }
}
