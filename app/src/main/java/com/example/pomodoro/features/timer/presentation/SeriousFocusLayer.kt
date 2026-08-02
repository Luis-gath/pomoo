package com.example.pomodoro.features.timer.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro.shared.ui.theme.OnBackdropMuted
import kotlinx.coroutines.delay

/**
 * Capa a pantalla completa del modo enfoque serio.
 *
 * Solo se ve el temporizador. Los controles no están a la vista: aparecen al tocar la
 * pantalla y se esconden solos a los pocos segundos. La fricción es intencionada — pausar
 * debe requerir una decisión, no un gesto reflejo.
 */
@Composable
fun SeriousFocusLayer(
    remaining: Float,
    timeText: String,
    modeText: String,
    accent: Color,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(4000)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.82f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TimerGauge(
                remaining = remaining,
                timeText = timeText,
                modeText = modeText,
                accent = accent,
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
        ) {
            TextButton(onClick = onPause) {
                Icon(Icons.Default.Pause, contentDescription = null, tint = OnBackdropMuted)
                Text("  Pausar", color = OnBackdropMuted)
            }
        }

        AnimatedVisibility(
            visible = !controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        ) {
            Text(
                text = "Toca para mostrar los controles",
                style = MaterialTheme.typography.labelSmall,
                color = OnBackdropMuted.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}
