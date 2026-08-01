package com.example.pomodoro.features.premium.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.premiumDataStore: DataStore<Preferences> by preferencesDataStore(name = "premium_prefs")

class PremiumStorage(private val context: Context) {

    private object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_UNTIL = longPreferencesKey("premium_until")
    }

    val isPremium: Flow<Boolean> = context.premiumDataStore.data.map { preferences ->
        val isPrem = preferences[Keys.IS_PREMIUM] ?: false
        val until = preferences[Keys.PREMIUM_UNTIL] ?: 0L
        if (isPrem) {
            true
        } else {
            // Check if subscription time is still valid
            until > System.currentTimeMillis()
        }
    }

    val premiumUntil: Flow<Long?> = context.premiumDataStore.data.map { preferences ->
        preferences[Keys.PREMIUM_UNTIL]
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.premiumDataStore.edit { preferences ->
            preferences[Keys.IS_PREMIUM] = isPremium
            // If setting directly to true (lifetime), maybe clear until or keep it?
            // User requirement: "Permanente (compra única)" -> isPremium = true.
        }
    }

    suspend fun setPremiumUntil(timestamp: Long) {
        context.premiumDataStore.edit { preferences ->
            preferences[Keys.PREMIUM_UNTIL] = timestamp
            // If we have a future date, we are effectively premium
        }
    }
}
