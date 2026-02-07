package com.example.pomodoro.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.pomodoro.data.model.CustomTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.audioDataStore by preferencesDataStore(name = "audio_settings")

data class AudioSettings(
    val isMusicEnabled: Boolean = false,
    val isMuted: Boolean = false,
    val selectedTrackId: String = "lofi_beat", // Default track
    val volume: Float = 0.5f,
    val customTracks: List<CustomTrack> = emptyList()
)

class AudioSettingsDataStore(private val context: Context) {

    private val IS_MUSIC_ENABLED = booleanPreferencesKey("is_music_enabled")
    private val IS_MUTED = booleanPreferencesKey("is_muted")
    private val SELECTED_TRACK_ID = stringPreferencesKey("selected_track_id")
    private val VOLUME = floatPreferencesKey("volume")
    private val CUSTOM_TRACKS = stringPreferencesKey("custom_tracks")

    private val json = Json { ignoreUnknownKeys = true }

    val audioSettingsFlow: Flow<AudioSettings> = context.audioDataStore.data.map { preferences ->
        val customTracksJson = preferences[CUSTOM_TRACKS] ?: "[]"
        val customTracks = try {
            json.decodeFromString<List<CustomTrack>>(customTracksJson)
        } catch (e: Exception) {
            emptyList()
        }
        
        AudioSettings(
            isMusicEnabled = preferences[IS_MUSIC_ENABLED] ?: false,
            isMuted = preferences[IS_MUTED] ?: false,
            selectedTrackId = preferences[SELECTED_TRACK_ID] ?: "lofi_beat",
            volume = preferences[VOLUME] ?: 0.5f,
            customTracks = customTracks
        )
    }

    suspend fun updateMusicEnabled(enabled: Boolean) {
        context.audioDataStore.edit { it[IS_MUSIC_ENABLED] = enabled }
    }

    suspend fun updateMuted(muted: Boolean) {
        context.audioDataStore.edit { it[IS_MUTED] = muted }
    }

    suspend fun updateSelectedTrack(trackId: String) {
        context.audioDataStore.edit { it[SELECTED_TRACK_ID] = trackId }
    }

    suspend fun updateVolume(volume: Float) {
        context.audioDataStore.edit { it[VOLUME] = volume }
    }

    suspend fun addCustomTrack(track: CustomTrack) {
        context.audioDataStore.edit { preferences ->
            val currentJson = preferences[CUSTOM_TRACKS] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<CustomTrack>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            currentList.add(track)
            preferences[CUSTOM_TRACKS] = json.encodeToString(currentList)
        }
    }

    suspend fun removeCustomTrack(trackId: String) {
        context.audioDataStore.edit { preferences ->
            val currentJson = preferences[CUSTOM_TRACKS] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<CustomTrack>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            currentList.removeAll { it.id == trackId }
            preferences[CUSTOM_TRACKS] = json.encodeToString(currentList)
        }
    }
}
