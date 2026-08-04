@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.pomodoro.features.tasks.presentation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskStatus
import com.example.pomodoro.features.tasks.domain.ScheduleEditScope
import com.example.pomodoro.features.tasks.domain.calendarStartMillis
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private enum class CalendarDisplayMode { WEEK, MONTH }

private val calendarLocale = Locale("es", "PE")
private val dayTitleFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", calendarLocale)
private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", calendarLocale)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", calendarLocale)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarWeekScreen(
    navController: NavController,
    viewModel: TaskViewModel
) {
    val today = LocalDate.now()
    var selectedDateText by rememberSaveable { mutableStateOf(today.toString()) }
    var anchorDateText by rememberSaveable { mutableStateOf(today.toString()) }
    var displayModeText by rememberSaveable { mutableStateOf(CalendarDisplayMode.WEEK.name) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val selectedDate = LocalDate.parse(selectedDateText)
    val anchorDate = LocalDate.parse(anchorDateText)
    val displayMode = CalendarDisplayMode.valueOf(displayModeText)
    val dashboardState by viewModel.dashboardState.collectAsState()
    val scheduleEditMessage by viewModel.scheduleEditMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val tasksByDate = remember(dashboardState.allTasks) {
        dashboardState.allTasks
            .mapNotNull { task -> task.calendarStartMillis?.let { it.toLocalDate() to task } }
            .groupBy({ it.first }, { it.second })
    }
    val selectedTasks = remember(tasksByDate, selectedDate) {
        tasksByDate[selectedDate].orEmpty().sortedBy { it.calendarStartMillis }
    }

    LaunchedEffect(scheduleEditMessage) {
        scheduleEditMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeScheduleEditMessage()
        }
    }

    if (dashboardState.showAddSheet) {
        AddTaskBottomSheet(
            onDismiss = viewModel::hideAddSheet,
            onSave = viewModel::createTask,
            onSaveAndStart = { data ->
                viewModel.createTask(data) { created -> viewModel.startTask(created) {} }
            },
            initialDate = dashboardState.preSelectedDate
        )
    }

    editingTask?.let { task ->
        ScheduleEditDialog(
            task = task,
            onDismiss = { editingTask = null },
            onOpenTask = {
                editingTask = null
                navController.navigate("task_editor?taskId=${task.id}")
            },
            onSave = { newStart, scope ->
                viewModel.rescheduleTask(task, newStart, scope)
                editingTask = null
            }
        )
    }

    fun selectDate(date: LocalDate) {
        selectedDateText = date.toString()
        anchorDateText = date.toString()
    }

    fun addSessionFor(date: LocalDate) {
        val defaultTime = if (date == today) {
            LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
        } else {
            LocalTime.of(9, 0)
        }
        val millis = LocalDateTime.of(date, defaultTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        viewModel.showAddSheet(millis)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Calendario", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = selectedDate.format(dayTitleFormatter).sentenceCase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { selectDate(today) }) {
                        Icon(Icons.Default.Today, contentDescription = "Ir a hoy")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CalendarModeSelector(
                    selected = displayMode,
                    onSelected = {
                        displayModeText = it.name
                        anchorDateText = selectedDateText
                    }
                )
            }

            item {
                val periodTitle = when (displayMode) {
                    CalendarDisplayMode.WEEK -> {
                        val start = anchorDate.with(
                            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                        )
                        val end = start.plusDays(6)
                        if (YearMonth.from(start) == YearMonth.from(end)) {
                            start.format(monthTitleFormatter).sentenceCase()
                        } else {
                            "${start.monthName()} - ${end.monthName()} ${end.year}"
                        }
                    }
                    CalendarDisplayMode.MONTH ->
                        anchorDate.format(monthTitleFormatter).sentenceCase()
                }
                PeriodNavigation(
                    title = periodTitle,
                    onPrevious = {
                        val moved = when (displayMode) {
                            CalendarDisplayMode.WEEK -> anchorDate.minusWeeks(1)
                            CalendarDisplayMode.MONTH -> anchorDate.minusMonths(1)
                        }
                        selectDate(moved)
                    },
                    onNext = {
                        val moved = when (displayMode) {
                            CalendarDisplayMode.WEEK -> anchorDate.plusWeeks(1)
                            CalendarDisplayMode.MONTH -> anchorDate.plusMonths(1)
                        }
                        selectDate(moved)
                    }
                )
            }

            item {
                when (displayMode) {
                    CalendarDisplayMode.WEEK -> WeekSelector(
                        anchorDate = anchorDate,
                        selectedDate = selectedDate,
                        tasksByDate = tasksByDate,
                        onSelectDate = ::selectDate
                    )
                    CalendarDisplayMode.MONTH -> MonthGrid(
                        visibleMonth = YearMonth.from(anchorDate),
                        selectedDate = selectedDate,
                        tasksByDate = tasksByDate,
                        onSelectDate = ::selectDate
                    )
                }
            }

            item {
                AgendaHeader(
                    date = selectedDate,
                    taskCount = selectedTasks.size,
                    onAdd = { addSessionFor(selectedDate) }
                )
            }

            if (selectedTasks.isEmpty()) {
                item { EmptyAgenda(onAdd = { addSessionFor(selectedDate) }) }
            } else {
                items(selectedTasks, key = { it.id }) { task ->
                    CalendarTaskCard(task = task, onEdit = { editingTask = task })
                }
            }
        }
    }
}

