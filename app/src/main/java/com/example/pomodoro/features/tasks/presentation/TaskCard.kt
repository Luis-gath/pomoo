package com.example.pomodoro.features.tasks.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pomodoro.features.tasks.data.TaskEntity
import com.example.pomodoro.features.tasks.data.TaskPriority
import com.example.pomodoro.features.tasks.data.TaskStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tarjeta de tarea premium con progreso Pomodoro integrado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: TaskEntity,
    onStartClick: () -> Unit,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveStatus: ((TaskStatus) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showMoveMenu by remember { mutableStateOf(false) }
    
    val animatedProgress by animateFloatAsState(
        targetValue = task.pomodoroProgress,
        animationSpec = tween(600),
        label = "progress"
    )
    
    val cardColor by animateColorAsState(
        targetValue = when (task.status) {
            TaskStatus.DONE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            TaskStatus.DOING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            TaskStatus.TODO -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "cardColor"
    )
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // --- Header: Título + Curso + Menú ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Título
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (task.isDone) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Curso/Proyecto
                    if (task.courseOrProject.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = task.courseOrProject,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Menú de opciones
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("✏️ Editar") },
                            onClick = { showMenu = false; onEditClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("📋 Duplicar") },
                            onClick = { showMenu = false; onDuplicateClick() }
                        )
                        
                        // Opción Mover a... (solo si onMoveStatus está disponible)
                        if (onMoveStatus != null) {
                            DropdownMenuItem(
                                text = { Text("➡️ Mover a...") },
                                onClick = { 
                                    showMenu = false
                                    showMoveMenu = true
                                }
                            )
                        }
                        
                        Divider()
                        DropdownMenuItem(
                            text = { Text("🗑️ Eliminar", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDeleteClick() }
                        )
                    }
                    
                    // Submenú Mover a
                    DropdownMenu(
                        expanded = showMoveMenu,
                        onDismissRequest = { showMoveMenu = false }
                    ) {
                        TaskStatus.entries.forEach { status ->
                            if (status != task.status) {
                                DropdownMenuItem(
                                    text = { 
                                        val label = when(status) {
                                            TaskStatus.TODO -> "Pendiente"
                                            TaskStatus.DOING -> "En curso"
                                            TaskStatus.DONE -> "Hecho"
                                        }
                                        Text(label) 
                                    },
                                    onClick = {
                                        showMoveMenu = false
                                        onMoveStatus?.invoke(status)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // --- Chips: Prioridad + Estado + Fecha ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip de prioridad
                PriorityChip(priority = task.priority)
                
                // Chip de estado
                StatusChip(status = task.status)
                
                Spacer(Modifier.weight(1f))
                
                // Fecha
                task.dueDateTime?.let { dueDate ->
                    Text(
                        text = formatDate(dueDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dueDate < System.currentTimeMillis() && !task.isDone)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // --- Progreso Pomodoro ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono y contador
                Text("🍅", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${task.completedPomodoros}/${task.totalPomodoros}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(Modifier.width(12.dp))
                
                // Barra de progreso
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        task.isDone -> MaterialTheme.colorScheme.tertiary
                        task.pomodoroProgress >= 0.5f -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(Modifier.width(12.dp))
                
                // Botón de iniciar/continuar
                if (!task.isDone) {
                    FilledTonalButton(
                        onClick = onStartClick,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (task.completedPomodoros > 0) "Continuar" else "Iniciar",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    // Badge de completado
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Completada",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityChip(priority: TaskPriority) {
    val (color, label) = when (priority) {
        TaskPriority.HIGH -> Pair(Color(0xFFEF5350), "Alta")
        TaskPriority.MEDIUM -> Pair(Color(0xFFFFB74D), "Media")
        TaskPriority.LOW -> Pair(Color(0xFF66BB6A), "Baja")
    }
    
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
fun StatusChip(status: TaskStatus) {
    val (color, label, icon) = when (status) {
        TaskStatus.TODO -> Triple(
            MaterialTheme.colorScheme.outline,
            "Pendiente",
            Icons.Default.AccessTime
        )
        TaskStatus.DOING -> Triple(
            MaterialTheme.colorScheme.primary,
            "En curso",
            Icons.Default.PlayCircle
        )
        TaskStatus.DONE -> Triple(
            MaterialTheme.colorScheme.tertiary,
            "Hecho",
            Icons.Default.CheckCircle
        )
    }
    
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = timestamp - now
    val oneDay = 24 * 60 * 60 * 1000L
    
    return when {
        diff < 0 -> "Vencido"
        diff < oneDay -> "Hoy"
        diff < 2 * oneDay -> "Mañana"
        else -> SimpleDateFormat("dd MMM", Locale("es")).format(Date(timestamp))
    }
}
