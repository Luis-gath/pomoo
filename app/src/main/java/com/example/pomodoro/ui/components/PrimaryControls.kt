package com.example.pomodoro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryControls(
    isRunning: Boolean,
    onToggleClick: () -> Unit,
    onResetClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset
        FilledIconButton(
            onClick = onResetClick,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = MaterialTheme.colorScheme.onSurface)
        }

        // Play/Pause
        FilledIconButton(
            onClick = onToggleClick,
            modifier = Modifier.size(80.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            val icon = if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            // Using system drawable for simplicity or could use Icons.Default.Pause
             Icon(
                 painter = painterResource(id = icon),
                 contentDescription = if (isRunning) "Pausar" else "Iniciar",
                 tint = MaterialTheme.colorScheme.onPrimary,
                 modifier = Modifier.size(40.dp)
             )
        }

        // Next
        FilledIconButton(
            onClick = onNextClick,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
