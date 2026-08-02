package com.example.pomodoro.features.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estados de una tarea en el flujo Kanban
 */
enum class TaskStatus {
    TODO,   // Pendiente
    DOING,  // En progreso
    DONE    // Completada
}

/**
 * Prioridad de la tarea
 */
enum class TaskPriority {
    LOW, MEDIUM, HIGH
}

/**
 * Tipo de repetición
 */
enum class RepeatType {
    NONE, DAILY, WEEKLY, MONTHLY
}

/**
 * Entidad de Tarea con configuración Pomodoro integrada
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    // --- Información básica ---
    val title: String,
    val notes: String = "",                     // Notas/descripción opcional
    val courseOrProject: String = "",           // Curso o proyecto asociado (texto libre, heredado)

    /** Área a la que pertenece la tarea. Sustituirá a [courseOrProject] cuando esté migrado. */
    val areaId: Int? = null,
    
    // --- Fechas y tiempos ---
    val dueDateTime: Long? = null,              // Fecha límite original (legacy o general)
    val dueDateTimeMillis: Long? = null,        // Fecha y Hora EXACTA programada (scheduling)
    val reminderMinutesBefore: Int? = null,     // Cuántos minutos antes recordar (e.g. 15, 30)
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // --- Estado y prioridad ---
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    
    // --- Configuración de repetición ---
    val repeatType: RepeatType = RepeatType.NONE,
    
    // --- Notificaciones y audio ---
    val isNotificationEnabled: Boolean = false,
    val isVoiceEnabled: Boolean = false,
    val audioUri: String? = null,
    
    // --- Configuración Pomodoro por tarea ---
    val totalPomodoros: Int = 4,                // 1-12 pomodoros objetivo
    val completedPomodoros: Int = 0,            // Progreso actual
    val focusMinutes: Int = 25,                 // 1-90 minutos por focus
    val shortBreakMinutes: Int = 5,             // 1-30 minutos descanso corto
    val longBreakMinutes: Int = 15,             // 1-60 minutos descanso largo
    val longBreakEvery: Int = 4,                // 2-8 cada cuantos focus un descanso largo
    val taskAutoStartNext: Boolean = false,     // Iniciar siguiente automáticamente
    
    // --- Advanced Duration & Scheduling ---
    val startDateTimeMillis: Long? = null,      // Hora inicio exacta (reemplaza o complementa dueDateTimeMillis)
    val endDateTimeMillis: Long? = null,        // Hora fin calculada
    val includeFinalBreak: Boolean = false,     // Si true, se añade descanso tras último pomodoro
    val computedDurationMinutes: Int = 25,      // Duración TOTAL pre-calculada (persisted)
    
    // --- Legacy fields for migration ---
    val timestamp: Long = System.currentTimeMillis(),  // Para compatibilidad, usar dueDateTime
    val isCompleted: Boolean = false,                  // Legacy, usar status == DONE
    val completedAt: Long? = null                      // Momento en que se completó
) {
    /**
     * Progreso como porcentaje (0.0 a 1.0)
     */
    val pomodoroProgress: Float
        get() = if (totalPomodoros > 0) completedPomodoros.toFloat() / totalPomodoros else 0f
    
    /**
     * Verifica si la tarea está completada
     */
    val isDone: Boolean
        get() = status == TaskStatus.DONE || completedPomodoros >= totalPomodoros
    
    /**
     * Pomodoros restantes
     */
    val remainingPomodoros: Int
        get() = (totalPomodoros - completedPomodoros).coerceAtLeast(0)

    /**
     * Duración en minutos (alias para computedDurationMinutes)
     */
    val durationMinutes: Int
        get() = computedDurationMinutes
}
