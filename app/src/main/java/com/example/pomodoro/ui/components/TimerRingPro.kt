package com.example.pomodoro.ui.components

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import kotlin.math.sin

/**
 * TimerRingPro - Componente premium del temporizador Pomodoro
 * 
 * Características:
 * - Track circular limpio (360° con alpha bajo)
 * - Degradado suave en el arco de progreso (cola tenue → cabeza brillante)
 * - Fade automático cuando queda poco tiempo
 * - Dot indicador siempre visible con glow
 * - Head glow arc en la punta
 * - Animaciones suaves y profesionales
 */
@Composable
fun TimerRingPro(
    progress: Float,
    remainingRatio: Float,
    timeText: String,
    modeText: String,
    mainColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    ticksEnabled: Boolean = true,
    pomodoroProgress: String? = null
) {
    // Animación suave del progreso
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "ProgressAnimation"
    )

    // Calcular alpha global basado en remainingRatio
    val globalAlphaMultiplier = when {
        remainingRatio <= 0.08f -> 0.4f + (remainingRatio / 0.08f) * 0.6f // 0.4 a 1.0
        remainingRatio <= 0.20f -> 0.7f + ((remainingRatio - 0.08f) / 0.12f) * 0.3f // 0.7 a 1.0
        else -> 1f
    }

    // Pulso de urgencia cuando queda muy poco tiempo
    val infiniteTransition = rememberInfiniteTransition(label = "urgency")
    val urgencyPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (remainingRatio <= 0.08f) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "urgencyPulse"
    )

    // Animación de escala al cambiar minuto
    val currentMinute = remember(timeText) { 
        timeText.split(":").firstOrNull()?.toIntOrNull() ?: 0 
    }
    var previousMinute by remember { mutableIntStateOf(currentMinute) }
    var triggerScale by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentMinute) {
        if (currentMinute != previousMinute) {
            triggerScale = true
            previousMinute = currentMinute
        }
    }
    
    val scaleAnimation by animateFloatAsState(
        targetValue = if (triggerScale) 1.06f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        finishedListener = { triggerScale = false },
        label = "ScaleAnimation"
    )

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val boxMaxWidth = maxWidth
        val stroke = with(androidx.compose.ui.platform.LocalDensity.current) { strokeWidth.toPx() }
        val tickLength = stroke * 0.5f
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val radius = (canvasSize - stroke * 4) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // ===== 1. Track de fondo (360° limpio) =====
            drawCircle(
                color = mainColor.copy(alpha = 0.12f),
                radius = radius,
                center = center,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // ===== 2. Ticks sutiles =====
            if (ticksEnabled) {
                drawTicks(
                    center = center,
                    radius = radius,
                    stroke = stroke,
                    tickLength = tickLength,
                    mainColor = mainColor,
                    animatedProgress = animatedProgress
                )
            }

            // ===== 3. Arco de progreso con degradado suave =====
            if (animatedProgress > 0.001f) {
                drawGradientArc(
                    center = center,
                    radius = radius,
                    stroke = stroke,
                    progress = animatedProgress,
                    mainColor = mainColor,
                    globalAlpha = globalAlphaMultiplier
                )

                // ===== 4. Head Glow Arc (brillo extra en la punta) =====
                drawHeadGlow(
                    center = center,
                    radius = radius,
                    stroke = stroke,
                    progress = animatedProgress,
                    mainColor = mainColor,
                    globalAlpha = globalAlphaMultiplier
                )

                // ===== 5. Dot indicador siempre visible =====
                drawDotIndicator(
                    center = center,
                    radius = radius,
                    stroke = stroke,
                    progress = animatedProgress,
                    mainColor = mainColor,
                    pulseScale = urgencyPulse,
                    isUrgent = remainingRatio <= 0.08f
                )
            }
        }

        // ===== 6. Contenido central =====
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = when {
                        boxMaxWidth < 200.dp -> 40.sp
                        boxMaxWidth < 300.dp -> 54.sp
                        else -> 62.sp
                    },
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.scale(scaleAnimation)
            )
            
            Text(
                text = modeText.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = if (boxMaxWidth < 200.dp) 11.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                ),
                color = mainColor.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp)
            )
            
            if (pomodoroProgress != null) {
                Text(
                    text = "🍅 $pomodoroProgress",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = if (boxMaxWidth < 200.dp) 12.sp else 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/**
 * Dibuja los ticks alrededor del círculo
 */
private fun DrawScope.drawTicks(
    center: Offset,
    radius: Float,
    stroke: Float,
    tickLength: Float,
    mainColor: Color,
    animatedProgress: Float
) {
    for (i in 0 until 12) {
        val angleDeg = i * 30f - 90f
        val angleRad = Math.toRadians(angleDeg.toDouble())
        
        val outerRadius = radius + stroke * 0.3f
        val innerRadius = radius - tickLength
        
        val startX = center.x + (outerRadius * cos(angleRad)).toFloat()
        val startY = center.y + (outerRadius * sin(angleRad)).toFloat()
        val endX = center.x + (innerRadius * cos(angleRad)).toFloat()
        val endY = center.y + (innerRadius * sin(angleRad)).toFloat()
        
        // Los ticks en cuartos son más gruesos
        val isQuarter = i % 3 == 0
        val tickProgress = i / 12f
        val isPassed = tickProgress <= animatedProgress
        
        drawLine(
            color = mainColor.copy(alpha = if (isPassed) 0.5f else 0.2f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (isQuarter) 3f else 1.5f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Dibuja el arco de progreso con trail fade:
 * - Inicio: muy transparente (alpha ~0.1)
 * - Final: intenso y definido (alpha ~1.0)
 * - Curva cúbica para transición ultra suave
 */
private fun DrawScope.drawGradientArc(
    center: Offset,
    radius: Float,
    stroke: Float,
    progress: Float,
    mainColor: Color,
    globalAlpha: Float
) {
    val totalSweep = 360f * progress
    val segments = 150 // Más segmentos para trail más suave
    val sweepPerSegment = totalSweep / segments
    
    for (i in 0 until segments) {
        val segmentProgress = i.toFloat() / segments // 0 a ~1 dentro del arco
        
        // Trail fade: curva cúbica para transición más pronunciada
        // Inicio muy transparente → final intenso
        val t = segmentProgress
        val alphaProgress = t * t * t // Cúbica: inicio muy tenue
        
        // Alpha: 0.08 (casi invisible) → 0.95 (intenso)
        val segmentAlpha = (0.08f + alphaProgress * 0.87f) * globalAlpha
        
        val startAngle = -90f + (i * sweepPerSegment)
        
        drawArc(
            color = mainColor.copy(alpha = segmentAlpha.coerceIn(0f, 1f)),
            startAngle = startAngle,
            sweepAngle = sweepPerSegment + 0.5f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
    }
    
    // Cap inicio (muy tenue, casi invisible)
    val startAngleRad = Math.toRadians(-90.0)
    drawCircle(
        color = mainColor.copy(alpha = 0.08f * globalAlpha),
        radius = stroke / 2,
        center = Offset(
            center.x + (radius * cos(startAngleRad)).toFloat(),
            center.y + (radius * sin(startAngleRad)).toFloat()
        )
    )
}

/**
 * Dibuja el head glow arc + head highlight para efecto 3D suave
 * - Glow difuso exterior
 * - Highlight brillante en la punta (últimos 8°)
 * - Efecto de luz que ilumina el final del arco
 */
private fun DrawScope.drawHeadGlow(
    center: Offset,
    radius: Float,
    stroke: Float,
    progress: Float,
    mainColor: Color,
    globalAlpha: Float
) {
    val totalSweep = 360f * progress
    if (totalSweep < 2f) return
    
    // === Head Glow (últimos 20°) ===
    val glowSweep = minOf(20f, totalSweep)
    val glowStart = -90f + totalSweep - glowSweep
    
    // Glow exterior muy difuso (aureola)
    drawArc(
        color = mainColor.copy(alpha = 0.12f * globalAlpha),
        startAngle = glowStart,
        sweepAngle = glowSweep,
        useCenter = false,
        topLeft = Offset(center.x - radius - stroke * 1.5f, center.y - radius - stroke * 1.5f),
        size = Size((radius + stroke * 1.5f) * 2, (radius + stroke * 1.5f) * 2),
        style = Stroke(width = stroke * 3f, cap = StrokeCap.Round)
    )
    
    // Glow medio
    drawArc(
        color = mainColor.copy(alpha = 0.25f * globalAlpha),
        startAngle = glowStart,
        sweepAngle = glowSweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke * 1.6f, cap = StrokeCap.Round)
    )
    
    // Arco principal intenso
    drawArc(
        color = mainColor.copy(alpha = 0.92f * globalAlpha),
        startAngle = glowStart,
        sweepAngle = glowSweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    
    // === Head Highlight (efecto 3D - últimos 6°) ===
    val highlightSweep = minOf(6f, totalSweep)
    val highlightStart = -90f + totalSweep - highlightSweep
    
    // Highlight blanco superior (simula luz)
    drawArc(
        color = Color.White.copy(alpha = 0.35f * globalAlpha),
        startAngle = highlightStart,
        sweepAngle = highlightSweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke * 0.5f, cap = StrokeCap.Round)
    )
    
    // Línea de brillo especular en el centro
    drawArc(
        color = Color.White.copy(alpha = 0.6f * globalAlpha),
        startAngle = highlightStart + highlightSweep * 0.3f,
        sweepAngle = highlightSweep * 0.5f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke * 0.25f, cap = StrokeCap.Round)
    )
}

/**
 * Dibuja el dot indicador en la punta del arco
 * Siempre visible con glow, independiente del alpha del arco
 */
private fun DrawScope.drawDotIndicator(
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
    
    val baseRadius = stroke * 0.55f
    val effectiveScale = if (isUrgent) pulseScale else 1f
    
    // 1. Halo exterior difuso (glow)
    drawCircle(
        color = mainColor.copy(alpha = 0.2f),
        radius = baseRadius * 2.2f * effectiveScale,
        center = dotCenter
    )
    
    // 2. Halo medio
    drawCircle(
        color = mainColor.copy(alpha = 0.35f),
        radius = baseRadius * 1.5f * effectiveScale,
        center = dotCenter
    )
    
    // 3. Círculo de color principal
    drawCircle(
        color = mainColor,
        radius = baseRadius,
        center = dotCenter
    )
    
    // 4. Borde blanco para contraste
    drawCircle(
        color = Color.White,
        radius = baseRadius,
        center = dotCenter,
        style = Stroke(width = 2f)
    )
    
    // 5. Núcleo brillante blanco
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = baseRadius * 0.45f,
        center = dotCenter
    )
}

// ========== VERSIÓN SIMPLIFICADA PARA COMPATIBILIDAD ==========

/**
 * Versión simplificada que calcula remainingRatio internamente
 */
@Composable
fun TimerRingPro(
    progress: Float,
    timeText: String,
    modeText: String,
    mainColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    glowEnabled: Boolean = true,
    ticksEnabled: Boolean = true,
    pomodoroProgress: String? = null,
    taskProgressPercent: Float? = null
) {
    // Calcular remainingRatio como inverso del progreso
    // progress = tiempo transcurrido / total → remainingRatio = tiempo restante / total = 1 - progress
    val remainingRatio = (1f - progress).coerceIn(0f, 1f)
    
    TimerRingPro(
        progress = progress,
        remainingRatio = remainingRatio,
        timeText = timeText,
        modeText = modeText,
        mainColor = mainColor,
        modifier = modifier,
        strokeWidth = strokeWidth,
        ticksEnabled = ticksEnabled,
        pomodoroProgress = pomodoroProgress
    )
}
