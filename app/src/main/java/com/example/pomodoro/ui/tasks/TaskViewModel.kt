package com.example.pomodoro.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.data.model.TaskPriority
import com.example.pomodoro.data.model.TaskStatus
import com.example.pomodoro.data.repository.TaskRepository
import com.example.pomodoro.ui.components.TaskData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

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
    val selectedTask: TaskEntity? = null,
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

// Legacy compatibility
data class TaskUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val filteredTasks: List<TaskEntity> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: TaskFilter = TaskFilter.TODAY,
    val isLoading: Boolean = false
)

enum class TaskFilter {
    TODAY, UPCOMING, COMPLETED
}

/**
 * ViewModel para el Dashboard de Tareas
 */
class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _dashboardState = MutableStateFlow(TaskDashboardUiState())
    val dashboardState: StateFlow<TaskDashboardUiState> = _dashboardState.asStateFlow()
    
    // Legacy compatibility
    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

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
                
                // Update legacy state
                updateLegacyState(allTasks)
            }
        }
    }
    
    private fun updateLegacyState(allTasks: List<TaskEntity>) {
        _uiState.update { state ->
            val filtered = applyLegacyFilters(allTasks, state.activeFilter, state.searchQuery)
            state.copy(tasks = allTasks, filteredTasks = filtered, isLoading = false)
        }
    }
    
    private fun applyLegacyFilters(
        tasks: List<TaskEntity>, 
        filter: TaskFilter, 
        query: String
    ): List<TaskEntity> {
        val now = System.currentTimeMillis()
        val startOfToday = getStartOfDay(now)
        val endOfToday = startOfToday + 24 * 60 * 60 * 1000

        var filtered = tasks.filter { task ->
            when (filter) {
                TaskFilter.TODAY -> {
                    val dueDate = task.dueDateTime ?: task.timestamp
                    dueDate in startOfToday..endOfToday && task.status != TaskStatus.DONE
                }
                TaskFilter.UPCOMING -> {
                    val dueDate = task.dueDateTime ?: task.timestamp
                    (dueDate > endOfToday || task.dueDateTime == null) && task.status != TaskStatus.DONE
                }
                TaskFilter.COMPLETED -> task.status == TaskStatus.DONE
            }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.courseOrProject.contains(query, ignoreCase = true) 
            }
        }

        return filtered
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
        _uiState.update { 
            it.copy(
                searchQuery = query,
                filteredTasks = applyLegacyFilters(it.tasks, it.activeFilter, query)
            ) 
        }
    }
    
    fun showAddSheet(preSelectedDate: Long? = null) {
        _dashboardState.update { it.copy(showAddSheet = true, preSelectedDate = preSelectedDate) }
    }
    
    fun hideAddSheet() {
        _dashboardState.update { it.copy(showAddSheet = false) }
    }
    
    fun selectTask(task: TaskEntity?) {
        _dashboardState.update { it.copy(selectedTask = task) }
    }
    
    // --- CRUD Operations ---
    
    fun createTask(data: TaskData, onComplete: ((TaskEntity) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                // Calculate Duration & End Time
                val computedDuration = com.example.pomodoro.util.TaskDurationCalculator.calculateTotalMinutes(
                    totalPomodoros = data.totalPomodoros,
                    focusMinutes = data.focusMinutes,
                    shortBreakMinutes = data.shortBreakMinutes,
                    longBreakMinutes = data.longBreakMinutes,
                    longBreakEvery = data.longBreakEvery,
                    includeFinalBreak = data.includeFinalBreak
                )
                
                val startMillis = data.dueDateTime ?: now
                val endMillis = com.example.pomodoro.util.TaskDurationCalculator.calculateEndTime(data.dueDateTime, computedDuration)

                val task = TaskEntity(
                    title = data.title,
                    courseOrProject = data.courseOrProject,
                    dueDateTime = data.dueDateTime,
                    dueDateTimeMillis = data.dueDateTime, // Legacy
                    startDateTimeMillis = data.dueDateTime, // New Standard
                    endDateTimeMillis = endMillis,
                    computedDurationMinutes = computedDuration,
                    includeFinalBreak = data.includeFinalBreak,
                    reminderMinutesBefore = null,
                    priority = data.priority,
                    totalPomodoros = data.totalPomodoros,
                    focusMinutes = data.focusMinutes,
                    shortBreakMinutes = data.shortBreakMinutes,
                    longBreakMinutes = data.longBreakMinutes,
                    longBreakEvery = data.longBreakEvery,
                    createdAt = now,
                    updatedAt = now,
                    timestamp = startMillis
                )
                
                val id = repository.insertOrUpdateTask(task)
                val createdTask = repository.getTaskById(id.toInt())
                
                hideAddSheet()
                
                createdTask?.let { onComplete?.invoke(it) }
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
            val newEnd = com.example.pomodoro.util.TaskDurationCalculator.calculateEndTime(newStart, task.computedDurationMinutes)
            
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
    
    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.markTaskAsCompleted(task, !task.isDone)
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
    
    // --- Legacy compatibility ---
    
    fun onFilterChange(filter: TaskFilter) {
        _uiState.update { 
            it.copy(
                activeFilter = filter,
                filteredTasks = applyLegacyFilters(it.tasks, filter, it.searchQuery)
            ) 
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

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
