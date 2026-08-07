package com.example.pomodoro.features.tasks.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.pomodoro.features.tasks.data.RepeatType
import com.example.pomodoro.features.tasks.data.TaskPriority
import com.example.pomodoro.features.tasks.domain.RecurrenceGenerator
import com.example.pomodoro.features.tasks.domain.StudyTemplate
import com.example.pomodoro.shared.ui.components.PickerDateUtils
import com.example.pomodoro.shared.ui.components.PomodoroDatePickerDialog
import com.example.pomodoro.shared.ui.components.PomodoroTimePickerDialog
import java.text.SimpleDateFormat
import java.util.*

/**
 * BottomSheet interactivo para crear tareas rápidamente
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    onDismiss: () -> Unit,
    onSave: (TaskData) -> Unit,
    onSaveAndStart: (TaskData) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    initialDate: Long? = null // New param
) {
    var title by remember { mutableStateOf("") }
    var courseOrProject by remember { mutableStateOf("") }
    var showCustomCourse by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(initialDate) } // Use initial date if present
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var totalPomodoros by remember { mutableIntStateOf(4) }
    var selectedTemplate by remember { mutableStateOf<StudyTemplate?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }
    var focusMinutes by remember { mutableIntStateOf(25) }
    var shortBreakMinutes by remember { mutableIntStateOf(5) }
    var longBreakMinutes by remember { mutableIntStateOf(15) }
    var longBreakEvery by remember { mutableIntStateOf(4) }
    var includeFinalBreak by remember { mutableStateOf(false) }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    val isValid = title.isNotBlank()
    
    // Cursos/proyectos sugeridos
    val suggestedCourses = listOf(
        "Estudio", "Programación", "Matemáticas",
        "Física", "Diseño", "Trabajo", "Personal"
    )
    
    val createTaskData = {
        TaskData(
            title = title.trim(),
            courseOrProject = courseOrProject.trim(),
            dueDateTime = selectedDate,
            priority = priority,
            totalPomodoros = totalPomodoros,
            focusMinutes = focusMinutes,
            shortBreakMinutes = shortBreakMinutes,
            longBreakMinutes = longBreakMinutes,
            longBreakEvery = longBreakEvery,
            includeFinalBreak = includeFinalBreak,
            repeatType = repeatType
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "Nueva tarea",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(24.dp))
            
            // --- Campo de título ---
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("¿Qué vas a hacer?") },
                placeholder = { Text("Ej: Estudiar capítulo 5") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp)
            )

            // El foco se pide DESDE DENTRO del sheet y a prueba de fallos: el contenido de
            // un ModalBottomSheet se compone en su propia ventana, así que pedirlo antes
            // (como se hacía) puede lanzar "FocusRequester is not initialized" al abrirlo.
            LaunchedEffect(Unit) {
                runCatching { focusRequester.requestFocus() }
            }


            Spacer(Modifier.height(20.dp))
            
            // --- Selector de curso/proyecto ---
            Text(
                text = "Curso o Proyecto",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestedCourses) { course ->
                    val isSelected = courseOrProject == course
                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            courseOrProject = if (isSelected) "" else course
                            showCustomCourse = false
                        },
                        label = { Text(course) }
                    )
                }
                
                item {
                    FilterChip(
                        selected = showCustomCourse,
                        onClick = { 
                            showCustomCourse = true
                            courseOrProject = ""
                        },
                        label = { Text("+ Otro") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            
            AnimatedVisibility(visible = showCustomCourse) {
                OutlinedTextField(
                    value = if (courseOrProject in suggestedCourses) "" else courseOrProject,
                    onValueChange = { courseOrProject = it },
                    label = { Text("Nombre personalizado") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Spacer(Modifier.height(20.dp))
            
            // --- Selector de fecha ---
            Text(
                text = "¿Cuándo?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(Modifier.height(8.dp))
            
            DateQuickPicker(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it }
            )

            // --- Repetición ---
            // Solo tiene sentido con fecha: sin ella no se sabe desde cuándo contar.
            if (selectedDate != null) {
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Repetir",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatType.entries.forEach { type ->
                        FilterChip(
                            selected = repeatType == type,
                            onClick = { repeatType = type },
                            label = { Text(type.quickLabel()) }
                        )
                    }
                }

                if (repeatType != RepeatType.NONE) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Se crearán ${RecurrenceGenerator.defaultCount(repeatType)} tareas. " +
                            "Ajusta el detalle desde el editor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // --- Selector de prioridad ---
            Text(
                text = "Prioridad",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(Modifier.height(8.dp))
            
            PrioritySelector(
                selected = priority,
                onSelect = { priority = it }
            )
            
            Spacer(Modifier.height(20.dp))

            // --- Plantilla de estudio ---
            // Rellena de golpe la configuración Pomodoro según el tipo de material, para no
            // tener que decidir cinco números en cada tarea. Sigue siendo editable debajo.
            Text(
                text = "Tipo de estudio",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StudyTemplate.entries) { template ->
                    FilterChip(
                        selected = selectedTemplate == template,
                        onClick = {
                            selectedTemplate = template
                            totalPomodoros = template.totalPomodoros
                            focusMinutes = template.focusMinutes
                            shortBreakMinutes = template.shortBreakMinutes
                            longBreakMinutes = template.longBreakMinutes
                            longBreakEvery = template.longBreakEvery
                        },
                        label = { Text(template.label) }
                    )
                }
            }

            selectedTemplate?.let { template ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${template.hint} · unos ${template.approximateMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            // --- Selector de Pomodoros ---
            PomodoroStepper(
                value = totalPomodoros,
                onValueChange = { totalPomodoros = it }
            )
            
            // --- Feedback de Duración ---
            val durationMinutes = com.example.pomodoro.features.tasks.domain.TaskDurationCalculator.calculateTotalMinutes(
                totalPomodoros, focusMinutes, shortBreakMinutes, longBreakMinutes, longBreakEvery, includeFinalBreak
            )
            val durationText = "${durationMinutes / 60}h ${durationMinutes % 60}m"
            
            val endTimeText = selectedDate?.let { start ->
                val endMillis = com.example.pomodoro.features.tasks.domain.TaskDurationCalculator.calculateEndTime(start, durationMinutes)
                endMillis?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it)) }
            }
            
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
                    onCheckedChange = { includeFinalBreak = it },
                    thumbContent = if (includeFinalBreak) {
                        { Icon(Icons.Default.Check, null, Modifier.size(12.dp)) }
                    } else null
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // --- Ajustes avanzados (colapsable) ---
            Surface(
                onClick = { showAdvanced = !showAdvanced },
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Ajustes avanzados",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            AnimatedVisibility(
                visible = showAdvanced,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CompactStepper(
                            value = focusMinutes,
                            onValueChange = { focusMinutes = it },
                            label = "Enfoque",
                            minValue = 1,
                            maxValue = 90
                        )
                        CompactStepper(
                            value = shortBreakMinutes,
                            onValueChange = { shortBreakMinutes = it },
                            label = "Descanso corto",
                            minValue = 1,
                            maxValue = 30
                        )
                        CompactStepper(
                            value = longBreakMinutes,
                            onValueChange = { longBreakMinutes = it },
                            label = "Descanso largo",
                            minValue = 1,
                            maxValue = 60
                        )
                        CompactStepper(
                            value = longBreakEvery,
                            onValueChange = { longBreakEvery = it },
                            label = "Largo cada",
                            suffix = "ciclos",
                            minValue = 2,
                            maxValue = 8
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // --- Botones de acción ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (isValid) {
                            onSave(createTaskData())
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar")
                }
                
                Button(
                    onClick = {
                        if (isValid) {
                            onSaveAndStart(createTaskData())
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar e Iniciar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateQuickPicker(
    selectedDate: Long?,
    onDateSelected: (Long?) -> Unit
) {
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.timeInMillis
    }
    
    val tomorrow = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.timeInMillis
    }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempDateMillis by remember { mutableStateOf<Long?>(null) }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val isToday = selectedDate != null && isSameDay(selectedDate, today)
        val isTomorrow = selectedDate != null && isSameDay(selectedDate, tomorrow)
        val isCustom = selectedDate != null && !isToday && !isTomorrow
        
        FilterChip(
            selected = selectedDate == null,
            onClick = { onDateSelected(null) },
            label = { Text("Sin fecha") }
        )
        
        FilterChip(
            selected = isToday,
            onClick = { onDateSelected(today) }, // Defaults to end of day? Or maybe schedule for next hour? Let's generic end of day or current time.
            label = { Text("Hoy") }
        )
        
        FilterChip(
            selected = isTomorrow,
            onClick = { onDateSelected(tomorrow) },
            label = { Text("Mañana") }
        )
        
        FilterChip(
            selected = isCustom,
            onClick = { showDatePicker = true },
            label = { 
                Text(
                    if (selectedDate != null) SimpleDateFormat("dd MMM HH:mm", Locale("es")).format(Date(selectedDate))
                    else "Elegir"
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
    
    if (showDatePicker) {
        PomodoroDatePickerDialog(
            initialDateMillis = selectedDate,
            confirmText = "Siguiente",
            onDismiss = { showDatePicker = false },
            onConfirm = { utcDateMillis ->
                tempDateMillis = utcDateMillis
                showDatePicker = false
                showTimePicker = true // Encadena con el selector de hora
            }
        )
    }

    if (showTimePicker) {
        val reference = selectedDate ?: System.currentTimeMillis()
        PomodoroTimePickerDialog(
            initialHour = PickerDateUtils.hourOf(reference),
            initialMinute = PickerDateUtils.minuteOf(reference),
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                // La conversión UTC -> local vive en PickerDateUtils: combinar a mano la
                // medianoche UTC con una hora local restaba un día en zonas UTC negativas.
                val dateMillis = tempDateMillis
                    ?: PickerDateUtils.toPickerUtcMillis(System.currentTimeMillis())
                onDateSelected(PickerDateUtils.combineDateAndTime(dateMillis, hour, minute))
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrioritySelector(
    selected: TaskPriority,
    onSelect: (TaskPriority) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskPriority.entries.forEach { priority ->
            val (color, label) = when (priority) {
                TaskPriority.LOW -> Pair(Color(0xFF66BB6A), "Baja")
                TaskPriority.MEDIUM -> Pair(Color(0xFFFFB74D), "Media")
                TaskPriority.HIGH -> Pair(Color(0xFFEF5350), "Alta")
            }
            
            FilterChip(
                selected = selected == priority,
                onClick = { onSelect(priority) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.2f)
                )
            )
        }
    }
}

private fun isSameDay(ts1: Long, ts2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = ts1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = ts2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

/**
 * Datos de tarea para crear/actualizar
 */
/** Etiquetas cortas: en el alta rápida no cabe «No repetir». */
private fun RepeatType.quickLabel(): String = when (this) {
    RepeatType.NONE -> "Una vez"
    RepeatType.DAILY -> "Diario"
    RepeatType.WEEKLY -> "Semanal"
    RepeatType.MONTHLY -> "Mensual"
}

data class TaskData(
    val title: String,
    val courseOrProject: String = "",
    val dueDateTime: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val totalPomodoros: Int = 4,
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakEvery: Int = 4,
    val includeFinalBreak: Boolean = false,

    /**
     * El alta rápida solo ofrece la frecuencia; el número de repeticiones toma el valor
     * por defecto de cada una. Para ajustarlo está el editor completo, y así lo rápido
     * sigue siendo rápido.
     */
    val repeatType: RepeatType = RepeatType.NONE
)
