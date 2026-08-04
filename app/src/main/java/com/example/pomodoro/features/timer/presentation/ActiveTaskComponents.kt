package com.example.pomodoro.features.timer.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro.shared.ui.theme.BackdropPanel
import com.example.pomodoro.shared.ui.theme.OnBackdrop
import com.example.pomodoro.shared.ui.theme.OnBackdropMuted

@Composable
fun ActiveTaskHeaderLabel(modifier: Modifier = Modifier) {
    Text(
        text = "TAREA ACTIVA",
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Bold
        ),
        color = OnBackdropMuted,
        modifier = modifier.padding(start = 2.dp, bottom = 7.dp)
    )
}

@Composable
fun ActiveTaskCard(
    state: ActiveTaskUiState,
    onSelectTask: () -> Unit,
    onOpenTask: () -> Unit,
    onClearTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.hasActiveTask) {
        RunningTaskCard(
            state = state,
            onOpenTask = onOpenTask,
            onSelectTask = onSelectTask,
            onClearTask = onClearTask,
            modifier = modifier
        )
    } else {
        EmptyTaskCard(onSelectTask = onSelectTask, modifier = modifier)
    }
}

@Composable
private fun EmptyTaskCard(
    onSelectTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onSelectTask,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BackdropPanel,
        border = BorderStroke(1.dp, OnBackdrop.copy(alpha = 0.11f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(OnBackdrop.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddTask,
                    contentDescription = null,
                    tint = OnBackdropMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sin tarea activa",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnBackdrop
                )
                Text(
                    text = "Elige qué quieres avanzar ahora",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackdropMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = onSelectTask,
                modifier = Modifier.height(38.dp),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Text("Elegir")
            }
        }
    }
}

@Composable
private fun RunningTaskCard(
    state: ActiveTaskUiState,
    onOpenTask: () -> Unit,
    onSelectTask: () -> Unit,
    onClearTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    val status = if (state.course.isNullOrBlank()) {
        state.statusText
    } else {
        "${state.course} · ${state.statusText}"
    }

    Surface(
        onClick = onOpenTask,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BackdropPanel,
        border = BorderStroke(
            width = 1.dp,
            color = if (state.isRunning) accent.copy(alpha = 0.46f)
            else OnBackdrop.copy(alpha = 0.10f)
        )
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (state.isRunning) accent else OnBackdropMuted,
                            shape = CircleShape
                        )
                )

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = OnBackdrop,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnBackdropMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (state.isRunning) accent.copy(alpha = 0.18f)
                    else OnBackdrop.copy(alpha = 0.07f)
                ) {
                    Text(
                        text = "${state.completedPomodoros}/${state.totalPomodoros}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isRunning) accent else OnBackdropMuted,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones de la tarea activa",
                            tint = OnBackdropMuted
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ver detalle") },
                            leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenTask()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cambiar tarea") },
                            leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSelectTask()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Quitar tarea", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onClearTask()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(9.dp))

            LinearProgressIndicator(
                progress = state.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 14.dp)
                    .height(3.dp)
                    .clip(CircleShape),
                color = accent,
                trackColor = OnBackdrop.copy(alpha = 0.10f)
            )
        }
    }
}
