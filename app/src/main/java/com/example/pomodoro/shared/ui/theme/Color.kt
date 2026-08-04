package com.example.pomodoro.shared.ui.theme

import androidx.compose.ui.graphics.Color

// Neutros comunes. Definir todos los roles evita que Material complete los que faltan con
// sus violetas predeterminados, que no pertenecen a ninguna de las paletas de la app.
val DarkBackground = Color(0xFF101412)
val DarkSurface = Color(0xFF181D1A)
val DarkSurfaceVariant = Color(0xFF272D2A)
val DarkOutline = Color(0xFF89918D)
val DarkOutlineVariant = Color(0xFF414844)

val LightBackground = Color(0xFFF6F7F4)
val LightSurface = Color(0xFFFCFDF9)
val LightSurfaceVariant = Color(0xFFE3E7E3)
val LightOutline = Color(0xFF737B76)
val LightOutlineVariant = Color(0xFFC3CAC5)

val TextPrimary = Color(0xFFF2F4F3)
val TextSecondary = Color(0xFFBFC7C3)
val Ink = Color(0xFF1A1F1C)
val InkMuted = Color(0xFF555D58)

// --- Contenido sobre la imagen de fondo ---
// La pantalla principal siempre se dibuja sobre una imagen con un velo negro encima, así que
// su contenido es claro SIEMPRE, sin importar si el sistema está en tema claro u oscuro.
// Usar aquí colorScheme.onSurface hacía que en tema claro los iconos salieran casi negros
// sobre fondo oscuro, es decir, invisibles.
val OnBackdrop = TextPrimary
val OnBackdropMuted = TextSecondary

/** Panel translúcido para tarjetas sobre la imagen: oscuro fijo, nunca claro. */
val BackdropPanel = Color(0x8C121A16)
