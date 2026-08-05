package com.example.pomodoro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pomodoro.features.areas.data.Area
import com.example.pomodoro.features.areas.data.Item
import com.example.pomodoro.features.stats.data.DailyStatsEntity
import com.example.pomodoro.features.tasks.data.TaskEntity

/**
 * La instancia única se construye en [com.example.pomodoro.core.di.DataModule];
 * ya no existe un singleton manual (`getInstance`) para evitar dos instancias de Room
 * apuntando al mismo archivo.
 *
 * Versión 10: permite archivar una entrega hecha mediante [MIGRATION_9_10], sin perder datos.
 */
@Database(
    entities = [
        TaskEntity::class,
        DailyStatsEntity::class,
        Area::class,
        Item::class
    ],
    version = 10,
    exportSchema = true
)
abstract class TaskDatabase : RoomDatabase() {
    abstract val taskDao: TaskDao
    abstract val statsDao: StatsDao
    abstract val areaDao: AreaDao
    abstract val itemDao: ItemDao
}
