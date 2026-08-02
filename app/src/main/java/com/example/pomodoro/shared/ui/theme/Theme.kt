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
            secondary = palette.secondary,
            tertiary = palette.focus,
            background = DarkBackground,
            surface = SurfaceColor,
            onPrimary = DarkBackground,
            onSecondary = DarkBackground,
            onTertiary = TextPrimary,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            secondary = palette.secondary,
            tertiary = palette.focus,
            onPrimary = Color.White,
            onSecondary = Color.White
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
            content = content
        )
    }
}
