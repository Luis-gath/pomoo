package com.example.pomodoro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tarjeta visual de progreso de tarea activa
 * Muestra el nombre de la tarea, progreso visual con círculos de pomodoros,
 * y barra de progreso animada.
 */
@Composable
fun TaskProgressCard(
    taskTitle: String,
    currentPomodoro: Int,
    totalPomodoros: Int,
    progressPercent: Float,
    accentColor: Color,
    hasAudio: Boolean = false,
    isAudioPlaying: Boolean = false,
    onToggleAudio: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Título y botón cerrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icono con gradiente
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(accentColor, accentColor.copy(alpha = 0.6f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "TAREA ACTIVA",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            taskTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Row {
                    // Botón de audio si existe
                    if (hasAudio) {
                        IconButton(
                            onClick = onToggleAudio,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isAudioPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                contentDescription = "Audio",
                                tint = if (isAudioPlaying) accentColor else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Botón cerrar
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar tarea",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Círculos de pomodoros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPomodoros) { index ->
                    val isCompleted = index < currentPomodoro
                    val isCurrent = index == currentPomodoro
                    
                    PomodoroCircle(
                        isCompleted = isCompleted,
                        isCurrent = isCurrent,
                        accentColor = accentColor,
                        index = index + 1
                    )
                    
                    if (index < totalPomodoros - 1) {
                        // Línea conectora entre círculos
                        Box(
                            modifier = Modifier
                                .width(if (totalPomodoros > 6) 8.dp else 16.dp)
                                .height(3.dp)
                                .background(
                                    if (isCompleted) accentColor.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Barra de progreso con texto
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Progreso",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "${(animatedProgress * 100).toInt()}% completado",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Barra de progreso estilizada
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Texto motivacional
            Text(
                text = when {
                    currentPomodoro == 0 -> "¡Comienza tu primer pomodoro! 🚀"
                    currentPomodoro < totalPomodoros -> "¡Vas muy bien! Quedan ${totalPomodoros - currentPomodoro} pomodoros 💪"
                    else -> "¡Excelente trabajo! 🎉"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PomodoroCircle(
    isCompleted: Boolean,
    isCurrent: Boolean,
    accentColor: Color,
    index: Int
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> accentColor
            isCurrent -> accentColor.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isCompleted -> accentColor
            isCurrent -> accentColor
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(300),
        label = "borderColor"
    )

    val size = if (isCurrent) 36.dp else 28.dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = if (isCurrent) 3.dp else 2.dp,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Completado",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        } else if (isCurrent) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        } else {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
