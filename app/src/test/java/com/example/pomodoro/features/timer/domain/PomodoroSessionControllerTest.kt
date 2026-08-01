package com.example.pomodoro.features.timer.domain

import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskStatus
import com.example.pomodoro.features.tasks.domain.CompleteFocusForTaskUseCase
import com.example.pomodoro.fake.FakeClock
import com.example.pomodoro.fake.FakeSettingsRepository
import com.example.pomodoro.fake.FakeStatsRepository
import com.example.pomodoro.fake.FakeTaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica la máquina de estados completa de una sesión Pomodoro con tiempo virtual.
 * Antes esta lógica vivía dentro del Service y no podía testearse sin instrumentación.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroSessionControllerTest {

    private val defaultSettings = Settings(
        focusDurationMinutes = 25,
        shortBreakDurationMinutes = 5,
        longBreakDurationMinutes = 15,
        longBreakEveryNCycles = 4,
        autoStartNext = false
    )

    private val focusMillis = 25 * 60_000L

    @Test
    fun `al terminar un focus sin tarea registra la sesion y pasa a descanso corto`() = runTest {
        val stats = FakeStatsRepository()
        val controller = buildController(scope = backgroundScope, stats = stats)
        runCurrent()

        controller.start()
        runTimer(focusMillis)

        assertEquals(listOf(25), stats.recordedFocusMinutes)
        assertEquals(PomodoroMode.ShortBreak, controller.state.value.mode)
        assertFalse(controller.state.value.isRunning)
    }

    @Test
    fun `al terminar un focus con tarea incrementa los pomodoros completados`() = runTest {
        val task = task(id = 1, totalPomodoros = 3, focusMinutes = 25)
        val tasks = FakeTaskRepository(listOf(task))
        val controller = buildController(scope = backgroundScope, tasks = tasks)
        runCurrent()

        controller.setActiveTaskWithConfig(task)
        controller.start()
        runTimer(focusMillis)

        assertEquals(1, tasks.taskById(1)?.completedPomodoros)
        assertEquals(1, controller.state.value.activeTaskCompletedPomodoros)
        assertEquals(TaskStatus.TODO, tasks.taskById(1)?.status)
        // La tarea sigue activa porque quedan pomodoros
        assertEquals(1, controller.state.value.activeTaskId)
    }

    @Test
    fun `al completar el ultimo pomodoro marca la tarea DONE y la desactiva`() = runTest {
        val task = task(id = 1, totalPomodoros = 1, focusMinutes = 25)
        val tasks = FakeTaskRepository(listOf(task))
        val stats = FakeStatsRepository()
        val controller = buildController(scope = backgroundScope, tasks = tasks, stats = stats)

        val events = mutableListOf<SessionEvent>()
        backgroundScope.launch { controller.events.collect { events += it } }
        runCurrent()

        controller.setActiveTaskWithConfig(task)
        controller.start()
        runTimer(focusMillis)

        // El estado DONE es lo que consultan el dashboard y el calendario
        assertEquals(TaskStatus.DONE, tasks.taskById(1)?.status)
        assertEquals(1, stats.taskCompletions)
        assertTrue(events.any { it is SessionEvent.TaskCompleted })
        assertNull(controller.state.value.activeTaskId)
    }

    @Test
    fun `emite TimerFinished con el modo que acaba de terminar`() = runTest {
        val controller = buildController(scope = backgroundScope)

        val events = mutableListOf<SessionEvent>()
        backgroundScope.launch { controller.events.collect { events += it } }
        runCurrent()

        controller.start()
        runTimer(focusMillis)

        val finished = events.filterIsInstance<SessionEvent.TimerFinished>()
        assertEquals(listOf(PomodoroMode.Focus), finished.map { it.mode })
    }

    @Test
    fun `con autoStartNext el siguiente modo arranca solo`() = runTest {
        val controller = buildController(
            scope = backgroundScope,
            settings = defaultSettings.copy(autoStartNext = true)
        )
        runCurrent()

        controller.start()
        // Justo después de que termine el focus, el descanso ya debe estar corriendo
        runTimer(focusMillis)

        assertEquals(PomodoroMode.ShortBreak, controller.state.value.mode)
        assertTrue(controller.state.value.isRunning)
    }

    @Test
    fun `pausar conserva el tiempo restante y reanudar continua desde ahi`() = runTest {
        val controller = buildController(scope = backgroundScope)
        runCurrent()

        controller.start()
        advanceTimeBy(10 * 60_000L)
        runCurrent()
        controller.pause()

        val remainingAfterPause = controller.state.value.currentTimeMillis
        assertFalse(controller.state.value.isRunning)
        // Quedan ~15 minutos de los 25
        assertTrue("Restante inesperado: $remainingAfterPause", remainingAfterPause in 14 * 60_000L..15 * 60_000L)

        // El tiempo sigue pasando mientras está en pausa, pero no debe descontarse
        advanceTimeBy(5 * 60_000L)
        runCurrent()
        assertEquals(remainingAfterPause, controller.state.value.currentTimeMillis)

        controller.start()
        runCurrent()
        assertTrue(controller.state.value.isRunning)
    }

    @Test
    fun `reset devuelve el temporizador a la duracion completa del modo`() = runTest {
        val controller = buildController(scope = backgroundScope)
        runCurrent()

        controller.start()
        advanceTimeBy(10 * 60_000L)
        runCurrent()
        controller.reset()

        assertEquals(focusMillis, controller.state.value.currentTimeMillis)
        assertFalse(controller.state.value.isRunning)
    }

    @Test
    fun `la duracion de la tarea activa manda sobre los ajustes globales`() = runTest {
        val task = task(id = 1, totalPomodoros = 2, focusMinutes = 10)
        val tasks = FakeTaskRepository(listOf(task))
        val stats = FakeStatsRepository()
        val controller = buildController(scope = backgroundScope, tasks = tasks, stats = stats)
        runCurrent()

        controller.setActiveTaskWithConfig(task)

        assertEquals(10 * 60_000L, controller.state.value.currentTimeMillis)

        controller.start()
        runTimer(10 * 60_000L)

        // Se registran los 10 minutos de la tarea, no los 25 de los ajustes globales
        assertEquals(listOf(10), stats.recordedFocusMinutes)
    }

    @Test
    fun `clearActiveTask deja el estado sin tarea`() = runTest {
        val task = task(id = 1, totalPomodoros = 2, focusMinutes = 25)
        val controller = buildController(scope = backgroundScope, tasks = FakeTaskRepository(listOf(task)))
        runCurrent()

        controller.setActiveTaskWithConfig(task)
        controller.clearActiveTask()

        assertNull(controller.state.value.activeTaskId)
        assertNull(controller.state.value.activeTaskTitle)
        assertEquals(0, controller.state.value.activeTaskTotalPomodoros)
    }

    // --- Helpers ---

    /**
     * Avanza el tiempo virtual hasta que el temporizador se agota.
     * No se usa advanceUntilIdle porque el controller corre en backgroundScope y esa
     * función, por diseño, no espera al trabajo de segundo plano.
     */
    private fun TestScope.runTimer(durationMillis: Long) {
        advanceTimeBy(durationMillis + 1_000)
        runCurrent()
    }

    private fun TestScope.buildController(
        scope: kotlinx.coroutines.CoroutineScope,
        tasks: FakeTaskRepository = FakeTaskRepository(),
        stats: FakeStatsRepository = FakeStatsRepository(),
        settings: Settings = defaultSettings
    ) = PomodoroSessionController(
        engine = PomodoroEngine(),
        statsRepository = stats,
        taskRepository = tasks,
        completeFocusForTask = CompleteFocusForTaskUseCase(tasks),
        settingsRepository = FakeSettingsRepository(settings),
        clock = FakeClock(testScheduler),
        scope = scope
    )

    private fun task(
        id: Int,
        totalPomodoros: Int,
        focusMinutes: Int
    ) = TaskEntity(
        id = id,
        title = "Tarea $id",
        totalPomodoros = totalPomodoros,
        completedPomodoros = 0,
        focusMinutes = focusMinutes,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        longBreakEvery = 4,
        status = TaskStatus.TODO
    )
}
