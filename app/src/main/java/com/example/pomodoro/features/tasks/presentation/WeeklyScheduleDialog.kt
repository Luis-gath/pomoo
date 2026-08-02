package com.example.pomodoro.features.tasks.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pomodoro.features.tasks.domain.WeeklyScheduleTemplate

/**
 * Elegir una rutina semanal y crear sus sesiones.
 *
 * Muestra por adelantado cuántas sesiones se van a crear, porque aplicar una rutina de
 * exámenes durante cuatro semanas genera casi cincuenta tareas: conviene saberlo antes de
 * pulsar y no después.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleDialog(
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onApply: (WeeklyScheduleTemplate, Int, String, Boolean) -> Unit
) {
    var selected by remember { mutableStateOf(WeeklyScheduleTemplate.INGENIERIA) }
    var weeks by remember { mutableIntStateOf(2) }
    var course by remember { mutableStateOf("") }
    var reminders by remember { mutableStateOf(true) }

    val totalSessions = selected.sessionsPerWeek * weeks

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aplicar un horario") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WeeklyScheduleTemplate.entries.forEach { template ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == template,
                                onClick = { selected = template }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == template,
                            onClick = { selected = template }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(template.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                template.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${template.sessionsPerWeek} sesiones · " +
                                    "${template.minutesPerWeek / 60} h ${template.minutesPerWeek % 60} min por semana",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("¿Cuántas semanas?", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 4, 8).forEach { option ->
                        FilterChip(
                            selected = weeks == option,
                            onClick = { weeks = option },
                            label = { Text("$option") }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Curso o tema (opcional)") },
                    placeholder = { Text("Ej: Cálculo II") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Avisarme antes de cada sesión")
                    Switch(checked = reminders, onCheckedChange = { reminders = it })
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Se crearán $totalSessions tareas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Son tareas normales: puedes editarlas o borrarlas una a una. Al agotarse " +
                        "las semanas habrá que volver a aplicar el horario.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(selected, weeks, course, reminders) },
                enabled = !isApplying
            ) {
                Text(if (isApplying) "Creando…" else "Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
