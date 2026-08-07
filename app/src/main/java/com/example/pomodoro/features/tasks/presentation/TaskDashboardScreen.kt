package com.example.pomodoro.features.tasks.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskStatus
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
    var showScheduleDialog by remember { mutableStateOf(false) }
    val applyingSchedule by viewModel.applyingSchedule.collectAsState()
    val scheduleMessage by viewModel.scheduleMessage.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(scheduleMessage) {
        scheduleMessage?.let {
            showScheduleDialog = false
            snackbar.showSnackbar(it)
            viewModel.consumeScheduleMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis tareas", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${state.todoTasks.size + state.doingTasks.size} activas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Horarios, hábitos y planes compartibles.
                    IconButton(onClick = { showScheduleDialog = true }) {
                        Icon(Icons.Default.EventRepeat, contentDescription = "Planificar estudio")
                    }
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
                    
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddSheet() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva tarea") },
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
                placeholder = { Text("Buscar tareas") },
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

            TaskDashboardSummary(
                today = state.todayCount,
                inProgress = state.doingTasks.size,
                completed = state.completedCount,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
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
                                            DashboardTab.COMPLETED -> "Completadas"
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
                        // Se usa el estado ya recolectado: leer dashboardState.value aquí
                        // no suscribe a cambios y el tablero no se refrescaba.
                        todo = state.filterTasks(state.todoTasks),
                        doing = state.filterTasks(state.doingTasks),
                        done = state.filterTasks(state.completedTasks),
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
    if (showScheduleDialog) {
        WeeklyScheduleDialog(
            isApplying = applyingSchedule,
            onDismiss = { showScheduleDialog = false },
            onApplySchedule = { template, weeks, course, reminders ->
                viewModel.applySchedule(template, weeks, course, reminders)
            },
            onApplyHabit = viewModel::applyHabit,
            onApplyClasses = viewModel::applyClassSchedule
        )
    }

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
            onTaskClick = onTaskClick,
            onStartClick = onStartTask,
            onEditClick = onEditClick,
            onDuplicateClick = onDuplicateClick,
            onDeleteClick = onDeleteClick,
            onMoveStatus = onMoveStatus
        )
        
        BoardColumn(
            title = "EN CURSO",
            tasks = doing,
            color = MaterialTheme.colorScheme.primaryContainer,
            onTaskClick = onTaskClick,
            onStartClick = onStartTask,
            onEditClick = onEditClick,
            onDuplicateClick = onDuplicateClick,
            onDeleteClick = onDeleteClick,
            onMoveStatus = onMoveStatus
        )
        
        BoardColumn(
            title = "COMPLETADAS",
            tasks = done,
            color = MaterialTheme.colorScheme.tertiaryContainer,
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
            .background(color.copy(alpha = 0.24f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
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
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
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
        
    }
}

@Composable
private fun TaskDashboardSummary(
    today: Int,
    inProgress: Int,
    completed: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskMetric(
            value = today,
            label = "Para hoy",
            icon = Icons.Default.Today,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        TaskMetric(
            value = inProgress,
            label = "En curso",
            icon = Icons.Default.PlayCircle,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        TaskMetric(
            value = completed,
            label = "Completadas",
            icon = Icons.Default.TaskAlt,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TaskMetric(
    value: Int,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
                AnimatedContent(targetState = value, label = "metric_value") { count ->
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EmptyState(
    tab: DashboardTab,
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
            val hasSearch = searchQuery.isNotBlank()
            val icon = if (hasSearch) Icons.Default.SearchOff else when (tab) {
                DashboardTab.TODAY -> Icons.Default.Today
                DashboardTab.UPCOMING -> Icons.Default.Event
                DashboardTab.COMPLETED -> Icons.Default.TaskAlt
            }
            val title = if (hasSearch) "Sin resultados" else when (tab) {
                DashboardTab.TODAY -> "Tu día está despejado"
                DashboardTab.UPCOMING -> "Nada programado"
                DashboardTab.COMPLETED -> "Aún no hay tareas completadas"
            }
            val subtitle = if (hasSearch) {
                "No encontramos tareas para “$searchQuery”."
            } else when (tab) {
                DashboardTab.TODAY -> "Crea una tarea o revisa lo que viene después."
                DashboardTab.UPCOMING -> "Las tareas con fecha futura aparecerán aquí."
                DashboardTab.COMPLETED -> "Tus avances se guardarán en esta sección."
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(18.dp).size(30.dp)
                )
            }

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
