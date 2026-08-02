package com.example.pomodoro.shared.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tono de la interfaz, elegible desde Ajustes.
 *
 * Sustituye al color dinámico de Android 12+, que tomaba los colores del fondo de pantalla
 * del móvil e ignoraba por completo la paleta definida aquí. Los tres tonos son
 * deliberadamente apagados: la pantalla principal se ve sobre una fotografía, y los colores
 * saturados competían con ella en lugar de acompañarla.
 */
enum class AppTone(val label: String, val description: String) {
    CALIDA("Cálida", "Arena y terracota"),
    FRIA("Fría", "Azul niebla y salvia"),
    NEUTRA("Neutra", "Grises suaves")
}

/** Colores que cambian con el tono elegido. */
data class AppPalette(
    val focus: Color,
    val shortBreak: Color,
    val longBreak: Color,
    val primary: Color,
    val secondary: Color
)

val AppTone.palette: AppPalette
    get() = when (this) {
        AppTone.CALIDA -> AppPalette(
            focus = Color(0xFFC08268),
            shortBreak = Color(0xFF8FA98B),
            longBreak = Color(0xFFB9976B),
            primary = Color(0xFFC08268),
            secondary = Color(0xFF8FA98B)
        )

        AppTone.FRIA -> AppPalette(
            focus = Color(0xFF7C97B5),
            shortBreak = Color(0xFF86A89B),
            longBreak = Color(0xFF8E8FB4),
            primary = Color(0xFF7C97B5),
            secondary = Color(0xFF86A89B)
        )

        AppTone.NEUTRA -> AppPalette(
            focus = Color(0xFFA8918A),
            shortBreak = Color(0xFF90998F),
            longBreak = Color(0xFF8C919B),
            primary = Color(0xFFA8918A),
            secondary = Color(0xFF90998F)
        )
    }

/**
 * Paleta activa. Se expone así para que cualquier pantalla lea el color del modo en curso
 * sin tener que recibirlo por parámetro desde arriba.
 */
val LocalAppPalette = staticCompositionLocalOf { AppTone.CALIDA.palette }
