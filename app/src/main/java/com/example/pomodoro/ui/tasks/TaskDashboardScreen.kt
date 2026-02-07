package com.example.pomodoro.ui.tasks

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.data.model.TaskStatus
import com.example.pomodoro.ui.components.AddTaskBottomSheet
import com.example.pomodoro.ui.components.TaskCard
import com.example.pomodoro.ui.components.TaskData

/**
 * Pantalla principal tipo tablero de tareas
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskDashboardScreen(
    viewModel: TaskViewModel,
    onTaskClick: (TaskEntity) -> Unit,
    onStartTask: (TaskEntity) -> Unit,
    onBack: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    val state by viewModel.dashboardState.collectAsState()
    val scope = rememberCoroutineScope()
        
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "📋 Mis Tareas",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón Calendario
                    IconButton(onClick = onOpenCalendar) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendario")
                    }
                    // View Toggle
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            if (state.viewMode == ViewMode.LIST) Icons.Default.ViewColumn else Icons.Default.ViewList,
                            contentDescription = "Cambiar vista",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(onClick = { viewModel.showAddSheet() }) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Nueva Tarea",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddSheet() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva Tarea") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- Barra de búsqueda ---
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar tareas...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
            

            
            // --- Tab View (Lista) ---
            if (state.viewMode == ViewMode.LIST) {
                // --- Tabs ---
                TabRow(
                    selectedTabIndex = state.selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DashboardTab.entries.forEach { tab ->
                        val count = when (tab) {
                            DashboardTab.TODAY -> state.todayCount
                            DashboardTab.UPCOMING -> state.upcomingCount
                            DashboardTab.COMPLETED -> state.completedCount
                        }
                        
                        Tab(
                            selected = state.selectedTab == tab,
                            onClick = { viewModel.onTabSelected(tab) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        when (tab) {
                                            DashboardTab.TODAY -> "Hoy"
                                            DashboardTab.UPCOMING -> "Próximas"
                                            DashboardTab.COMPLETED -> "Hechas"
                                        }
                                    )
                                    if (count > 0) {
                                        Badge(
                                            containerColor = if (state.selectedTab == tab)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(count.toString())
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // --- Content ---
            Box(modifier = Modifier.weight(1f)) {
                 if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.viewMode == ViewMode.BOARD) {
                    TaskBoardView(
                        todo = viewModel.dashboardState.value.filterTasks(state.todoTasks),
                        doing = viewModel.dashboardState.value.filterTasks(state.doingTasks),
                        done = viewModel.dashboardState.value.filterTasks(state.completedTasks),
                        onTaskClick = onTaskClick,
                        onStartTask = { viewModel.startTask(it) { onStartTask(it) } },
                        onEditClick = onTaskClick,
                        onDuplicateClick = { viewModel.duplicateTask(it) },
                        onDeleteClick = { viewModel.deleteTask(it) },
                        onMoveStatus = { task, status -> viewModel.updateTaskStatus(task, status) }
                    )
                } else {
                    // List View content
                    AnimatedContent(
                        targetState = state.selectedTab,
                        transitionSpec = {
                            fadeIn() + slideInHorizontally() togetherWith fadeOut() + slideOutHorizontally()
                        },
                        label = "tabContent"
                    ) { tab ->
                        if (state.displayedTasks.isEmpty()) {
                            EmptyState(tab = tab, searchQuery = state.searchQuery)
                        } else {
                            TaskList(
                                tasks = state.displayedTasks,
                                onTaskClick = onTaskClick,
                                onStartClick = { task ->
                                    viewModel.startTask(task) { onStartTask(it) }
                                },
                                onEditClick = onTaskClick,
                                onDuplicateClick = { viewModel.duplicateTask(it) },
                                onDeleteClick = { viewModel.deleteTask(it) },
                                onMoveStatus = { task, status -> viewModel.updateTaskStatus(task, status) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // --- Bottom Sheet para agregar tarea ---
    if (state.showAddSheet) {
        AddTaskBottomSheet(
            onDismiss = { viewModel.hideAddSheet() },
            onSave = { data ->
                viewModel.createTask(data)
            },
            onSaveAndStart = { data ->
                viewModel.createTask(data) { task ->
                    onStartTask(task)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskBoardView(
    todo: List<TaskEntity>,
    doing: List<TaskEntity>,
    done: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    onStartTask: (TaskEntity) -> Unit,
    onEditClick: (TaskEntity) -> Unit,
    onDuplicateClick: (TaskEntity) -> Unit,
    onDeleteClick: (TaskEntity) -> Unit,
    onMoveStatus: (TaskEntity, TaskStatus) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BoardColumn(
            title = "POR HACER",
            tasks = todo,
            color = MaterialTheme.colorScheme.secondaryContainer,
            taskColor = MaterialTheme.colorScheme.surface,
            onTaskClick = onTaskClick,
            onStartClick = onStartTask,
            onEditClick = onEditClick,
            onDuplicateClick = onDuplicateClick,
            onDeleteClick = onDeleteClick,
            onMoveStatus = onMoveStatus
        )
        
        BoardColumn(
            title = "EN PROCESO",
            tasks = doing,
            color = MaterialTheme.colorScheme.primaryContainer,
            taskColor = MaterialTheme.colorScheme.surface,
            onTaskClick = onTaskClick,
            onStartClick = onStartTask,
            onEditClick = onEditClick,
            onDuplicateClick = onDuplicateClick,
            onDeleteClick = onDeleteClick,
            onMoveStatus = onMoveStatus
        )
        
        BoardColumn(
            title = "HECHO",
            tasks = done,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            taskColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            onTaskClick = onTaskClick,
            onStartClick = onStartTask,
            onEditClick = onEditClick,
            onDuplicateClick = onDuplicateClick,
            onDeleteClick = onDeleteClick,
            onMoveStatus = onMoveStatus
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BoardColumn(
    title: String,
    tasks: List<TaskEntity>,
    color: Color,
    taskColor: Color,
    onTaskClick: (TaskEntity) -> Unit,
    onStartClick: (TaskEntity) -> Unit,
    onEditClick: (TaskEntity) -> Unit,
    onDuplicateClick: (TaskEntity) -> Unit,
    onDeleteClick: (TaskEntity) -> Unit,
    onMoveStatus: (TaskEntity, TaskStatus) -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Column Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Badge(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Text(tasks.size.toString())
            }
        }
        
        // Task List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(tasks, key = { it.id }) { task ->
                // Simplified Card for Board
                TaskCard(
                    task = task,
                    onStartClick = { onStartClick(task) },
                    onClick = { onTaskClick(task) },
                    onEditClick = { onEditClick(task) },
                    onDuplicateClick = { onDuplicateClick(task) },
                    onDeleteClick = { onDeleteClick(task) },
                    onMoveStatus = { status -> onMoveStatus(task, status) },
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskList(
    tasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    onStartClick: (TaskEntity) -> Unit,
    onEditClick: (TaskEntity) -> Unit,
    onDuplicateClick: (TaskEntity) -> Unit,
    onDeleteClick: (TaskEntity) -> Unit,
    onMoveStatus: (TaskEntity, TaskStatus) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onClick = { onTaskClick(task) },
                onStartClick = { onStartClick(task) },
                onEditClick = { onEditClick(task) },
                onDuplicateClick = { onDuplicateClick(task) },
                onDeleteClick = { onDeleteClick(task) },
                onMoveStatus = { status -> onMoveStatus(task, status) },
                modifier = Modifier.animateItemPlacement()
            )
        }
        
        // Espacio para el FAB
        item { Spacer(Modifier.height(80.dp)) }
    }
}
@Composable
private fun EmptyState(
    tab: DashboardTab, // Deprecated usage in board, kept for compatibility
    searchQuery: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            val (emoji, title, subtitle) = Triple(
                "📋",
                "Sin tareas", 
                "Crea tu primera tarea para verla aquí"
            )
            
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayLarge
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
