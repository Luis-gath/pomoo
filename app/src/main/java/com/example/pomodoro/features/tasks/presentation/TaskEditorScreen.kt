package com.example.pomodoro.features.tasks.presentation

import com.example.pomodoro.shared.ui.components.PickerDateUtils
import com.example.pomodoro.shared.ui.components.PomodoroDatePickerDialog
import com.example.pomodoro.shared.ui.components.PomodoroTimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pomodoro.features.tasks.data.RepeatType
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskPriority
import com.example.pomodoro.features.tasks.domain.ScheduleEditScope
import com.example.pomodoro.features.tasks.domain.calendarStartMillis
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.pomodoro.core.audio.AudioRecorder
import com.example.pomodoro.core.audio.AudioPlayer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(
    taskId: Int?,
    viewModel: TaskViewModel,
    onBack: () -> Unit,
    onStartTask: ((TaskEntity) -> Unit)? = null // Callback para iniciar tarea
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var courseOrProject by remember { mutableStateOf("") }
    var dueDateTime by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }
    var isNotificationEnabled by remember { mutableStateOf(true) }
    var isVoiceEnabled by remember { mutableStateOf(false) }
    var audioUri by remember { mutableStateOf<String?>(null) }

    // Configuración Pomodoro por tarea
    var totalPomodoros by remember { mutableFloatStateOf(4f) }
    var focusMinutes by remember { mutableFloatStateOf(25f) }
    var shortBreakMinutes by remember { mutableFloatStateOf(5f) }
    var longBreakMinutes by remember { mutableFloatStateOf(15f) }
    var longBreakEvery by remember { mutableFloatStateOf(4f) }
    var taskAutoStartNext by remember { mutableStateOf(false) }
    var completedPomodoros by remember { mutableIntStateOf(0) }
    var includeFinalBreak by remember { mutableStateOf(false) }

    // Recording State
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var isRecording by remember { mutableStateOf(false) }
    val audioRecorder = remember { AudioRecorder(context) }
    val audioPlayer = remember { AudioPlayer(context) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val editingTask by viewModel.editingTask.collectAsState()
    var pendingSave by remember { mutableStateOf<TaskEntity?>(null) }
    var startAfterSave by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        viewModel.loadTaskForEdit(taskId)
    }

    LaunchedEffect(editingTask) {
        editingTask?.let { task ->
            title = task.title
            notes = task.notes
            courseOrProject = task.courseOrProject
            dueDateTime = task.calendarStartMillis ?: task.timestamp
            priority = task.priority
            repeatType = task.repeatType
            isNotificationEnabled = task.isNotificationEnabled
            isVoiceEnabled = task.isVoiceEnabled
            audioUri = task.audioUri
            // Cargar configuración Pomodoro
            totalPomodoros = task.totalPomodoros.toFloat()
            focusMinutes = task.focusMinutes.toFloat()
            shortBreakMinutes = task.shortBreakMinutes.toFloat()
            longBreakMinutes = task.longBreakMinutes.toFloat()
            longBreakEvery = task.longBreakEvery.toFloat()
            taskAutoStartNext = task.taskAutoStartNext
            completedPomodoros = task.completedPomodoros
            includeFinalBreak = task.includeFinalBreak
        }
    }

    fun buildTask(): TaskEntity {
        val now = System.currentTimeMillis()
        
        val computedDuration = com.example.pomodoro.features.tasks.domain.TaskDurationCalculator.calculateTotalMinutes(
            totalPomodoros = totalPomodoros.roundToInt(),
            focusMinutes = focusMinutes.roundToInt(),
            shortBreakMinutes = shortBreakMinutes.roundToInt(),
            longBreakMinutes = longBreakMinutes.roundToInt(),
            longBreakEvery = longBreakEvery.roundToInt(),
            includeFinalBreak = includeFinalBreak
        )
        
        val startMillis = dueDateTime ?: now
        val endMillis = com.example.pomodoro.features.tasks.domain.TaskDurationCalculator.calculateEndTime(dueDateTime, computedDuration)

        val base = editingTask ?: TaskEntity(title = title, createdAt = now)

        return base.copy(
            id = taskId ?: 0,
            title = title,
            notes = notes,
            courseOrProject = courseOrProject,
            dueDateTime = dueDateTime,
            dueDateTimeMillis = dueDateTime, // Sync both fields
            startDateTimeMillis = startMillis,
            endDateTimeMillis = endMillis,
            computedDurationMinutes = computedDuration,
            includeFinalBreak = includeFinalBreak,
            timestamp = startMillis,
            priority = priority,
            repeatType = repeatType,
            isNotificationEnabled = isNotificationEnabled,
            isVoiceEnabled = isVoiceEnabled,
            audioUri = audioUri,
            totalPomodoros = totalPomodoros.roundToInt(),
            completedPomodoros = completedPomodoros,
            focusMinutes = focusMinutes.roundToInt(),
            shortBreakMinutes = shortBreakMinutes.roundToInt(),
            longBreakMinutes = longBreakMinutes.roundToInt(),
            longBreakEvery = longBreakEvery.roundToInt(),
            taskAutoStartNext = taskAutoStartNext,
            createdAt = base.createdAt,
            updatedAt = now
        )
    }

    fun saveWithScope(
        task: TaskEntity,
        scope: ScheduleEditScope,
        shouldStart: Boolean = startAfterSave
    ) {
        viewModel.saveTask(task, scope) { savedTask ->
            if (shouldStart) onStartTask?.invoke(savedTask) else onBack()
        }
        pendingSave = null
        startAfterSave = false
    }

    fun requestSave(shouldStart: Boolean) {
        val task = buildTask()
        if (taskId != null && editingTask?.scheduleSeriesId != null) {
            pendingSave = task
            startAfterSave = shouldStart
        } else {
            saveWithScope(task, ScheduleEditScope.THIS_DAY, shouldStart)
        }
    }

    pendingSave?.let { task ->
        TaskEditScopeDialog(
            taskTitle = task.title,
            onDismiss = {
                pendingSave = null
                startAfterSave = false
            },
            onThisTask = { saveWithScope(task, ScheduleEditScope.THIS_DAY) },
            onAllWeeks = { saveWithScope(task, ScheduleEditScope.ALL_WEEKS) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "Nueva tarea" else "Editar tarea") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        requestSave(shouldStart = false)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = courseOrProject,
                onValueChange = { courseOrProject = it },
                label = { Text("Tema / Curso") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas / Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Date & Time Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dueDateTime ?: System.currentTimeMillis())))
                }

                Button(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(dueDateTime ?: System.currentTimeMillis())))
                }
            }

            // Mismos selectores de Material 3 que la hoja "Nueva Tarea": antes esta
            // pantalla usaba los diálogos del sistema y se veía distinta haciendo lo mismo.
            if (showDatePicker) {
                val reference = dueDateTime ?: System.currentTimeMillis()
                PomodoroDatePickerDialog(
                    initialDateMillis = reference,
                    onDismiss = { showDatePicker = false },
                    onConfirm = { utcDateMillis ->
                        dueDateTime = PickerDateUtils.combineDateAndTime(
                            utcDateMillis,
                            PickerDateUtils.hourOf(reference),
                            PickerDateUtils.minuteOf(reference)
                        )
                        showDatePicker = false
                    }
                )
            }

            if (showTimePicker) {
                val reference = dueDateTime ?: System.currentTimeMillis()
                PomodoroTimePickerDialog(
                    initialHour = PickerDateUtils.hourOf(reference),
                    initialMinute = PickerDateUtils.minuteOf(reference),
                    onDismiss = { showTimePicker = false },
                    onConfirm = { hour, minute ->
                        dueDateTime = PickerDateUtils.combineDateAndTime(
                            PickerDateUtils.toPickerUtcMillis(reference),
                            hour,
                            minute
                        )
                        showTimePicker = false
                    }
                )
            }

            // Priority
            Text("Prioridad", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TaskPriority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.spanishLabel()) }
                    )
                }
            }

            // Repeat
            Text("Repetir", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RepeatType.entries.forEach { r ->
                    FilterChip(
                        selected = repeatType == r,
                        onClick = { repeatType = r },
                        label = { Text(r.spanishLabel()) }
                    )
                }
            }

            Divider()

            // ===== SECCIÓN CONFIGURACIÓN POMODORO =====
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Configuración Pomodoro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Pomodoros objetivo
                    PomodoroSlider(
                        label = "Pomodoros objetivo",
                        value = totalPomodoros,
                        onValueChange = { totalPomodoros = it },
                        valueRange = 1f..12f,
                        steps = 10,
                        valueText = "${totalPomodoros.roundToInt()}"
                    )
                    
                    // --- Feedback de Duración ---
                    val durationMinutes = com.example.pomodoro.features.tasks.domain.TaskDurationCalculator.calculateTotalMinutes(
                        totalPomodoros.roundToInt(),
                        focusMinutes.roundToInt(),
                        shortBreakMinutes.roundToInt(),
                        longBreakMinutes.roundToInt(),
                        longBreakEvery.roundToInt(),
                        includeFinalBreak
                    )
                    val durationText = "${durationMinutes / 60}h ${durationMinutes % 60}m"
                    
                    val endTimeText = dueDateTime?.let { start ->
                        val endMillis = com.example.pomodoro.features.tasks.domain.TaskDurationCalculator.calculateEndTime(start, durationMinutes)
                        endMillis?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it)) }
                    }
                    
                    Divider()
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Duración total: $durationText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (endTimeText != null) {
                            Text(
                                text = "Termina aprox: $endTimeText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Toggle Final Break
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Incluir descanso final",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = includeFinalBreak,
                            onCheckedChange = { includeFinalBreak = it }
                        )
                    }
                    Divider()

                    // Focus
                    PomodoroSlider(
                        label = "Enfoque",
                        value = focusMinutes,
                        onValueChange = { focusMinutes = it },
                        valueRange = 1f..90f,
                        steps = 88,
                        valueText = "${focusMinutes.roundToInt()} min"
                    )

                    // Descanso corto
                    PomodoroSlider(
                        label = "Descanso corto",
                        value = shortBreakMinutes,
                        onValueChange = { shortBreakMinutes = it },
                        valueRange = 1f..30f,
                        steps = 28,
                        valueText = "${shortBreakMinutes.roundToInt()} min"
                    )

                    // Descanso largo
                    PomodoroSlider(
                        label = "Descanso largo",
                        value = longBreakMinutes,
                        onValueChange = { longBreakMinutes = it },
                        valueRange = 1f..60f,
                        steps = 58,
                        valueText = "${longBreakMinutes.roundToInt()} min"
                    )

                    // Cada cuántos
                    PomodoroSlider(
                        label = "Descanso largo cada",
                        value = longBreakEvery,
                        onValueChange = { longBreakEvery = it },
                        valueRange = 2f..8f,
                        steps = 5,
                        valueText = "${longBreakEvery.roundToInt()} enfoques"
                    )

                    // Auto start
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Inicio automático", modifier = Modifier.weight(1f))
                        Switch(
                            checked = taskAutoStartNext,
                            onCheckedChange = { taskAutoStartNext = it }
                        )
                    }

                    // Botón Iniciar tarea
                    if (title.isNotBlank() && onStartTask != null) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                requestSave(shouldStart = true)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Iniciar esta tarea", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Divider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Notificación", modifier = Modifier.weight(1f))
                Switch(checked = isNotificationEnabled, onCheckedChange = { isNotificationEnabled = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hablar recordatorio (Voz)", modifier = Modifier.weight(1f))
                Switch(checked = isVoiceEnabled, onCheckedChange = { isVoiceEnabled = it })
            }

            Divider()

            Text("Audio de fondo (Nota de voz)", style = MaterialTheme.typography.labelLarge)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRecording) {
                    Button(
                        onClick = {
                            audioRecorder.stopRecording()
                            isRecording = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Detener")
                    }
                } else {
                    Button(onClick = {
                        val permission = android.Manifest.permission.RECORD_AUDIO
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val file = File(context.filesDir, "task_audio_${System.currentTimeMillis()}.mp4")
                            audioRecorder.startRecording(file)
                            audioUri = file.absolutePath
                            isRecording = true
                        } else {
                            permissionLauncher.launch(permission)
                        }
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (audioUri == null) "Grabar Nota" else "Grabar de nuevo")
                    }
                }

                if (audioUri != null && !isRecording) {
                    IconButton(onClick = {
                        audioPlayer.playFile(android.net.Uri.fromFile(File(audioUri!!)))
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir")
                    }
                }
            }
            
            if (audioUri != null) {
                Text("Nota grabada correctamente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TaskEditScopeDialog(
    taskTitle: String,
    onDismiss: () -> Unit,
    onThisTask: () -> Unit,
    onAllWeeks: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aplicar cambios") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = taskTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Elige si quieres modificar únicamente esta tarea o el mismo día del horario en todas las semanas pendientes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onThisTask,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sólo esta tarea")
                }
                Button(
                    onClick = onAllWeeks,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Todas las semanas")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun TaskPriority.spanishLabel(): String = when (this) {
    TaskPriority.LOW -> "Baja"
    TaskPriority.MEDIUM -> "Media"
    TaskPriority.HIGH -> "Alta"
}

private fun RepeatType.spanishLabel(): String = when (this) {
    RepeatType.NONE -> "No repetir"
    RepeatType.DAILY -> "Diaria"
    RepeatType.WEEKLY -> "Semanal"
    RepeatType.MONTHLY -> "Mensual"
}

@Composable
private fun PomodoroSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

