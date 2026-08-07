package com.example.pomodoro.features.tasks.presentation

import com.example.pomodoro.features.tasks.domain.ClassSchedulePlan
import androidx.compose.material.icons.filled.School
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pomodoro.features.tasks.domain.HabitSessionLength
import com.example.pomodoro.features.tasks.domain.StudyHabitPlan
import com.example.pomodoro.features.tasks.domain.StudyPlanCodeCodec
import com.example.pomodoro.features.tasks.domain.StudyScheduleShareFormatter
import com.example.pomodoro.features.tasks.domain.WeeklyScheduleTemplate
import com.example.pomodoro.shared.ui.components.PomodoroTimePickerDialog
import java.time.DayOfWeek
import java.time.LocalTime

private enum class PlannerMode { SCHEDULE, HABIT, CLASSES }

/** Centro de planificación: aplica plantillas, construye un hábito y comparte ambos planes. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WeeklyScheduleDialog(
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onApplySchedule: (WeeklyScheduleTemplate, Int, String, Boolean) -> Unit,
    onApplyHabit: (StudyHabitPlan) -> Unit,
    onApplyClasses: (ClassSchedulePlan) -> Unit
) {
    var mode by remember { mutableStateOf(PlannerMode.SCHEDULE) }

    var selectedTemplate by remember { mutableStateOf(WeeklyScheduleTemplate.INGENIERIA) }
    var scheduleWeeks by remember { mutableIntStateOf(2) }
    var course by remember { mutableStateOf("") }
    var reminders by remember { mutableStateOf(true) }

    // --- Horario de clases real ---
    var classCourse by remember { mutableStateOf("") }
    var classDays by remember { mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)) }
    var classStart by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var classEnd by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var classWeeks by remember { mutableIntStateOf(16) }
    var showClassStartPicker by remember { mutableStateOf(false) }
    var showClassEndPicker by remember { mutableStateOf(false) }

    val classPlan = ClassSchedulePlan(
        courseName = classCourse,
        days = classDays,
        startTime = classStart,
        endTime = classEnd,
        weeks = classWeeks,
        withReminders = reminders
    )

    var habitDays by remember {
        mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
    }
    var habitTime by remember { mutableStateOf(LocalTime.of(19, 0)) }
    var habitLength by remember { mutableStateOf(HabitSessionLength.STEADY) }
    var habitWeeks by remember { mutableIntStateOf(8) }
    var startCue by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var shareContent by remember { mutableStateOf<StudyPlanShareContent?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val habitPlan = StudyHabitPlan(
        days = habitDays,
        time = habitTime,
        sessionLength = habitLength,
        weeks = habitWeeks,
        courseOrProject = course,
        startCue = startCue,
        withReminders = reminders
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Planifica tu estudio",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Organiza sesiones que puedas cumplir y compartir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showImport = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Importar código o QR")
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlannerModeCard(
                        title = "Horario",
                        description = "Plantillas por materia",
                        icon = Icons.Default.EventRepeat,
                        selected = mode == PlannerMode.SCHEDULE,
                        onClick = { mode = PlannerMode.SCHEDULE },
                        modifier = Modifier.weight(1f)
                    )
                    PlannerModeCard(
                        title = "Crear hábito",
                        description = "Rutina constante",
                        icon = Icons.Default.TrackChanges,
                        selected = mode == PlannerMode.HABIT,
                        onClick = { mode = PlannerMode.HABIT },
                        modifier = Modifier.weight(1f)
                    )
                    PlannerModeCard(
                        title = "Mis clases",
                        description = "Horario real",
                        icon = Icons.Default.School,
                        selected = mode == PlannerMode.CLASSES,
                        onClick = { mode = PlannerMode.CLASSES },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (mode == PlannerMode.CLASSES) {
                    ClassPlannerContent(
                        plan = classPlan,
                        onCourseChanged = { classCourse = it },
                        onToggleDay = { day ->
                            classDays = if (day in classDays) classDays - day else classDays + day
                        },
                        onPickStart = { showClassStartPicker = true },
                        onPickEnd = { showClassEndPicker = true },
                        onWeeksChanged = { classWeeks = it },
                        onRemindersChanged = { reminders = it }
                    )
                } else if (mode == PlannerMode.SCHEDULE) {
                    SchedulePlannerContent(
                        selected = selectedTemplate,
                        onSelected = { selectedTemplate = it },
                        weeks = scheduleWeeks,
                        onWeeksChanged = { scheduleWeeks = it },
                        course = course,
                        onCourseChanged = { course = it },
                        reminders = reminders,
                        onRemindersChanged = { reminders = it }
                    )
                } else {
                    HabitPlannerContent(
                        plan = habitPlan,
                        onCourseChanged = { course = it },
                        onCueChanged = { startCue = it },
                        onToggleDay = { day ->
                            habitDays = if (day in habitDays) {
                                if (habitDays.size > 1) habitDays - day else habitDays
                            } else {
                                habitDays + day
                            }
                        },
                        onPickTime = { showTimePicker = true },
                        onLengthChanged = { habitLength = it },
                        onWeeksChanged = { habitWeeks = it },
                        onRemindersChanged = { reminders = it }
                    )
                }
            }

            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareContent = if (mode == PlannerMode.SCHEDULE) {
                                StudyPlanShareContent(
                                    title = "Horario ${selectedTemplate.label}",
                                    summary = StudyScheduleShareFormatter.weekly(
                                        selectedTemplate,
                                        scheduleWeeks,
                                        course,
                                        reminders
                                    ),
                                    code = StudyPlanCodeCodec.encodeWeekly(
                                        selectedTemplate,
                                        scheduleWeeks,
                                        course,
                                        reminders
                                    )
                                )
                            } else {
                                StudyPlanShareContent(
                                    title = "Hábito de estudio",
                                    summary = StudyScheduleShareFormatter.habit(habitPlan),
                                    code = StudyPlanCodeCodec.encodeHabit(habitPlan)
                                )
                            }
                        },
                        enabled = !isApplying,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Compartir")
                    }
                    Button(
                        onClick = {
                            when (mode) {
                                PlannerMode.SCHEDULE ->
                                    onApplySchedule(selectedTemplate, scheduleWeeks, course, reminders)
                                PlannerMode.HABIT -> onApplyHabit(habitPlan)
                                PlannerMode.CLASSES -> onApplyClasses(classPlan)
                            }
                        },
                        // Sin nombre de curso o sin días la clase no se puede crear.
                        enabled = !isApplying &&
                            (mode != PlannerMode.CLASSES ||
                                (classPlan.isValid && classCourse.isNotBlank())),
                        modifier = Modifier.weight(1.25f)
                    ) {
                        Text(
                            when {
                                isApplying -> "Creando…"
                                mode == PlannerMode.SCHEDULE -> "Aplicar horario"
                                mode == PlannerMode.CLASSES -> "Añadir clases"
                                else -> "Crear hábito"
                            }
                        )
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        PomodoroTimePickerDialog(
            initialHour = habitTime.hour,
            initialMinute = habitTime.minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                habitTime = LocalTime.of(hour, minute)
                showTimePicker = false
            }
        )
    }

    if (showClassStartPicker) {
        PomodoroTimePickerDialog(
            initialHour = classStart.hour,
            initialMinute = classStart.minute,
            onDismiss = { showClassStartPicker = false },
            onConfirm = { hour, minute ->
                classStart = LocalTime.of(hour, minute)
                showClassStartPicker = false
            }
        )
    }

    if (showClassEndPicker) {
        PomodoroTimePickerDialog(
            initialHour = classEnd.hour,
            initialMinute = classEnd.minute,
            onDismiss = { showClassEndPicker = false },
            onConfirm = { hour, minute ->
                classEnd = LocalTime.of(hour, minute)
                showClassEndPicker = false
            }
        )
    }

    if (showImport) {
        StudyPlanImportDialog(
            onDismiss = { showImport = false },
            onApplySchedule = { template, weeks, importedCourse, importedReminders ->
                showImport = false
                onApplySchedule(template, weeks, importedCourse, importedReminders)
            },
            onApplyHabit = { importedPlan ->
                showImport = false
                onApplyHabit(importedPlan)
            }
        )
    }

    shareContent?.let { content ->
        StudyPlanShareDialog(
            content = content,
            onDismiss = { shareContent = null }
        )
    }
}

@Composable
private fun PlannerModeCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SchedulePlannerContent(
    selected: WeeklyScheduleTemplate,
    onSelected: (WeeklyScheduleTemplate) -> Unit,
    weeks: Int,
    onWeeksChanged: (Int) -> Unit,
    course: String,
    onCourseChanged: (String) -> Unit,
    reminders: Boolean,
    onRemindersChanged: (Boolean) -> Unit
) {
    SectionTitle("Elige una estructura", "Puedes editar cada tarea después de aplicarla.")

    WeeklyScheduleTemplate.entries.forEach { template ->
        val isSelected = selected == template
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelected(template) },
            shape = RoundedCornerShape(18.dp),
            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = isSelected, onClick = { onSelected(template) })
                Column(Modifier.weight(1f)) {
                    Text(template.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${template.sessionsPerWeek} sesiones · ${durationText(template.focusMinutesPerWeek)} de enfoque por semana",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    ChoiceRow(
        title = "Duración del plan",
        options = listOf(1, 2, 4, 8),
        selected = weeks,
        label = { if (it == 1) "1 sem" else "$it sem" },
        onSelected = onWeeksChanged
    )

    OutlinedTextField(
        value = course,
        onValueChange = onCourseChanged,
        label = { Text("Curso o tema") },
        placeholder = { Text("Ej. Cálculo II") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    ReminderRow(reminders, onRemindersChanged)

    PlanSummary(
        title = "Tu plan",
        primary = "${selected.sessionsPerWeek * weeks} sesiones en $weeks ${if (weeks == 1) "semana" else "semanas"}",
        secondary = "${durationText(selected.focusMinutesPerWeek)} de enfoque por semana"
    )
}

/**
 * Alta del horario real de un curso: qué días, de qué hora a qué hora y cuántas semanas.
 *
 * A diferencia de las plantillas, aquí no se propone nada: el horario lo pone la
 * institución y el usuario solo lo transcribe.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ClassPlannerContent(
    plan: ClassSchedulePlan,
    onCourseChanged: (String) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onWeeksChanged: (Int) -> Unit,
    onRemindersChanged: (Boolean) -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.School, contentDescription = null)
            Column {
                Text(
                    "Tu horario de clases",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Copia el horario de tu universidad o colegio. Las clases salen en el " +
                        "calendario y puedes arrancarles el temporizador, pero no se mezclan " +
                        "con tus pendientes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    OutlinedTextField(
        value = plan.courseName,
        onValueChange = onCourseChanged,
        label = { Text("Nombre del curso") },
        placeholder = { Text("Ej: Anatomía") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Días de clase", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = day in plan.days,
                    onClick = { onToggleDay(day) },
                    label = { Text(day.shortSpanish()) }
                )
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onPickStart, modifier = Modifier.weight(1f)) {
            Text("Entra ${clockText(plan.startTime)}")
        }
        OutlinedButton(onClick = onPickEnd, modifier = Modifier.weight(1f)) {
            Text("Sale ${clockText(plan.endTime)}")
        }
    }

    if (plan.durationMinutes <= 0) {
        Text(
            "La hora de salida tiene que ser posterior a la de entrada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Duración del ciclo", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(4, 8, 12, 16, 20).forEach { weeks ->
                FilterChip(
                    selected = plan.weeks == weeks,
                    onClick = { onWeeksChanged(weeks) },
                    label = { Text("$weeks sem.") }
                )
            }
        }
    }

    if (plan.isValid) {
        Text(
            "Se crearán ${plan.totalSessions} clases · " +
                "${plan.minutesPerWeek / 60}h ${plan.minutesPerWeek % 60}min por semana",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Avisarme antes de cada clase", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = plan.withReminders, onCheckedChange = onRemindersChanged)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HabitPlannerContent(
    plan: StudyHabitPlan,
    onCourseChanged: (String) -> Unit,
    onCueChanged: (String) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onPickTime: () -> Unit,
    onLengthChanged: (HabitSessionLength) -> Unit,
    onWeeksChanged: (Int) -> Unit,
    onRemindersChanged: (Boolean) -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.TrackChanges, contentDescription = null)
            Column {
                Text("Haz fácil empezar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Repite una sesión manejable en días, hora y señal estables. Si fallas una vez, retoma la siguiente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }

    OutlinedTextField(
        value = plan.courseOrProject,
        onValueChange = onCourseChanged,
        label = { Text("¿Qué quieres estudiar?") },
        placeholder = { Text("Ej. Inglés") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = plan.startCue,
        onValueChange = onCueChanged,
        label = { Text("Señal de inicio (opcional)") },
        placeholder = { Text("Ej. Después de cenar") },
        supportingText = { Text("Relaciona el estudio con algo que ya haces.") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Días de estudio", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = day in plan.days,
                    onClick = { onToggleDay(day) },
                    label = { Text(day.shortSpanish()) }
                )
            }
        }
    }

    OutlinedButton(onClick = onPickTime, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text("Hora fija · ${clockText(plan.time)}")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Tamaño de la sesión", "Empieza con un bloque que puedas repetir incluso en días difíciles.")
        HabitSessionLength.entries.forEach { option ->
            val selected = plan.sessionLength == option
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLengthChanged(option) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected, onClick = { onLengthChanged(option) })
                    Column {
                        Text("${option.label} · ${option.focusMinutes} min", fontWeight = FontWeight.SemiBold)
                        Text(
                            option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    ChoiceRow(
        title = "Tiempo para consolidarlo",
        options = listOf(4, 8, 12),
        selected = plan.weeks,
        label = { "$it sem" },
        onSelected = onWeeksChanged
    )

    ReminderRow(plan.withReminders, onRemindersChanged)

    PlanSummary(
        title = "Tu hábito",
        primary = "${plan.sessions} sesiones · ${plan.days.size} por semana",
        secondary = "${durationText(plan.focusMinutesPerWeek)} de enfoque semanal"
    )

    Text(
        text = "Compromiso: ${plan.implementationIntention()}.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
}

@Composable
private fun ReminderRow(enabled: Boolean, onChanged: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Recordatorios", fontWeight = FontWeight.SemiBold)
                Text(
                    "Avisar al comenzar cada sesión",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onChanged)
        }
    }
}

@Composable
private fun PlanSummary(title: String, primary: String, secondary: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(secondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun DayOfWeek.shortSpanish(): String = when (this) {
    DayOfWeek.MONDAY -> "L"
    DayOfWeek.TUESDAY -> "M"
    DayOfWeek.WEDNESDAY -> "X"
    DayOfWeek.THURSDAY -> "J"
    DayOfWeek.FRIDAY -> "V"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "D"
}

private fun clockText(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

private fun durationText(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest min"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest min"
    }
}
