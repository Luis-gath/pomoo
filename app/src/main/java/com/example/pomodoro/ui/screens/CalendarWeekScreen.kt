package com.example.pomodoro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pomodoro.data.model.TaskEntity
import com.example.pomodoro.ui.tasks.TaskViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarWeekScreen(
    navController: NavController,
    viewModel: TaskViewModel
) {
    // State
    var currentWeekStart by remember { mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) }
    val dashboardState by viewModel.dashboardState.collectAsState()
    val tasks = dashboardState.allTasks
    
    // Filter tasks for current week
    val weekEnd = currentWeekStart.plusDays(6)
    val tasksForWeek = remember(tasks, currentWeekStart) {
        tasks.filter { task ->
            val effectiveMillis = task.dueDateTimeMillis ?: task.dueDateTime
            if (effectiveMillis == null) return@filter false
            
            val date = Instant.ofEpochMilli(effectiveMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            (date.isEqual(currentWeekStart) || date.isAfter(currentWeekStart)) && 
            (date.isEqual(weekEnd) || date.isBefore(weekEnd))
        }
    }

    if (dashboardState.showAddSheet) {
        com.example.pomodoro.ui.components.AddTaskBottomSheet(
            onDismiss = { viewModel.hideAddSheet() },
            onSave = { 
                viewModel.createTask(it)  // Save
            },
            onSaveAndStart = {
                viewModel.createTask(it) { created ->
                    viewModel.startTask(created) { /* Started */ }
                }
            },
            // Pre-fill date if available
            initialDate = dashboardState.preSelectedDate 
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Calendario", fontWeight = FontWeight.Bold)
                        val formatter = DateTimeFormatter.ofPattern("d MMM")
                        Text(
                            "${currentWeekStart.format(formatter)} - ${weekEnd.format(formatter)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }) {
                        Icon(Icons.Default.Today, "Hoy")
                    }
                    IconButton(onClick = { currentWeekStart = currentWeekStart.minusWeeks(1) }) {
                        Icon(Icons.Default.ArrowBack, "Anterior") // Reusing ArrowBack for simple prev
                    }
                     IconButton(onClick = { currentWeekStart = currentWeekStart.plusWeeks(1) }) {
                        Icon(Icons.Default.ArrowForward, "Siguiente")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Week Header
            WeekHeader(currentWeekStart)
            Divider()
            
            // Time Grid
            TimeGrid(
                tasks = tasksForWeek,
                weekStart = currentWeekStart,
                onTaskClick = { task -> 
                    navController.navigate("task_editor?taskId=${task.id}")
                },
                onEmptySlotClick = { date, hour -> 
                     // Create timestamp for Date + Hour
                     val time = date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                     viewModel.showAddSheet(time)
                },
                onPostpone = { task, minutes ->
                    viewModel.postponeTask(task, minutes)
                }
            )
        }
    }
}

@Composable
fun WeekHeader(startDate: LocalDate) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        val today = LocalDate.now()
        // First column spacer for time labels
        Spacer(modifier = Modifier.width(40.dp))
        
        for (i in 0 until 7) {
            val date = startDate.plusDays(i.toLong())
            val isToday = date.isEqual(today)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfWeek.name.take(3), 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${date.dayOfMonth}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun TimeGrid(
    tasks: List<TaskEntity>,
    weekStart: LocalDate,
    onTaskClick: (TaskEntity) -> Unit,
    onEmptySlotClick: (LocalDate, Int) -> Unit,
    onPostpone: (TaskEntity, Int) -> Unit
) {
    val scrollState = rememberLazyListState()
    val hourHeight = 60.dp // 1 dp per minute for easy calc
    
    // Grid Setup
    // Use Box to overlay scroll
    
    Box(modifier = Modifier.fillMaxSize()) {
        val verticalScrollState = rememberScrollState()
        
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // --- Time Labels Column ---
            Column(modifier = Modifier.width(50.dp).padding(top = 10.dp)) {
                for (i in 0..23) { // 00:00 to 23:00 (Full Day)
                    Text(
                        text = "%02d:00".format(i),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .height(hourHeight)
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                Spacer(Modifier.height(hourHeight)) // Extra space at bottom
            }

            // --- Days Columns ---
            for (dayIndex in 0 until 7) {
                val date = weekStart.plusDays(dayIndex.toLong())
                val isToday = date.isEqual(LocalDate.now())
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(hourHeight * 24) // Total height for 24 hours
                        .border(
                            width = 0.5.dp, 
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                ) {
                    // Background Lines for hours
                    Column {
                        for (i in 0..23) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(hourHeight)
                                    .border(
                                        width = 0.5.dp, 
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                                    )
                                    .clickable { onEmptySlotClick(date, i) }
                            )
                        }
                    }

                    // Render Tasks
                    val tasksForDay = tasks.filter { task ->
                        val effMillis = task.dueDateTimeMillis ?: task.dueDateTime
                        if (effMillis == null) return@filter false
                        val taskDate = Instant.ofEpochMilli(effMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                        taskDate.isEqual(date)
                    }

                    // "Now" Indicator
                    if (isToday) {
                        val nowTime = java.time.LocalTime.now()
                        val nowMinutes = nowTime.hour * 60 + nowTime.minute
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = nowMinutes.dp)
                                .height(1.dp)
                                .background(Color.Red)
                        )
                        Box(
                            modifier = Modifier
                                .offset(y = (nowMinutes - 3).dp, x = (-4).dp)
                                .size(6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color.Red)
                        )
                    }

                    tasksForDay.forEach { task ->
                        val effMillis = task.dueDateTimeMillis ?: task.dueDateTime!!
                        val taskTime = Instant.ofEpochMilli(effMillis).atZone(ZoneId.systemDefault())
                        val hour = taskTime.hour
                        val minute = taskTime.minute
                        
                        // Render full day tasks
                        val startOffsetMinutes = hour * 60 + minute
                        val durationMinutes = task.durationMinutes
                        
                        TaskBlock(
                            task = task,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp)
                                .offset(y = startOffsetMinutes.dp)
                                .height(durationMinutes.dp)
                                .heightIn(min = 20.dp), // Ensure minimal visibility
                            onClick = { onTaskClick(task) },
                            onPostpone = { minutes -> onPostpone(task, minutes) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TaskBlock(
    task: TaskEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPostpone: (Int) -> Unit // New callback
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    lineHeight = 11.sp
                )
                val dur = task.computedDurationMinutes
                val durText = if (dur >= 60) "${dur / 60}h ${dur % 60}m" else "${dur}m"

                Text(
                    text = "$durText • ${task.completedPomodoros}/${task.totalPomodoros} \uD83C\uDF45",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Posponer 10 min") },
                onClick = { 
                    onPostpone(10)
                    showMenu = false 
                }
            )
            DropdownMenuItem(
                text = { Text("Posponer 30 min") },
                onClick = { 
                    onPostpone(30)
                    showMenu = false 
                }
            )
            DropdownMenuItem(
                text = { Text("Posponer 1 hora") },
                onClick = { 
                    onPostpone(60)
                    showMenu = false 
                }
            )
        }
    }
}
