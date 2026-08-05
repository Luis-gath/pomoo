package com.example.pomodoro.core.di

import android.content.Context
import androidx.room.Room
import com.example.pomodoro.core.time.Clock
import com.example.pomodoro.core.time.SystemClock
import com.example.pomodoro.features.premium.data.BillingManager
import com.example.pomodoro.core.audio.AudioSettingsDataStore
import com.example.pomodoro.features.premium.data.PremiumStorage
import com.example.pomodoro.features.timer.data.SettingsDataStore
import com.example.pomodoro.features.stats.data.LegacyStatsStore
import com.example.pomodoro.core.database.StatsDao
import com.example.pomodoro.core.database.TaskDao
import com.example.pomodoro.core.database.AreaDao
import com.example.pomodoro.core.database.ItemDao
import com.example.pomodoro.core.database.MIGRATION_7_8
import com.example.pomodoro.core.database.MIGRATION_8_9
import com.example.pomodoro.core.database.MIGRATION_9_10
import com.example.pomodoro.core.database.TaskDatabase
import com.example.pomodoro.core.feedback.SessionFeedback
import com.example.pomodoro.core.focus.DoNotDisturbController
import com.example.pomodoro.features.areas.data.FileImporter
import com.example.pomodoro.features.areas.data.DeliverableAlarmScheduler
import com.example.pomodoro.core.notification.DeliverableNotifier
import com.example.pomodoro.features.timer.domain.PomodoroEngine
import com.example.pomodoro.core.notification.NotificationHelper
import com.example.pomodoro.features.tasks.data.TaskAlarmScheduler
import com.example.pomodoro.core.audio.AudioPlayerManager
import com.example.pomodoro.core.notification.TaskReminderNotifier
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Punto único de construcción del grafo de dependencias.
 * Antes este grafo se reconstruía a mano en NavGraph, los ViewModels, el Service y el Receiver.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // --- Persistencia ---

    @Provides
    @Singleton
    fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase =
        Room.databaseBuilder(context, TaskDatabase::class.java, "task_database")
            // Solo se destruye al venir de versiones antiguas cuyo esquema nunca se exportó.
            // A partir de la 7 (la actual) cualquier cambio EXIGE una Migration explícita:
            // sin ella Room lanza excepción en vez de borrar los datos del usuario.
            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .build()

    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao

    @Provides
    fun provideStatsDao(database: TaskDatabase): StatsDao = database.statsDao

    @Provides
    fun provideAreaDao(database: TaskDatabase): AreaDao = database.areaDao

    @Provides
    fun provideItemDao(database: TaskDatabase): ItemDao = database.itemDao

    // --- Preferencias ---

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context) = SettingsDataStore(context)

    @Provides
    @Singleton
    fun provideAudioSettingsDataStore(@ApplicationContext context: Context) =
        AudioSettingsDataStore(context)

    @Provides
    @Singleton
    fun provideLegacyStatsStore(@ApplicationContext context: Context) = LegacyStatsStore(context)

    @Provides
    @Singleton
    fun provideFileImporter(@ApplicationContext context: Context) = FileImporter(context)

    @Provides
    @Singleton
    fun providePremiumStorage(@ApplicationContext context: Context) = PremiumStorage(context)

    // --- Dominio ---

    @Provides
    @Singleton
    fun providePomodoroEngine() = PomodoroEngine()

    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock()

    // --- Infraestructura Android ---

    @Provides
    @Singleton
    fun provideTaskAlarmScheduler(@ApplicationContext context: Context) = TaskAlarmScheduler(context)

    @Provides
    @Singleton
    fun provideAudioPlayerManager(@ApplicationContext context: Context) = AudioPlayerManager(context)

    @Provides
    @Singleton
    fun provideTimerNotificationHelper(@ApplicationContext context: Context) =
        NotificationHelper(context)

    @Provides
    @Singleton
    fun provideSessionFeedback(@ApplicationContext context: Context) = SessionFeedback(context)

    @Provides
    @Singleton
    fun provideDoNotDisturbController(@ApplicationContext context: Context) =
        DoNotDisturbController(context)

    @Provides
    @Singleton
    fun provideTaskReminderNotifier(@ApplicationContext context: Context) =
        TaskReminderNotifier(context)

    @Provides
    @Singleton
    fun provideDeliverableAlarmScheduler(@ApplicationContext context: Context) =
        DeliverableAlarmScheduler(context)

    @Provides
    @Singleton
    fun provideDeliverableNotifier(@ApplicationContext context: Context) =
        DeliverableNotifier(context)

    // --- Autenticación ---

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    // --- Concurrencia ---

    @Provides
    @Singleton
    @AppScope
    fun provideAppCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // --- Facturación ---

    @Provides
    @Singleton
    fun provideBillingManager(
        @ApplicationContext context: Context,
        premiumStorage: PremiumStorage,
        @AppScope externalScope: CoroutineScope
    ) = BillingManager(context, premiumStorage, externalScope)
}
