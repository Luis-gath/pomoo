package com.example.pomodoro.features.timer.data

import com.example.pomodoro.features.timer.domain.Settings
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de ajustes globales. Lo implementa
 * [com.example.pomodoro.features.timer.data.SettingsDataStore]; existir como interfaz permite
 * que la máquina de estados del Pomodoro se testee sin Context de Android.
 */
interface SettingsRepository {
    val settingsFlow: Flow<Settings>
    suspend fun updateSettings(settings: Settings)
}
