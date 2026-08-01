package com.example.pomodoro

import android.app.Application
import com.example.pomodoro.core.di.AppScope
import com.example.pomodoro.features.stats.data.LegacyStatsMigrator
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PomodoroApp : Application() {

    @Inject lateinit var legacyStatsMigrator: LegacyStatsMigrator

    @Inject @field:AppScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Importación única del historial antiguo de estadísticas hacia Room.
        appScope.launch { legacyStatsMigrator.migrateIfNeeded() }
    }
}
