package com.example.pomodoro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para registrar estadísticas agregadas por día.
 * Se usa para gráficos históricos y tendencias.
 */
@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    val date: String, // Formato YYYY-MM-DD
    
    val focusCount: Int = 0,         // Número de sesiones de foco completadas
    val focusMinutes: Int = 0,       // Total minutos de foco
    val tasksCompleted: Int = 0      // Tareas marcadas como completadas ese día
)
