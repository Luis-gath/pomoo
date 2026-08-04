package com.example.pomodoro.features.timer.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.pomodoro.shared.ui.theme.OnBackdrop
import com.example.pomodoro.shared.ui.theme.OnBackdropMuted
import kotlin.math.cos
import kotlin.math.sin

private const val MINOR_TICKS = 60
private const val TICKS_PER_MAJOR = 5
private const val START_ANGLE = -90f

/**
 * Cuadrante del temporizador, con aire de velocímetro.
 *
 * Dos decisiones que lo diferencian del anillo anterior:
 *
 * 1. **No hay estela.** No se dibuja ningún aro de fondo que marque el recorrido completo:
 *    solo existe el arco de lo que queda. Lo ya consumido desaparece, y de la esfera solo
 *    permanece una graduación tenue que da la referencia sin ensuciar la imagen de fondo.
 * 2. **Nitidez en lugar de resplandor.** Extremos redondeados, marcas finas y un halo oscuro
 *    detrás del cuadrante. Ese halo es lo que lo hace legible sobre cualquier fotografía
 *    sin tener que oscurecer la pantalla entera.
 *
 * @param remaining fracción de tiempo que queda, de 1 a 0.
 */
@Composable
fun TimerGauge(
    remaining: Float,
    timeText: String,
    modeText: String,
    accent: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    subtitle: String? = null,
    isRunning: Boolean = false,
    /** Si se pasa, aparece el control de play/pausa en el centro del cuadrante. */
    onToggleRunning: (() -> Unit)? = null
) {
    val target = remaining.coerceIn(0f, 1f)

    // Solo se anima el avance del arco; no hay animaciones en bucle permanente, que era lo
    // que mantenía la pantalla repintándose incluso con el temporizador en pausa.
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "remaining"
    )

    val density = LocalDensity.current
    val strokePx = with(density) { strokeWidth.toPx() }

    BoxWithConstraints(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        val timeFontSize = when {
            maxWidth < 180.dp -> 40.sp
            maxWidth < 240.dp -> 50.sp
            else -> 58.sp
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outer = size.minDimension / 2f
            val radius = outer - strokePx * 2.2f

            drawUniformBackdrop(center, outer)
            drawGraduation(center, radius, strokePx, animated, accent)
            drawRemainingArc(center, radius, strokePx, animated, accent)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = modeText.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = OnBackdropMuted,
                letterSpacing = 3.sp
            )
            Text(
                text = timeText,
                fontSize = timeFontSize,
                fontWeight = FontWeight.Light,
                color = OnBackdrop,
                textAlign = TextAlign.Center
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackdropMuted
                )
            }

            // Control discreto bajo el tiempo: sin fondo relleno ni color de acento, solo
            // el contorno. Va aquí y no en la cabecera porque el centro del cuadrante es
            // donde ya está puesta la mirada.
            onToggleRunning?.let { toggle ->
                Spacer(Modifier.height(10.dp))
                IconButton(
                    onClick = toggle,
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (isRunning) Color.Transparent else accent.copy(alpha = 0.16f),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (isRunning) OnBackdrop.copy(alpha = 0.22f)
                            else accent.copy(alpha = 0.62f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pausar" else "Iniciar temporizador",
                        tint = if (isRunning) OnBackdrop.copy(alpha = 0.75f) else OnBackdrop
                    )
                }
            }
        }
    }
}

/**
 * Superficie oscura y homogénea detrás del cuadrante. El tono permanece plano en la zona
 * central y sólo se desvanece en el borde para integrarse con la fotografía.
 */
private fun DrawScope.drawUniformBackdrop(center: Offset, outer: Float) {
    val uniformTone = Color(0xFF211D21)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to uniformTone,
                0.88f to uniformTone,
                1.0f to uniformTone.copy(alpha = 0f)
            ),
            center = center,
            radius = outer
        ),
        radius = outer,
        center = center
    )
}

/**
 * Graduación tipo esfera de instrumento: 60 marcas finas con una más larga cada cinco.
 * Las que ya pasaron quedan casi invisibles, de modo que la esfera se vacía en vez de
 * dejar rastro.
 */
private fun DrawScope.drawGraduation(
    center: Offset,
    radius: Float,
    strokePx: Float,
    remaining: Float,
    accent: Color
) {
    val consumedTicks = ((1f - remaining) * MINOR_TICKS).toInt()
    val tickOuter = radius - strokePx * 1.25f

    for (i in 0 until MINOR_TICKS) {
        val isMajor = i % TICKS_PER_MAJOR == 0
        val spent = i < consumedTicks

        val length = if (isMajor) strokePx * 1.15f else strokePx * 0.55f
        val width = if (isMajor) 2.4f else 1.4f
        val color = when {
            spent -> Color.White.copy(alpha = 0.07f)
            isMajor -> OnBackdrop.copy(alpha = 0.85f)
            else -> OnBackdrop.copy(alpha = 0.38f)
        }

        val angle = Math.toRadians((START_ANGLE + i * (360f / MINOR_TICKS)).toDouble())
        val cosA = cos(angle).toFloat()
        val sinA = sin(angle).toFloat()

        drawLine(
            color = color,
            start = Offset(center.x + cosA * (tickOuter - length), center.y + sinA * (tickOuter - length)),
            end = Offset(center.x + cosA * tickOuter, center.y + sinA * tickOuter),
            strokeWidth = width,
            cap = StrokeCap.Butt
        )
    }
}

/**
 * El arco de lo que queda. Su extremo inicial avanza en el sentido de las agujas del reloj
 * conforme pasa el tiempo, así que lo recorrido no deja rastro: simplemente deja de existir.
 */
private fun DrawScope.drawRemainingArc(
    center: Offset,
    radius: Float,
    strokePx: Float,
    remaining: Float,
    accent: Color
) {
    if (remaining <= 0.001f) return

    val sweep = remaining * 360f
    val start = START_ANGLE + (1f - remaining) * 360f
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)

    // Trazo de refuerzo: mismo arco, más ancho y translúcido. Da cuerpo sin difuminar el
    // borde, que es lo que restaba nitidez al diseño anterior.
    drawArc(
        color = accent.copy(alpha = 0.22f),
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokePx * 2.1f, cap = StrokeCap.Round)
    )

    // Arco principal, con degradado para que se note la dirección de avance.
    drawArc(
        brush = Brush.sweepGradient(
            colorStops = arrayOf(
                0f to accent,
                0.5f to accent.copy(alpha = 0.92f),
                1f to accent.copy(alpha = 0.72f)
            ),
            center = center
        ),
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
    )

    // Testigo en la cabeza del arco, como la aguja de un instrumento.
    val headAngle = Math.toRadians(start.toDouble())
    val head = Offset(
        center.x + cos(headAngle).toFloat() * radius,
        center.y + sin(headAngle).toFloat() * radius
    )
    drawCircle(color = Color.White.copy(alpha = 0.30f), radius = strokePx * 0.85f, center = head)
    drawCircle(color = OnBackdrop, radius = strokePx * 0.40f, center = head)
}
