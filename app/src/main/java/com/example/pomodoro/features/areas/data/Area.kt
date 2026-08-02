package com.example.pomodoro.features.areas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un curso, una habilidad y un proyecto son la misma estructura: un contenedor con
 * materiales, tareas y fechas. Por eso la tabla se llama `areas` y no `cursos`: cuando el
 * día de mañana se quiera un tablero tipo Trello, será una vista nueva sobre estos mismos
 * datos y no una migración.
 */
enum class AreaType {
    CURSO,
    HABILIDAD,
    PROYECTO
}

@Entity(tableName = "areas")
data class Area(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val type: AreaType = AreaType.CURSO,

    /** Color de acento en formato #RRGGBB, para distinguirlas de un vistazo. */
    val colorHex: String = "#2E7D57",

    /** Las áreas terminadas se archivan en vez de borrarse: conservan su material. */
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
)
