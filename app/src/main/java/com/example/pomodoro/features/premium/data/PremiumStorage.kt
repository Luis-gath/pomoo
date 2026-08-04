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

/**
 * Estado premium del usuario.
 *
 * Es un reflejo de lo que Play declara activo, no una verdad propia: [reconcile] lo
 * reescribe entero en cada consulta de compras. Esto es lo que permite retirar el
 * acceso tras un reembolso o una cancelación, cosa que antes no ocurría nunca.
 *
 * Sigue siendo una copia local y manipulable en un dispositivo rooteado. Cerrar eso
 * exige validar contra servidor con la Google Play Developer API.
 */
class PremiumStorage(private val context: Context) {

    private object Keys {
        val IS_LIFETIME = booleanPreferencesKey("is_lifetime")
        val SUBSCRIPTION_ACTIVE = booleanPreferencesKey("subscription_active")

        /** Solo para mostrar. 0 significa que no lo sabemos. */
        val SUBSCRIPTION_RENEWS_AT = longPreferencesKey("subscription_renews_at")
    }

    val isPremium: Flow<Boolean> = context.premiumDataStore.data.map { prefs ->
        prefs[Keys.IS_LIFETIME] == true || prefs[Keys.SUBSCRIPTION_ACTIVE] == true
    }

    val isLifetime: Flow<Boolean> = context.premiumDataStore.data.map { prefs ->
        prefs[Keys.IS_LIFETIME] == true
    }

    val subscriptionActive: Flow<Boolean> = context.premiumDataStore.data.map { prefs ->
        prefs[Keys.SUBSCRIPTION_ACTIVE] == true
    }

    /** Fecha aproximada de renovación, o null si no se pudo determinar. */
    val subscriptionRenewsAt: Flow<Long?> = context.premiumDataStore.data.map { prefs ->
        prefs[Keys.SUBSCRIPTION_RENEWS_AT]?.takeIf { it > 0L }
    }

    /**
     * Reescribe el estado a partir de lo que Play considera vigente ahora mismo.
     *
     * Se pasa el conjunto completo a propósito: conceder y retirar tienen que ocurrir
     * en la misma operación, porque una compra ausente es precisamente la señal de que
     * hubo cancelación, caducidad o reembolso.
     */
    suspend fun reconcile(
        lifetimeOwned: Boolean,
        subscriptionActive: Boolean,
        subscriptionRenewsAt: Long?
    ) {
        context.premiumDataStore.edit { prefs ->
            prefs[Keys.IS_LIFETIME] = lifetimeOwned
            prefs[Keys.SUBSCRIPTION_ACTIVE] = subscriptionActive
            prefs[Keys.SUBSCRIPTION_RENEWS_AT] = subscriptionRenewsAt ?: 0L
        }
    }
}
