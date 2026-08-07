package com.example.pomodoro.features.tasks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskStatus
import com.example.pomodoro.features.tasks.data.TaskRepository
import com.example.pomodoro.features.tasks.data.RepeatType
import com.example.pomodoro.features.tasks.domain.ApplyClassScheduleUseCase
import com.example.pomodoro.features.tasks.domain.ClassSchedulePlan
import com.example.pomodoro.features.tasks.domain.ApplyRecurrenceUseCase
import com.example.pomodoro.features.tasks.domain.ApplyStudyHabitUseCase
import com.example.pomodoro.features.tasks.domain.ApplyWeeklyScheduleUseCase
import com.example.pomodoro.features.tasks.domain.CreateTaskUseCase
import com.example.pomodoro.features.tasks.domain.RecurrenceGenerator
import com.example.pomodoro.features.tasks.domain.ScheduleEditScope
import com.example.pomodoro.features.tasks.domain.ScheduleRescheduler
import com.example.pomodoro.features.tasks.domain.ScheduleSeriesEditor
import com.example.pomodoro.features.tasks.domain.StudyHabitPlan
import com.example.pomodoro.features.tasks.domain.WeeklyScheduleTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Estado del Dashboard de Tareas
 */
data class TaskDashboardUiState(
    val todayTasks: List<TaskEntity> = emptyList(),
    val upcomingTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    
    // Board Mode Lists
    val todoTasks: List<TaskEntity> = emptyList(),
    val doingTasks: List<TaskEntity> = emptyList(),
    
    val allTasks: List<TaskEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: DashboardTab = DashboardTab.TODAY,
    val viewMode: ViewMode = ViewMode.LIST,
    val isLoading: Boolean = false,
    val showAddSheet: Boolean = false,
    val preSelectedDate: Long? = null, // Fecha pre-seleccionada para nueva tarea
    val error: String? = null
) {
    val displayedTasks: List<TaskEntity>
        get() = filterTasks(
            when (selectedTab) {
                DashboardTab.TODAY -> todayTasks
                DashboardTab.UPCOMING -> upcomingTasks
                DashboardTab.COMPLETED -> completedTasks
            }
        )
        
    fun filterTasks(tasks: List<TaskEntity>): List<TaskEntity> {
        return if (searchQuery.isBlank()) {
            tasks
        } else {
            tasks.filter { task ->
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.courseOrProject.contains(searchQuery, ignoreCase = true) ||
                task.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val todayCount: Int get() = todayTasks.size
    val upcomingCount: Int get() = upcomingTasks.size
    val completedCount: Int get() = completedTasks.size
}

enum class ViewMode {
    LIST, BOARD
}

enum class DashboardTab {
    TODAY, UPCOMING, COMPLETED
}

/**
 * ViewModel para el Dashboard de Tareas
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val createTaskUseCase: CreateTaskUseCase,
    private val applyWeeklySchedule: ApplyWeeklyScheduleUseCase,
    private val applyStudyHabit: ApplyStudyHabitUseCase,
    private val applyRecurrence: ApplyRecurrenceUseCase,
    private val applyClasses: ApplyClassScheduleUseCase
) : ViewModel() {

    /** Resultado de aplicar una rutina semanal, para avisar en pantalla. */
    private val _scheduleMessage = MutableStateFlow<String?>(null)
    val scheduleMessage: StateFlow<String?> = _scheduleMessage.asStateFlow()

    private val _applyingSchedule = MutableStateFlow(false)
    val applyingSchedule: StateFlow<Boolean> = _applyingSchedule.asStateFlow()

    private val _scheduleEditMessage = MutableStateFlow<String?>(null)
    val scheduleEditMessage: StateFlow<String?> = _scheduleEditMessage.asStateFlow()

    fun consumeScheduleMessage() { _scheduleMessage.value = null }

    fun consumeScheduleEditMessage() { _scheduleEditMessage.value = null }

    fun applySchedule(
        template: WeeklyScheduleTemplate,
        weeks: Int,
        courseOrProject: String,
        withReminders: Boolean
    ) {
        if (_applyingSchedule.value) return
        _applyingSchedule.value = true

        viewModelScope.launch {
            try {
                val result = applyWeeklySchedule(
                    template = template,
                    weeks = weeks,
                    courseOrProject = courseOrProject,
                    withReminders = withReminders
                )
                _scheduleMessage.value =
                    "${result.created} sesiones creadas para ${result.weeks} semana(s)"
            } catch (e: Exception) {
                _scheduleMessage.value = "No se pudo aplicar el horario: ${e.message}"
            } finally {
                _applyingSchedule.value = false
            }
        }
    }

    /** Da de alta el horario real de un curso creando todas sus clases. */
    fun applyClassSchedule(plan: ClassSchedulePlan) {
        if (_applyingSchedule.value) return
        _applyingSchedule.value = true

        viewModelScope.launch {
            try {
                val result = applyClasses(plan)
                _scheduleMessage.value =
                    "${result.created} clases de ${result.courseName} añadidas al calendario"
            } catch (e: Exception) {
                _scheduleMessage.value = "No se pudo crear el horario: ${e.message}"
            } finally {
                _applyingSchedule.value = false
            }
        }
    }

    fun applyHabit(plan: StudyHabitPlan) {
        if (_applyingSchedule.value) return
        _applyingSchedule.value = true

        viewModelScope.launch {
            try {
                val result = applyStudyHabit(plan)
                val weeklyTime = formatMinutes(result.focusMinutesPerWeek)
                _scheduleMessage.value =
                    "${result.created} sesiones creadas · $weeklyTime de enfoque por semana"
            } catch (e: Exception) {
                _scheduleMessage.value = "No se pudo crear el hábito: ${e.message}"
            } finally {
                _applyingSchedule.value = false
            }
        }
    }

    private val _dashboardState = MutableStateFlow(TaskDashboardUiState())
    val dashboardState: StateFlow<TaskDashboardUiState> = _dashboardState.asStateFlow()

    /** Tarea que está editando la pantalla de editor; la UI ya no consulta el repositorio. */
    private val _editingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask: StateFlow<TaskEntity?> = _editingTask.asStateFlow()

    init {
        loadAllTasks()
    }

    private fun loadAllTasks() {
        _dashboardState.update { it.copy(isLoading = true) }
        
        // Obtener todas las tareas y clasificarlas en memoria para reactividad instantánea
        viewModelScope.launch {
            repository.getAllTasks().collect { allTasks ->
                val today = ArrayList<TaskEntity>()
                val upcoming = ArrayList<TaskEntity>()
                val completed = ArrayList<TaskEntity>()
                
                val todo = ArrayList<TaskEntity>()
                val doing = ArrayList<TaskEntity>()
                
                val now = System.currentTimeMillis()
                val startOfToday = getStartOfDay(now)
                val endOfToday = startOfToday + 24 * 60 * 60 * 1000
                
                allTasks.forEach { task ->
                    // Las clases del horario salen en el calendario, pero no en las listas
                    // de pendientes: son sitios donde estar, no cosas por hacer, y un
                    // ciclo entero las convertiría en decenas de filas de ruido.
                    if (task.isClassSession) return@forEach

                    // Clasificación para VISTA DE LISTA (Timestamps)
                    if (task.status == TaskStatus.DONE) {
                        completed.add(task)
                    } else {
                        val dueDate = task.dueDateTime ?: task.timestamp
                        if (dueDate < endOfToday) {
                            today.add(task)
                        } else {
                            upcoming.add(task)
                        }
                    }
                    
                    // Clasificación para VISTA DE TABLERO (Status)
                    when (task.status) {
                        TaskStatus.TODO -> todo.add(task)
                        TaskStatus.DOING -> doing.add(task)
                        TaskStatus.DONE -> {} // Ya está en 'completed'
                    }
                }
                
                _dashboardState.update { state ->
                    state.copy(
                        todayTasks = today,
                        upcomingTasks = upcoming,
                        completedTasks = completed,
                        todoTasks = todo,
                        doingTasks = doing,
                        allTasks = allTasks,
                        isLoading = false
                    )
                }
            }
        }
    }

    // --- Dashboard Actions ---
    
    fun onTabSelected(tab: DashboardTab) {
        _dashboardState.update { it.copy(selectedTab = tab) }
    }
    
    fun toggleViewMode() {
        _dashboardState.update { state ->
            val newMode = if (state.viewMode == ViewMode.LIST) ViewMode.BOARD else ViewMode.LIST
            state.copy(viewMode = newMode)
        }
    }
    
    fun onSearchQueryChange(query: String) {
        _dashboardState.update { it.copy(searchQuery = query) }
    }
    
    fun showAddSheet(preSelectedDate: Long? = null) {
        _dashboardState.update { it.copy(showAddSheet = true, preSelectedDate = preSelectedDate) }
    }
    
    fun hideAddSheet() {
        _dashboardState.update { it.copy(showAddSheet = false) }
    }
    
    // --- CRUD Operations ---
    
    fun createTask(data: TaskData, onComplete: ((TaskEntity) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val createdTask = createTaskUseCase(
                    title = data.title,
                    courseOrProject = data.courseOrProject,
                    dueDateTime = data.dueDateTime,
                    priority = data.priority,
                    totalPomodoros = data.totalPomodoros,
                    focusMinutes = data.focusMinutes,
                    shortBreakMinutes = data.shortBreakMinutes,
                    longBreakMinutes = data.longBreakMinutes,
                    longBreakEvery = data.longBreakEvery,
                    includeFinalBreak = data.includeFinalBreak,
                    repeatType = data.repeatType
                )

                // La primera ya está creada y validada; el caso de uso le pone el
                // identificador de serie y añade las siguientes.
                if (createdTask != null && data.repeatType != RepeatType.NONE) {
                    val applied = applyRecurrence(
                        base = createdTask,
                        repeatCount = RecurrenceGenerator.defaultCount(data.repeatType)
                    )
                    _scheduleMessage.value = "Se crearon ${applied.created} repeticiones"
                }

                hideAddSheet()

                createdTask?.let { onComplete?.invoke(it) }
            } catch (e: Exception) {
                _dashboardState.update { it.copy(error = e.message) }
            }
        }
    }

    // --- Edición de tarea (pantalla de editor) ---

    /** Carga la tarea a editar. Con [taskId] nulo se trata de una tarea nueva. */
    fun loadTaskForEdit(taskId: Int?) {
        if (taskId == null) {
            _editingTask.value = null
            return
        }
        viewModelScope.launch {
            _editingTask.value = repository.getTaskById(taskId)
        }
    }

    /**
     * Persiste la tarea del editor y devuelve la versión guardada (con id asignado
     * cuando es nueva), para que la pantalla pueda encadenar acciones como "iniciar".
     */
    fun saveTask(
        task: TaskEntity,
        scope: ScheduleEditScope = ScheduleEditScope.THIS_DAY,
        repeatCount: Int = RecurrenceGenerator.defaultCount(task.repeatType),
        onSaved: ((TaskEntity) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                // Una tarea nueva que se repite se expande en toda su serie. Al editar
                // una existente no se regenera nada: eso duplicaría las ocurrencias que
                // ya están en el calendario.
                val isNew = task.id == 0
                if (isNew && task.repeatType != RepeatType.NONE) {
                    val applied = applyRecurrence(task, repeatCount)
                    _scheduleMessage.value = "Se crearon ${applied.created} repeticiones"
                    applied.first?.let { onSaved?.invoke(it) }
                    return@launch
                }

                val original = if (task.id != 0) repository.getTaskById(task.id) else null
                val seriesUpdates = if (
                    scope == ScheduleEditScope.ALL_WEEKS &&
                    original?.scheduleSeriesId != null
                ) {
                    ScheduleSeriesEditor.applyEdit(
                        selectedOriginal = original,
                        editedTask = task,
                        allTasks = _dashboardState.value.allTasks
                    )
                } else {
                    emptyList()
                }

                val savedId = if (seriesUpdates.isNotEmpty()) {
                    seriesUpdates.forEach { repository.insertOrUpdateTask(it) }
                    task.id.toLong()
                } else {
                    repository.insertOrUpdateTask(task)
                }
                val savedTask = repository.getTaskById(savedId.toInt())
                    ?: task.copy(id = savedId.toInt())
                onSaved?.invoke(savedTask)
            } catch (e: Exception) {
                _dashboardState.update { it.copy(error = e.message) }
            }
        }
    }
    
    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.insertOrUpdateTask(task.copy(updatedAt = System.currentTimeMillis()))
        }
    }
    
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }
    
    fun duplicateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.duplicateTask(task)
        }
    }
    
    fun postponeTask(task: TaskEntity, minutes: Int) {
        viewModelScope.launch {
            val validStart = task.startDateTimeMillis ?: return@launch
            val newStart = validStart + (minutes * 60 * 1000L)
            
            // Recalculate end time just in case duration logic changes, though duration shouldn't change here.
            // Using existing duration.
            val newEnd = com.example.pomodoro.features.tasks.domain.TaskDurationCalculator.calculateEndTime(newStart, task.computedDurationMinutes)
            
            val updated = task.copy(
                dueDateTime = newStart, // Update legacy/sync
                dueDateTimeMillis = newStart,
                startDateTimeMillis = newStart,
                endDateTimeMillis = newEnd,
                timestamp = newStart,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateTask(updated)
        }
    }
    
    fun updateTaskStatus(task: TaskEntity, newStatus: TaskStatus) {
        viewModelScope.launch {
            repository.updateTaskStatus(task.id, newStatus)
        }
    }
    
    fun startTask(task: TaskEntity, onReady: (TaskEntity) -> Unit) {
        viewModelScope.launch {
            // Cambiar estado a DOING
            if (task.status != TaskStatus.DOING) {
                repository.updateTaskStatus(task.id, TaskStatus.DOING)
            }
            
            val updatedTask = repository.getTaskById(task.id) ?: task
            onReady(updatedTask)
        }
    }
    
    fun incrementPomodoro(taskId: Int, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val isComplete = repository.incrementPomodoro(taskId)
            onComplete?.invoke(isComplete)
        }
    }
    
    fun resetTaskProgress(taskId: Int) {
        viewModelScope.launch {
            repository.resetTaskProgress(taskId)
        }
    }
    
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun rescheduleTask(
        task: TaskEntity,
        newStartMillis: Long,
        scope: ScheduleEditScope
    ) {
        viewModelScope.launch {
            try {
                val updates = ScheduleRescheduler.reschedule(
                    selectedTask = task,
                    allTasks = _dashboardState.value.allTasks,
                    newStartMillis = newStartMillis,
                    scope = scope
                )
                updates.forEach { repository.insertOrUpdateTask(it) }
                _scheduleEditMessage.value = when {
                    updates.isEmpty() -> "No se encontró una fecha programada para esta tarea"
                    scope == ScheduleEditScope.ALL_WEEKS && updates.size > 1 ->
                        "Horario actualizado en ${updates.size} semanas"
                    else -> "Horario actualizado para este día"
                }
            } catch (e: Exception) {
                _scheduleEditMessage.value = "No se pudo actualizar el horario: ${e.message}"
            }
        }
    }

    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours == 0 -> "$rest min"
            rest == 0 -> "$hours h"
            else -> "$hours h $rest min"
        }
    }
}
