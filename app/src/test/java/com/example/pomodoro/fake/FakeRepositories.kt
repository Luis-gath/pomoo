package com.example.pomodoro.fake

import com.example.pomodoro.core.time.Clock
import com.example.pomodoro.features.stats.data.DailyStatsEntity
import com.example.pomodoro.features.stats.data.StatsRepository
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskRepository
import com.example.pomodoro.features.tasks.data.TaskStatus
import com.example.pomodoro.features.timer.data.SettingsRepository
import com.example.pomodoro.features.timer.domain.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.time.LocalDate

/**
 * Reloj de prueba anclado al planificador de corrutinas: al avanzar el tiempo virtual
 * con advanceTimeBy, este reloj avanza igual.
 */
class FakeClock(
    private val scheduler: TestCoroutineScheduler,
    private val startMillis: Long = 1_700_000_000_000L
) : Clock {
    override fun now(): Long = startMillis + scheduler.currentTime
}

class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settingsFlow: Flow<Settings> = state
    override suspend fun updateSettings(settings: Settings) {
        state.value = settings
    }
}

class FakeStatsRepository : StatsRepository {

    val recordedFocusMinutes = mutableListOf<Int>()
    var taskCompletions = 0
        private set

    private val todayCount = MutableStateFlow(0)
    private val totalCount = MutableStateFlow(0)

    override fun getStatsForRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailyStatsEntity>> = flowOf(emptyList())

    override fun getTotalFocusCount(): Flow<Int> = totalCount

    override fun getTodayFocusCount(): Flow<Int> = todayCount

    override fun getCurrentStreak(): Flow<Int> = flowOf(0)

    override suspend fun recordFocusSession(minutes: Int) {
        recordedFocusMinutes += minutes
        todayCount.value += 1
        totalCount.value += 1
    }

    override suspend fun recordTaskCompletion() {
        taskCompletions += 1
    }

    override fun getTopTasksByPomodoros(): Flow<List<TaskEntity>> = flowOf(emptyList())
}

/** Repositorio de tareas en memoria. */
class FakeTaskRepository(initialTasks: List<TaskEntity> = emptyList()) : TaskRepository {

    private val tasks = MutableStateFlow(initialTasks.associateBy { it.id })
    private var nextId = (initialTasks.maxOfOrNull { it.id } ?: 0) + 1

    fun taskById(id: Int): TaskEntity? = tasks.value[id]

    override fun getAllTasks(): Flow<List<TaskEntity>> = tasks.map { it.values.toList() }

    override suspend fun getTaskById(id: Int): TaskEntity? = tasks.value[id]

    override fun getTaskByIdFlow(id: Int): Flow<TaskEntity?> = tasks.map { it[id] }

    override suspend fun insertOrUpdateTask(task: TaskEntity): Long {
        val id = if (task.id == 0) nextId++ else task.id
        tasks.value = tasks.value + (id to task.copy(id = id))
        return id.toLong()
    }

    override suspend fun deleteTask(task: TaskEntity) {
        tasks.value = tasks.value - task.id
    }

    override suspend fun duplicateTask(task: TaskEntity): Long =
        insertOrUpdateTask(task.copy(id = 0, title = "${task.title} (copia)"))

    override suspend fun updateTaskStatus(taskId: Int, status: TaskStatus) {
        update(taskId) { it.copy(status = status) }
    }

    override suspend fun markTaskDone(taskId: Int) {
        update(taskId) {
            it.copy(
                status = TaskStatus.DONE,
                isCompleted = true,
                completedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun updateCompletedPomodoros(taskId: Int, completed: Int) {
        update(taskId) { it.copy(completedPomodoros = completed) }
    }

    override suspend fun incrementPomodoro(taskId: Int): Boolean {
        val task = tasks.value[taskId] ?: return false
        val newCompleted = (task.completedPomodoros + 1).coerceAtMost(task.totalPomodoros)
        updateCompletedPomodoros(taskId, newCompleted)
        if (newCompleted >= task.totalPomodoros) {
            markTaskDone(taskId)
            return true
        }
        return false
    }

    override suspend fun resetTaskProgress(taskId: Int) {
        update(taskId) { it.copy(completedPomodoros = 0, status = TaskStatus.TODO) }
    }

    private fun update(taskId: Int, transform: (TaskEntity) -> TaskEntity) {
        val current = tasks.value[taskId] ?: return
        tasks.value = tasks.value + (taskId to transform(current))
    }
}
