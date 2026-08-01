package com.example.pomodoro.features.timer.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * TimerRingGamerPremium - Componente premium estilo "Gamer" para el temporizador Pomodoro
 * 
 * Características:
 * - Track base con alpha bajo (fondo elegante)
 * - Progreso por segmentos (200) para degradado homogéneo sin banding
 * - Trail fade: cola tenue → punta intensa
 * - Glow arc siguiendo el progreso
 * - Neon head: highlight brillante en la punta
 * - Dot siempre visible con halo y pulso de urgencia
 * - Fade del arco cuando queda poco tiempo (dot permanece brillante)
 */
@Composable
fun TimerRingGamerPremium(
    progress: Float,
    remainingRatio: Float,
    mainColor: Color,
    modifier: Modifier = Modifier,
    strokeDp: Dp = 18.dp,
    showTicks: Boolean = false,
    // Contenido central
    timeText: String = "",
    modeText: String = "",
    pomodoroProgress: String? = null
) {
    // === Animaciones ===
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // Alpha global basado en tiempo restante (fade cuando queda poco)
    val globalAlpha = remember(remainingRatio) {
        when {
            remainingRatio <= 0.08f -> 0.45f + (remainingRatio / 0.08f) * 0.25f // 0.45 - 0.70
            remainingRatio <= 0.20f -> 0.70f + ((remainingRatio - 0.08f) / 0.12f) * 0.30f // 0.70 - 1.0
            else -> 1f
        }
    }

    // Glow boost cuando queda poco (sutil)
    val glowBoost = remember(remainingRatio) {
        if (remainingRatio <= 0.20f) 1f + (0.20f - remainingRatio) * 0.8f else 1f
    }

    // Pulso de urgencia para el dot
    val infiniteTransition = rememberInfiniteTransition(label = "urgency")
    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (remainingRatio <= 0.08f) 1.35f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val boxMaxWidth = maxWidth
        val strokePx = with(androidx.compose.ui.platform.LocalDensity.current) { strokeDp.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val radius = (canvasSize - strokePx * 4) / 2
            val center = Offset(size.width / 2, size.height / 2)
            val arcSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(center.x - radius, center.y - radius)

            // === 1. Track base (360° fondo) ===
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = radius,
                center = center,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // === 2. Ticks opcionales ===
            if (showTicks) {
                drawGamerTicks(center, radius, strokePx, mainColor, animatedProgress)
            }

            // === 3. Glow Arc (detrás del progreso) ===
            if (animatedProgress > 0.005f) {
                drawGlowArc(
                    center = center,
                    radius = radius,
                    stroke = strokePx,
                    progress = animatedProgress,
                    mainColor = mainColor,
                    globalAlpha = globalAlpha,
                    glowBoost = glowBoost
                )

                // === 4. Progreso principal por segmentos (trail fade) ===
                drawSegmentedProgress(
                    center = center,
                    radius = radius,
                    stroke = strokePx,
                    progress = animatedProgress,
                    mainColor = mainColor,
                    globalAlpha = globalAlpha
                )

                // === 5. Neon Head (highlight en la punta) ===
                drawNeonHead(
                    center = center,
                    radius = radius,
                    stroke = strokePx,
                    progress = animatedProgress,
                    mainColor = mainColor,
                    globalAlpha = globalAlpha
                )

                // === 6. Dot indicador (SIEMPRE brillante) ===
                drawGamerDot(
                    center = center,
                    radius = radius,
                    stroke = strokePx,
                    progress = animatedProgress,
                    mainColor = mainColor,
                    pulseScale = dotPulse,
                    isUrgent = remainingRatio <= 0.08f
                )
            }
        }

        // === 7. Contenido central ===
        if (timeText.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Tiempo mm:ss
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = when {
                            boxMaxWidth < 180.dp -> 38.sp
                            boxMaxWidth < 280.dp -> 52.sp
                            else -> 60.sp
                        },
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Modo
                if (modeText.isNotEmpty()) {
                    Text(
                        text = modeText.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = if (boxMaxWidth < 200.dp) 11.sp else 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp
                        ),
                        color = mainColor.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Pomodoro X/N
                if (pomodoroProgress != null) {
                    Text(
                        text = "🍅 $pomodoroProgress",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = if (boxMaxWidth < 200.dp) 11.sp else 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// FUNCIONES DE DIBUJO PRIVADAS
// ============================================================================

/**
 * Dibuja ticks estilo gamer (opcionales)
 */
private fun DrawScope.drawGamerTicks(
    center: Offset,
    radius: Float,
    stroke: Float,
    mainColor: Color,
    progress: Float
) {
    val tickLength = stroke * 0.4f
    for (i in 0 until 12) {
        val angleDeg = i * 30f - 90f
        val angleRad = Math.toRadians(angleDeg.toDouble())

        val outerR = radius + stroke * 0.2f
        val innerR = radius - tickLength

        val startX = center.x + (outerR * cos(angleRad)).toFloat()
        val startY = center.y + (outerR * sin(angleRad)).toFloat()
        val endX = center.x + (innerR * cos(angleRad)).toFloat()
        val endY = center.y + (innerR * sin(angleRad)).toFloat()

        val tickProgress = i / 12f
        val isPassed = tickProgress <= progress
        val isQuarter = i % 3 == 0

        drawLine(
            color = if (isPassed) mainColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (isQuarter) 3f else 1.5f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Dibuja el glow arc detrás del progreso
 * Mismo sweep que el progreso, pero más grueso y con alpha bajo
 */
private fun DrawScope.drawGlowArc(
    center: Offset,
    radius: Float,
    stroke: Float,
    progress: Float,
    mainColor: Color,
    globalAlpha: Float,
    glowBoost: Float
) {
    val sweepAngle = 360f * progress
    val glowStroke = stroke * 1.5f
    val glowAlpha = (0.22f * globalAlpha * glowBoost).coerceAtMost(0.35f)

    // Glow exterior difuso
    drawArc(
        color = mainColor.copy(alpha = glowAlpha * 0.6f),
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius - stroke * 0.4f, center.y - radius - stroke * 0.4f),
        size = Size((radius + stroke * 0.4f) * 2, (radius + stroke * 0.4f) * 2),
        style = Stroke(width = glowStroke * 1.8f, cap = StrokeCap.Round)
    )

    // Glow principal
    drawArc(
        color = mainColor.copy(alpha = glowAlpha),
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = glowStroke, cap = StrokeCap.Round)
    )
}

/**
 * Dibuja el progreso por segmentos con trail fade homogéneo
 * 200 segmentos para máxima suavidad sin banding
 * Cola: alpha ~0.15 | Punta: alpha ~0.95
 */
private fun DrawScope.drawSegmentedProgress(
    center: Offset,
    radius: Float,
    stroke: Float,
    progress: Float,
    mainColor: Color,
    globalAlpha: Float
) {
    val totalSweep = 360f * progress
    val segments = 200
    val sweepPerSegment = totalSweep / segments

    for (i in 0 until segments) {
        val t = i.toFloat() / (segments - 1) // 0 a 1

        // Curva de ease-in cuadrática para trail suave
        // Cola muy tenue → Punta muy intensa
        val alphaT = t.pow(2.2f) // Exponente para más contraste
        val segmentAlpha = (0.12f + alphaT * 0.83f) * globalAlpha

        val startAngle = -90f + (i * sweepPerSegment)

        drawArc(
            color = mainColor.copy(alpha = segmentAlpha.coerceIn(0f, 1f)),
            startAngle = startAngle,
            sweepAngle = sweepPerSegment + 0.6f, // Overlap para evitar gaps
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
    }

    // Cap redondeado al inicio (sutil)
    val startRad = Math.toRadians(-90.0)
    drawCircle(
        color = mainColor.copy(alpha = 0.12f * globalAlpha),
        radius = stroke / 2,
        center = Offset(
            center.x + (radius * cos(startRad)).toFloat(),
            center.y + (radius * sin(startRad)).toFloat()
        )
    )
}

/**
 * Dibuja el Neon Head - highlight brillante en la punta del arco
 * Da el efecto "gamer premium" con luz concentrada
 */
private fun DrawScope.drawNeonHead(
    center: Offset,
    radius: Float,
    stroke: Float,
    progress: Float,
    mainColor: Color,
    globalAlpha: Float
) {
    val totalSweep = 360f * progress
    if (totalSweep < 3f) return

    // === Head glow (últimos 15°) ===
    val headSweep = minOf(15f, totalSweep)
    val headStart = -90f + totalSweep - headSweep

    // Glow exterior del head
    drawArc(
        color = mainColor.copy(alpha = 0.18f * globalAlpha),
        startAngle = headStart,
        sweepAngle = headSweep,
        useCenter = false,
        topLeft = Offset(center.x - radius - stroke * 0.8f, center.y - radius - stroke * 0.8f),
        size = Size((radius + stroke * 0.8f) * 2, (radius + stroke * 0.8f) * 2),
        style = Stroke(width = stroke * 2.2f, cap = StrokeCap.Round)
    )

    // Head principal intenso
    drawArc(
        color = mainColor.copy(alpha = 0.95f * globalAlpha),
        startAngle = headStart,
        sweepAngle = headSweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )

    // === Neon highlight (últimos 5°) - luz blanca ===
    val neonSweep = minOf(5f, totalSweep)
    val neonStart = -90f + totalSweep - neonSweep

    // Highlight blanco brillante
    drawArc(
        color = Color.White.copy(alpha = 0.4f * globalAlpha),
        startAngle = neonStart,
        sweepAngle = neonSweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke * 0.55f, cap = StrokeCap.Round)
    )

    // Línea central súper brillante
    drawArc(
        color = Color.White.copy(alpha = 0.7f * globalAlpha),
        startAngle = neonStart + neonSweep * 0.4f,
        sweepAngle = neonSweep * 0.4f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke * 0.2f, cap = StrokeCap.Round)
    )
}

/**
 * Dibuja el dot indicador en la punta - SIEMPRE visible y brillante
 * Con halo, borde de contraste, y pulso de urgencia
 */
private fun DrawScope.drawGamerDot(
    center: Offset,
    radius: Float,
    stroke: Float,
    progress: Float,
    mainColor: Color,
    pulseScale: Float,
    isUrgent: Boolean
) {
    val angleRad = Math.toRadians(-90.0 + 360.0 * progress)
    val dotX = center.x + (radius * cos(angleRad)).toFloat()
    val dotY = center.y + (radius * sin(angleRad)).toFloat()
    val dotCenter = Offset(dotX, dotY)

    val baseRadius = stroke * 0.6f
    val scale = if (isUrgent) pulseScale else 1f

    // 1. Halo exterior grande (glow difuso) - SIEMPRE alpha completo
    drawCircle(
        color = mainColor.copy(alpha = 0.25f),
        radius = baseRadius * 2.5f * scale,
        center = dotCenter
    )

    // 2. Halo medio
    drawCircle(
        color = mainColor.copy(alpha = 0.4f),
        radius = baseRadius * 1.7f * scale,
        center = dotCenter
    )

    // 3. Círculo principal de color
    drawCircle(
        color = mainColor,
        radius = baseRadius,
        center = dotCenter
    )

    // 4. Borde blanco para contraste
    drawCircle(
        color = Color.White.copy(alpha = 0.75f),
        radius = baseRadius,
        center = dotCenter,
        style = Stroke(width = 2.5f)
    )

    // 5. Núcleo brillante
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = baseRadius * 0.4f,
        center = dotCenter
    )
}

// ============================================================================
// VERSIÓN SIMPLIFICADA PARA COMPATIBILIDAD CON HomeScreen
// ============================================================================

/**
 * Wrapper que calcula remainingRatio automáticamente
 */
@Composable
fun TimerRingGamerPremium(
    progress: Float,
    timeText: String,
    modeText: String,
    mainColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 18.dp,
    glowEnabled: Boolean = true,
    ticksEnabled: Boolean = false,
    pomodoroProgress: String? = null,
    taskProgressPercent: Float? = null
) {
    val remainingRatio = (1f - progress).coerceIn(0f, 1f)

    TimerRingGamerPremium(
        progress = progress,
        remainingRatio = remainingRatio,
        mainColor = mainColor,
        modifier = modifier,
        strokeDp = strokeWidth,
        showTicks = ticksEnabled,
        timeText = timeText,
        modeText = modeText,
        pomodoroProgress = pomodoroProgress
    )
}
