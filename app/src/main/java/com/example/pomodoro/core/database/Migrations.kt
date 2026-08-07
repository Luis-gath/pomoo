package com.example.pomodoro.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Primera migración real de la base de datos.
 *
 * Hasta la versión 7 el proyecto usaba `fallbackToDestructiveMigration`, que borraba los
 * datos del usuario en cada cambio de esquema. A partir de aquí cada versión lleva su
 * migración: esta añade las áreas y sus materiales **sin tocar** las tareas ni las
 * estadísticas existentes.
 *
 * El SQL debe coincidir exactamente con lo que Room genera para las entidades, incluidos
 * los índices; si no, Room lanza "Migration didn't properly handle" al abrir la base.
 * La referencia es el esquema exportado en `app/schemas/`.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `areas` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `areaId` INTEGER NOT NULL,
                `kind` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `uri` TEXT,
                `localPath` TEXT,
                `content` TEXT NOT NULL,
                `mark` TEXT NOT NULL,
                `dueAt` INTEGER,
                `taskId` INTEGER,
                `sizeBytes` INTEGER,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_areaId` ON `items` (`areaId`)")

        // Las tareas pasan a poder colgar de un área. Se deja anulable para no romper las
        // que ya existen: su campo de texto `courseOrProject` sigue ahí y se migrará
        // emparejando por nombre cuando el usuario cree sus áreas.
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `areaId` INTEGER")
    }
}

/** Añade el vínculo seguro entre las sesiones que pertenecen al mismo horario. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `scheduleSeriesId` TEXT")
    }
}

/**
 * Marca las tareas que en realidad son clases de un horario.
 *
 * Anulable y sin valor por defecto: todo lo que ya existe queda como tarea normal, que es
 * exactamente lo que era antes de este cambio.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `classGroupId` TEXT")
    }
}

/**
 * Permite archivar una entrega en vez de borrarla.
 *
 * Se añade anulable y sin valor por defecto: las entregas que ya existan quedan como
 * pendientes, que es justo lo que eran antes de este cambio.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `items` ADD COLUMN `completedAt` INTEGER")
    }
}
