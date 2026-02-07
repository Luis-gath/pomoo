package com.example.pomodoro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Stepper para seleccionar cantidad de Pomodoros (1-12)
 */
@Composable
fun PomodoroStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minValue: Int = 1,
    maxValue: Int = 12,
    label: String = "Objetivo"
) {
    val actualValue = value.coerceIn(minValue, maxValue)
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono y label
            Text("🍅", style = MaterialTheme.typography.headlineMedium)
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "$actualValue Pomodoro${if (actualValue > 1) "s" else ""}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Controles +/-
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón -
                FilledIconButton(
                    onClick = { if (actualValue > minValue) onValueChange(actualValue - 1) },
                    enabled = actualValue > minValue,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Disminuir")
                }
                
                // Valor central prominente
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = actualValue.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Botón +
                FilledIconButton(
                    onClick = { if (actualValue < maxValue) onValueChange(actualValue + 1) },
                    enabled = actualValue < maxValue,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar")
                }
            }
        }
    }
}

/**
 * Stepper compacto para ajustes avanzados
 */
@Composable
fun CompactStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    suffix: String = "min",
    minValue: Int = 1,
    maxValue: Int = 90,
    modifier: Modifier = Modifier
) {
    val actualValue = value.coerceIn(minValue, maxValue)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { if (actualValue > minValue) onValueChange(actualValue - 1) },
                enabled = actualValue > minValue,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Remove, 
                    contentDescription = "Disminuir",
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Text(
                text = "$actualValue $suffix",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(min = 50.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            IconButton(
                onClick = { if (actualValue < maxValue) onValueChange(actualValue + 1) },
                enabled = actualValue < maxValue,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Aumentar",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