@Composable
private fun CalendarModeSelector(
    selected: CalendarDisplayMode,
    onSelected: (CalendarDisplayMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == CalendarDisplayMode.WEEK,
            onClick = { onSelected(CalendarDisplayMode.WEEK) },
            label = { Text("Semana") },
            leadingIcon = {
                Icon(
                    Icons.Default.CalendarViewWeek,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selected == CalendarDisplayMode.MONTH,
            onClick = { onSelected(CalendarDisplayMode.MONTH) },
            label = { Text("Mes completo") },
            leadingIcon = {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PeriodNavigation(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Periodo anterior")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Periodo siguiente")
        }
    }
}

@Composable
private fun WeekSelector(
    anchorDate: LocalDate,
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskEntity>>,
    onSelectDate: (LocalDate) -> Unit
) {
    val weekStart = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(7) { index ->
            val date = weekStart.plusDays(index.toLong())
            val selected = date == selectedDate
            val isToday = date == LocalDate.now()
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectDate(date) },
                color = when {
                    selected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                },
                shape = RoundedCornerShape(14.dp),
                border = if (isToday) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                } else null
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.shortWeekday(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(Modifier.height(5.dp))
                    TaskCountIndicator(tasksByDate[date].orEmpty().size)
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskEntity>>,
    onSelectDate: (LocalDate) -> Unit
) {
    val firstCell = visibleMonth.atDay(1)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useCompactCells = maxWidth >= 600.dp
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    DayOfWeek.entries.forEach { day ->
                        Text(
                            text = day.getDisplayName(
                                java.time.format.TextStyle.NARROW,
                                calendarLocale
                            ).uppercase(calendarLocale),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 6.dp)
                        )
                    }
                }
                repeat(6) { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(7) { column ->
                            val date = firstCell.plusDays((row * 7 + column).toLong())
                            MonthDayCell(
                                date = date,
                                isInMonth = YearMonth.from(date) == visibleMonth,
                                isSelected = date == selectedDate,
                                taskCount = tasksByDate[date].orEmpty().size,
                                useCompactHeight = useCompactCells,
                                onClick = { onSelectDate(date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    isInMonth: Boolean,
    isSelected: Boolean,
    taskCount: Int,
    useCompactHeight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = date == LocalDate.now()
    Box(
        modifier = modifier
            .then(
                if (useCompactHeight) Modifier.height(58.dp)
                else Modifier.aspectRatio(1f)
            )
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .then(
                if (isToday) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isInMonth) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                }
            )
            Spacer(Modifier.height(4.dp))
            TaskDensityIndicator(taskCount)
        }
    }
}

@Composable
private fun TaskCountIndicator(count: Int) {
    if (count == 0) {
        Spacer(Modifier.height(6.dp))
        return
    }
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = CircleShape
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun TaskDensityIndicator(count: Int) {
    if (count == 0) {
        Spacer(Modifier.height(6.dp))
        return
    }

    Row(
        modifier = Modifier.height(6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (count <= 5) {
            repeat(count) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun AgendaHeader(date: LocalDate, taskCount: Int, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = date.format(dayTitleFormatter).sentenceCase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when (taskCount) {
                    0 -> "Sin sesiones programadas"
                    1 -> "1 sesión programada"
                    else -> "$taskCount sesiones programadas"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Agregar")
        }
    }
}

@Composable
private fun EmptyAgenda(onAdd: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tu día está disponible",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Añade una sesión y reserva un momento concreto para estudiar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Programar sesión")
            }
        }
    }
}

@Composable
private fun CalendarTaskCard(task: TaskEntity, onEdit: () -> Unit) {
    val startMillis = task.calendarStartMillis ?: return
    val start = startMillis.toLocalDateTime()
    val end = (task.endDateTimeMillis
        ?: (startMillis + task.computedDurationMinutes * 60_000L)).toLocalDateTime()

    OutlinedCard(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.width(62.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = start.format(timeFormatter),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = end.format(timeFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .width(3.dp)
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (task.courseOrProject.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = task.courseOrProject,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = buildString {
                        append(formatDuration(task.computedDurationMinutes))
                        append(" · ")
                        append(task.completedPomodoros)
                        append("/")
                        append(task.totalPomodoros)
                        append(" bloques")
                        if (task.status == TaskStatus.DONE) append(" · Completada")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Modificar horario",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ScheduleEditDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onOpenTask: () -> Unit,
    onSave: (Long, ScheduleEditScope) -> Unit
) {
    val context = LocalContext.current
    val original = task.calendarStartMillis?.toLocalDateTime() ?: return
    var selectedDate by remember(task.id) { mutableStateOf(original.toLocalDate()) }
    var selectedTime by remember(task.id) {
        mutableStateOf(original.toLocalTime().withSecond(0).withNano(0))
    }
    var scope by remember(task.id) { mutableStateOf(ScheduleEditScope.THIS_DAY) }
    val canEditSeries = task.scheduleSeriesId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modificar horario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    selectedDate = LocalDate.of(year, month + 1, day)
                                },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedDate.format(DateTimeFormatter.ofPattern("dd MMM", calendarLocale)))
                    }
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> selectedTime = LocalTime.of(hour, minute) },
                                selectedTime.hour,
                                selectedTime.minute,
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedTime.format(timeFormatter))
                    }
                }

                Text(
                    text = "Aplicar el cambio a",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ScopeOption(
                    selected = scope == ScheduleEditScope.THIS_DAY,
                    title = "Sólo este día",
                    description = "Modifica únicamente esta sesión.",
                    onClick = { scope = ScheduleEditScope.THIS_DAY }
                )
                ScopeOption(
                    selected = scope == ScheduleEditScope.ALL_WEEKS,
                    enabled = canEditSeries,
                    title = "Todas las semanas",
                    description = if (canEditSeries) {
                        "Aplica el día y la hora a las sesiones pendientes de este mismo día."
                    } else {
                        "Disponible en los horarios y hábitos creados desde la planificación."
                    },
                    onClick = { if (canEditSeries) scope = ScheduleEditScope.ALL_WEEKS }
                )
                TextButton(
                    onClick = onOpenTask,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Editar detalles de la tarea")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newStart = LocalDateTime.of(selectedDate, selectedTime)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    onSave(newStart, scope)
                }
            ) {
                Text("Guardar cambio")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ScopeOption(
    selected: Boolean,
    enabled: Boolean = true,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick, enabled = enabled)
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.55f
                    )
                )
            }
        }
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

private fun LocalDate.shortWeekday(): String =
    format(DateTimeFormatter.ofPattern("EEE", calendarLocale))
        .removeSuffix(".")
        .take(2)
        .uppercase(calendarLocale)

private fun LocalDate.monthName(): String =
    format(DateTimeFormatter.ofPattern("MMM", calendarLocale))
        .removeSuffix(".")
        .sentenceCase()

private fun String.sentenceCase(): String = replaceFirstChar { first ->
    if (first.isLowerCase()) first.titlecase(calendarLocale) else first.toString()
}

private fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
