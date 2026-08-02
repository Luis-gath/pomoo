package com.example.pomodoro.shared.ui.theme

import androidx.compose.ui.graphics.Color

// Fondos y superficies del tema oscuro. Los colores de acento ya no viven aquí: dependen del
// tono elegido en Ajustes y están en AppTone.kt.
val DarkBackground = Color(0xFF121212)
val SurfaceColor = Color(0xFF1E1E1E)

val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFFB0B0B0)

// --- Contenido sobre la imagen de fondo ---
// La pantalla principal siempre se dibuja sobre una imagen con un velo negro encima, así que
// su contenido es claro SIEMPRE, sin importar si el sistema está en tema claro u oscuro.
// Usar aquí colorScheme.onSurface hacía que en tema claro los iconos salieran casi negros
// sobre fondo oscuro, es decir, invisibles.
val OnBackdrop = Color(0xFFF2F4F3)
val OnBackdropMuted = Color(0xFFBFC7C3)

/** Panel translúcido para tarjetas sobre la imagen: oscuro fijo, nunca claro. */
val BackdropPanel = Color(0x8C121A16)
