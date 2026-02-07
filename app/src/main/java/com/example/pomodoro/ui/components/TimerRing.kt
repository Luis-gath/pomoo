package com.example.pomodoro.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
fun TimerRing(
    progress: Float,
    timeText: String,
    mainColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
    glowEnabled: Boolean = true
) {
    // 3. Smooth animation with easing
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ProgressAnimation"
    )

    // Using BoxWithConstraints for responsiveness
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f), // Keeps the ring circular
        contentAlignment = Alignment.Center
    ) {
        // Calculate dynamic sizes based on the container constraints
        val diameter = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val stroke = with(androidx.compose.ui.platform.LocalDensity.current) { strokeWidth.toPx() }
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val radius = (canvasSize - stroke) / 2
            val center = Offset(size.width / 2, size.height / 2)
            val arcSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(center.x - radius, center.y - radius)

            // 4. Glow Effect (Layered drawing behind)
            if (glowEnabled) {
                // Outer soft glow
                drawArc(
                    color = mainColor.copy(alpha = 0.1f),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 3f, cap = StrokeCap.Round)
                )
                // Inner brighter glow
                drawArc(
                    color = mainColor.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke * 1.5f, cap = StrokeCap.Round)
                )
            }

            // 1. Background Track (Complete circle)
            drawCircle(
                color = mainColor.copy(alpha = 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )

            // 2. Progress Arc with StrokeCap.Round
            // Drawing the progress
            drawArc(
                color = mainColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        // Timer Text
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = if (maxWidth < 200.dp) 40.sp else 64.sp, // Responsive font size
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}
