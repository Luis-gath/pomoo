package com.example.pomodoro.features.auth.domain

/** Cómo se identificó el usuario actual. */
enum class AuthType {
    /** Sesión de invitado: puede usar la app sin dar datos, pero vive solo en este dispositivo. */
    ANONYMOUS,
    GOOGLE,
    NONE
}

/**
 * Usuario autenticado, independiente de Firebase.
 *
 * El resto de la app no debería conocer `FirebaseUser`: así los ViewModels se pueden
 * probar con un repositorio falso, igual que el resto de features.
 */
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val type: AuthType
) {
    val isAnonymous: Boolean get() = type == AuthType.ANONYMOUS
}
