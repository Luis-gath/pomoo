package com.example.pomodoro.features.tasks.domain

import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleReschedulerTest {

    private val lima = ZoneId.of("America/Lima")

    @Test
    fun `solo este dia cambia una unica sesion`() {
        val monday = task(1, "serie-a", at(2026, 8, 3, 18, 0))
        val nextMonday = task(2, "serie-a", at(2026, 8, 10, 18, 0))
        val target = at(2026, 8, 3, 20, 30)

        val updates = ScheduleRescheduler.reschedule(
            selectedTask = monday,
            allTasks = listOf(monday, nextMonday),
            newStartMillis = target,
            scope = ScheduleEditScope.THIS_DAY,
            zone = lima
        )

        assertEquals(listOf(1), updates.map { it.id })
        assertEquals(target, updates.single().startDateTimeMillis)
        assertEquals(target + 50 * 60_000L, updates.single().endDateTimeMillis)
    }

    @Test
    fun `todas las semanas solo cambia el mismo dia pendiente de la serie`() {
        val monday = task(1, "serie-a", at(2026, 8, 3, 18, 0))
        val nextMonday = task(2, "serie-a", at(2026, 8, 10, 18, 0))
        val wednesday = task(3, "serie-a", at(2026, 8, 5, 18, 0))
        val otherSeries = task(4, "serie-b", at(2026, 8, 10, 18, 0))
        val completed = task(
            5,
            "serie-a",
            at(2026, 7, 27, 18, 0),
            status = TaskStatus.DONE
        )

        val updates = ScheduleRescheduler.reschedule(
            selectedTask = monday,
            allTasks = listOf(monday, nextMonday, wednesday, otherSeries, completed),
            newStartMillis = at(2026, 8, 3, 20, 0),
            scope = ScheduleEditScope.ALL_WEEKS,
            zone = lima
        )

        assertEquals(listOf(1, 2), updates.map { it.id })
        assertTrue(updates.all { local(it.startDateTimeMillis!!).hour == 20 })
    }

    @Test
    fun `cambiar el dia desplaza esa recurrencia en cada semana`() {
        val firstMonday = task(1, "serie-a", at(2026, 8, 3, 18, 0))
        val secondMonday = task(2, "serie-a", at(2026, 8, 10, 18, 0))

        val updates = ScheduleRescheduler.reschedule(
            selectedTask = firstMonday,
            allTasks = listOf(firstMonday, secondMonday),
            newStartMillis = at(2026, 8, 4, 19, 15),
            scope = ScheduleEditScope.ALL_WEEKS,
            zone = lima
        )

        assertEquals(
            listOf(
                LocalDateTime.of(2026, 8, 4, 19, 15),
                LocalDateTime.of(2026, 8, 11, 19, 15)
            ),
            updates.map { local(it.startDateTimeMillis!!) }
        )
    }

    @Test
    fun `una tarea antigua sin serie usa alcance individual`() {
        val legacy = task(1, null, at(2026, 8, 3, 18, 0))
        val similar = task(2, null, at(2026, 8, 10, 18, 0))

        val updates = ScheduleRescheduler.reschedule(
            selectedTask = legacy,
            allTasks = listOf(legacy, similar),
            newStartMillis = at(2026, 8, 3, 21, 0),
            scope = ScheduleEditScope.ALL_WEEKS,
            zone = lima
        )

        assertEquals(listOf(1), updates.map { it.id })
    }

    @Test
    fun `editar toda la serie comparte cambios pero conserva progreso y estado`() {
        val first = task(1, "serie-a", at(2026, 8, 3, 18, 0)).copy(
            completedPomodoros = 1,
            createdAt = 100L
        )
        val second = task(2, "serie-a", at(2026, 8, 10, 18, 0)).copy(
            completedPomodoros = 2,
            createdAt = 200L
        )
        val edited = first.copy(
            title = "Álgebra avanzada",
            notes = "Repasar matrices",
            totalPomodoros = 4,
            computedDurationMinutes = 100,
            dueDateTime = at(2026, 8, 3, 20, 0),
            dueDateTimeMillis = at(2026, 8, 3, 20, 0),
            startDateTimeMillis = at(2026, 8, 3, 20, 0)
        )

        val updates = ScheduleSeriesEditor.applyEdit(
            selectedOriginal = first,
            editedTask = edited,
            allTasks = listOf(first, second),
            zone = lima
        )

        assertEquals(listOf("Álgebra avanzada", "Álgebra avanzada"), updates.map { it.title })
        assertEquals(listOf(1, 2), updates.map { it.completedPomodoros })
        assertEquals(listOf(100L, 200L), updates.map { it.createdAt })
        assertEquals(listOf(20, 20), updates.map { local(it.startDateTimeMillis!!).hour })
        assertTrue(updates.all {
            it.endDateTimeMillis == it.startDateTimeMillis!! + 100 * 60_000L
        })
    }

    private fun task(
        id: Int,
        seriesId: String?,
        start: Long,
        status: TaskStatus = TaskStatus.TODO
    ) = TaskEntity(
        id = id,
        title = "Álgebra lineal",
        scheduleSeriesId = seriesId,
        startDateTimeMillis = start,
        dueDateTimeMillis = start,
        dueDateTime = start,
        computedDurationMinutes = 50,
        status = status
    )

    private fun at(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long = LocalDateTime.of(year, month, day, hour, minute)
        .atZone(lima)
        .toInstant()
        .toEpochMilli()

    private fun local(millis: Long): LocalDateTime =
        java.time.Instant.ofEpochMilli(millis).atZone(lima).toLocalDateTime()
}
