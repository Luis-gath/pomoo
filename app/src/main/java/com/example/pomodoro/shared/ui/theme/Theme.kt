package com.example.pomodoro.shared.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun PomodoroTheme(
    tone: AppTone = AppTone.CALIDA,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = tone.palette

    // Ya no se usa el color dinámico de Android 12+: tomaba los colores del fondo de
    // pantalla del móvil, con lo que el tono elegido en Ajustes no se habría respetado.
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = Ink,
            primaryContainer = lerp(DarkSurfaceVariant, palette.primary, 0.30f),
            onPrimaryContainer = TextPrimary,
            secondary = palette.secondary,
            onSecondary = Ink,
            secondaryContainer = lerp(DarkSurfaceVariant, palette.secondary, 0.26f),
            onSecondaryContainer = TextPrimary,
            tertiary = palette.longBreak,
            onTertiary = Ink,
            tertiaryContainer = lerp(DarkSurfaceVariant, palette.longBreak, 0.26f),
            onTertiaryContainer = TextPrimary,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            onSurfaceVariant = TextSecondary,
            outline = DarkOutline,
            outlineVariant = DarkOutlineVariant,
            error = Color(0xFFFFB4AB),
            errorContainer = Color(0xFF5B211D),
            onError = Color(0xFF3A0907),
            onErrorContainer = Color(0xFFFFDAD5)
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Ink,
            primaryContainer = lerp(LightSurfaceVariant, palette.primary, 0.24f),
            onPrimaryContainer = Ink,
            secondary = palette.secondary,
            onSecondary = Ink,
            secondaryContainer = lerp(LightSurfaceVariant, palette.secondary, 0.22f),
            onSecondaryContainer = Ink,
            tertiary = palette.longBreak,
            onTertiary = Ink,
            tertiaryContainer = lerp(LightSurfaceVariant, palette.longBreak, 0.22f),
            onTertiaryContainer = Ink,
            background = LightBackground,
            surface = LightSurface,
            surfaceVariant = LightSurfaceVariant,
            onBackground = Ink,
            onSurface = Ink,
            onSurfaceVariant = InkMuted,
            outline = LightOutline,
            outlineVariant = LightOutlineVariant,
            error = Color(0xFFBA1A1A),
            errorContainer = Color(0xFFFFDAD6),
            onError = Color.White,
            onErrorContainer = Color(0xFF410002)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Barras transparentes para que el contenido llegue de borde a borde. Antes se
            // pintaba la de estado con el color de fondo del tema, que en modo claro es casi
            // blanco: de ahí las franjas blancas arriba y abajo sobre la imagen de fondo.
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
