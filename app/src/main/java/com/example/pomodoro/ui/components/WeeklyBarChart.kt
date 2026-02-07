package com.example.pomodoro.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BarData(
    val label: String, // e.g. "L", "M"
    val value: Int,    // 5
    val maxValue: Int  // 10 (scale max)
)

@Composable
fun WeeklyBarChart(
    data: List<BarData>,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        if (data.isEmpty()) {
            Text(
                "Sin datos suficientes",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Box
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { item ->
                BarItem(
                    data = item,
                    color = primaryColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun BarItem(
    data: BarData,
    color: Color,
    modifier: Modifier
) {
    val animatedHeightFactor by animateFloatAsState(
        targetValue = if (data.maxValue > 0) (data.value.toFloat() / data.maxValue).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(1000),
        label = "barHeight"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Value Text (Show only if > 0)
        if (data.value > 0) {
            Text(
                text = "${data.value}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Bar Logic
        Box(
            modifier = Modifier
                .width(12.dp)
                .weight(1f, fill = false) // Don't fill, use weight for distribution only
                .fillMaxHeight() // But we want column to dictate height? No.
        ) {
            // We use a Canvas or just Box with fraction height? 
            // Box with fillMaxHeight(fraction) is easier
            
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            )
            
            // Animated Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedHeightFactor)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(color, color.copy(alpha = 0.7f))
                        ),
                        RoundedCornerShape(6.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label (Day)
        Text(
            text = data.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
