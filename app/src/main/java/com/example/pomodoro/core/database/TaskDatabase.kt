package com.example.pomodoro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pomodoro.features.stats.data.DailyStatsEntity
import com.example.pomodoro.features.tasks.data.TaskEntity

/**
 * La instancia única se construye en [com.example.pomodoro.core.di.DataModule];
 * ya no existe un singleton manual (`getInstance`) para evitar dos instancias de Room
 * apuntando al mismo archivo.
 */
@Database(entities = [TaskEntity::class, DailyStatsEntity::class], version = 7, exportSchema = true)
abstract class TaskDatabase : RoomDatabase() {
    abstract val taskDao: TaskDao
    abstract val statsDao: StatsDao
}
