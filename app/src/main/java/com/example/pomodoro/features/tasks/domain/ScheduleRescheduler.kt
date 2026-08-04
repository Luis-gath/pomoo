package com.example.pomodoro.features.tasks.domain

import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskStatus
import java.time.Instant
import java.time.ZoneId

enum class ScheduleEditScope {
    THIS_DAY,
    ALL_WEEKS
}

/**
 * Calcula los cambios de horario sin depender de la interfaz ni de Room.
 *
 * Una edición semanal sólo toca sesiones pendientes de la misma serie y del mismo día
 * original. De este modo un horario con lunes, miércoles y viernes puede modificar sus
 * lunes sin desplazar también el resto de la rutina.
 */
object ScheduleRescheduler {

    fun reschedule(
        selectedTask: TaskEntity,
        allTasks: List<TaskEntity>,
        newStartMillis: Long,
        scope: ScheduleEditScope,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<TaskEntity> {
        val selectedStart = selectedTask.calendarStartMillis ?: return emptyList()
        if (scope == ScheduleEditScope.THIS_DAY || selectedTask.scheduleSeriesId == null) {
            return listOf(selectedTask.withCalendarStart(newStartMillis))
        }

        val originalDateTime = selectedStart.toLocalDateTime(zone)
        val newDateTime = newStartMillis.toLocalDateTime(zone)
        val weekdayShift = newDateTime.dayOfWeek.value - originalDateTime.dayOfWeek.value

        return allTasks
            .asSequence()
            .filter { it.scheduleSeriesId == selectedTask.scheduleSeriesId }
            .filter { it.status != TaskStatus.DONE }
            .filter { task ->
                task.calendarStartMillis
                    ?.toLocalDateTime(zone)
                    ?.dayOfWeek == originalDateTime.dayOfWeek
            }
            .map { task ->
                val occurrence = task.calendarStartMillis!!.toLocalDateTime(zone)
                val moved = occurrence
                    .plusDays(weekdayShift.toLong())
                    .withHour(newDateTime.hour)
                    .withMinute(newDateTime.minute)
                    .withSecond(0)
                    .withNano(0)
                task.withCalendarStart(moved.atZone(zone).toInstant().toEpochMilli())
            }
            .sortedBy { it.calendarStartMillis }
            .toList()
    }

    private fun TaskEntity.withCalendarStart(startMillis: Long): TaskEntity = copy(
        dueDateTime = startMillis,
        dueDateTimeMillis = startMillis,
        startDateTimeMillis = startMillis,
        endDateTimeMillis = TaskDurationCalculator.calculateEndTime(
            startMillis,
            computedDurationMinutes
        ),
        timestamp = startMillis,
        updatedAt = System.currentTimeMillis()
    )

    private fun Long.toLocalDateTime(zone: ZoneId) =
        Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime()
}

/** Aplica todos los campos editables a la misma recurrencia sin borrar su progreso propio. */
object ScheduleSeriesEditor {

    fun applyEdit(
        selectedOriginal: TaskEntity,
        editedTask: TaskEntity,
        allTasks: List<TaskEntity>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<TaskEntity> {
        val newStart = editedTask.calendarStartMillis ?: return emptyList()
        val movedOccurrences = ScheduleRescheduler.reschedule(
            selectedTask = selectedOriginal,
            allTasks = allTasks,
            newStartMillis = newStart,
            scope = ScheduleEditScope.ALL_WEEKS,
            zone = zone
        )

        return movedOccurrences.map { occurrence ->
            val occurrenceStart = occurrence.calendarStartMillis!!
            editedTask.copy(
                id = occurrence.id,
                scheduleSeriesId = occurrence.scheduleSeriesId,
                dueDateTime = occurrence.dueDateTime,
                dueDateTimeMillis = occurrence.dueDateTimeMillis,
                startDateTimeMillis = occurrence.startDateTimeMillis,
                endDateTimeMillis = TaskDurationCalculator.calculateEndTime(
                    occurrenceStart,
                    editedTask.computedDurationMinutes
                ),
                timestamp = occurrence.timestamp,
                createdAt = occurrence.createdAt,
                status = occurrence.status,
                completedPomodoros = occurrence.completedPomodoros
                    .coerceAtMost(editedTask.totalPomodoros),
                completedAt = occurrence.completedAt,
                isCompleted = occurrence.isCompleted,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}

val TaskEntity.calendarStartMillis: Long?
    get() = startDateTimeMillis ?: dueDateTimeMillis ?: dueDateTime
