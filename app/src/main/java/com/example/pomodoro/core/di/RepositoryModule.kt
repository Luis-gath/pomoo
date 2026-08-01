package com.example.pomodoro.core.di

import com.example.pomodoro.features.timer.data.SettingsDataStore
import com.example.pomodoro.features.timer.data.SettingsRepository
import com.example.pomodoro.features.stats.data.StatsRepository
import com.example.pomodoro.features.stats.data.StatsRepositoryImpl
import com.example.pomodoro.features.tasks.data.TaskRepository
import com.example.pomodoro.features.tasks.data.TaskRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Enlaza cada contrato de repositorio con su implementación concreta.
 * Los consumidores dependen solo de la interfaz, lo que permite inyectar fakes en tests.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository
}
