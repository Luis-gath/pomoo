package com.example.pomodoro.features.auth.data

import android.content.Context
import com.example.pomodoro.features.auth.domain.AuthResult
import com.example.pomodoro.features.auth.domain.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * Autenticación de la aplicación.
 *
 * Las operaciones de Google reciben el [Context] de la Activity porque Credential Manager
 * necesita una ventana sobre la que dibujar el selector de cuentas. El identificador de
 * cliente OAuth no se pide por parámetro: lo resuelve la implementación.
 */
interface AuthRepository {

    /** Emite el usuario actual, o null si no hay sesión. */
    val currentUser: Flow<AuthUser?>

    fun currentUserOrNull(): AuthUser?

    /** Sesión de invitado, para poder usar la app sin registrarse. */
    suspend fun signInAnonymously(): AuthResult

    suspend fun signInWithGoogle(activityContext: Context): AuthResult

    /**
     * Convierte la sesión de invitado en una cuenta de Google conservando el mismo uid,
     * de modo que el usuario no pierde lo que hizo como invitado.
     */
    suspend fun linkAnonymousToGoogle(activityContext: Context): AuthResult

    fun signOut()
}
