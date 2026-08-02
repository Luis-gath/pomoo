package com.example.pomodoro.features.timer.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro.features.timer.domain.PomodoroMode
import com.example.pomodoro.shared.ui.theme.BackdropPanel
import com.example.pomodoro.shared.ui.theme.OnBackdrop
import com.example.pomodoro.shared.ui.theme.OnBackdropMuted
@Composable
fun ActiveTaskHeaderLabel() {
    Text(
        text = "TAREA ACTIVA",
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        ),
        color = OnBackdropMuted,
        modifier = Modifier.padding(bottom = 8.dp)
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
    if (!state.hasActiveTask) {
        EmptyTaskCard(onSelectTask, modifier)
    } else {
        RunningTaskCard(
            state = state,
            onOpenTask = onOpenTask,
            onSelectTask = onSelectTask,
            onClearTask = onClearTask,
            modifier = modifier
        )
    }
}

@Composable
private fun EmptyTaskCard(
    onSelectTask: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectTask),
        shape = RoundedCornerShape(16.dp),
        color = BackdropPanel,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sin tarea activa",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = OnBackdrop
                )
                Text(
                    text = "Elige una tarea para enfocarte",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackdropMuted
                )
            }
            
            Button(
                onClick = onSelectTask,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Elegir", style = MaterialTheme.typography.labelMedium)
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
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val borderColor = if (state.isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
    val shadowElevation = if (state.isRunning) 8.dp else 2.dp
    
    // Animation
    val containerColor = BackdropPanel
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(shadowElevation, RoundedCornerShape(16.dp))
            .animateContentSize(), // Smooth expansion
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        onClick = { expanded = !expanded } // Toggle expansion
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ALWAYS VISIBLE: Title + Pulse + Mini Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Title Area
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                     // Active Indicator (Dot) with Pulse
                    if (state.isRunning) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Show mini progress or arrow when collapsed?
                if (!expanded) {
                     Text(
                        text = "${state.completedPomodoros}/${state.totalPomodoros}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnBackdropMuted
                    )
                }
            }
            
            // EXPANDED CONTENT
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                
                if (!state.course.isNullOrBlank()) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(state.course, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp),
                        enabled = false
                    )
                    Spacer(Modifier.height(8.dp))
                }
                
                // Status Text
                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (state.etaString != null) {
                    Text(
                        text = state.etaString,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnBackdropMuted
                    )
                }
                
                Spacer(Modifier.height(12.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(Modifier.height(12.dp))
                
                // ACTIONS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit/Open Task
                     TextButton(
                        onClick = onOpenTask,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp)) // Or Edit icon
                        Spacer(Modifier.width(4.dp))
                        Text("Ver Detalle", style = MaterialTheme.typography.labelSmall)
                    }

                    Row {
                        TextButton(
                            onClick = onSelectTask,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cambiar", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        TextButton(
                            onClick = onClearTask,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Quitar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                 // Collapsed Progress Line at bottom (Optional, for visual feedback)
                 Spacer(Modifier.height(8.dp))
                 LinearProgressIndicator(
                    progress = state.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
                    color = MaterialTheme.colorScheme.primary.copy(alpha=0.8f)
                )
            }
        }
    }
}
