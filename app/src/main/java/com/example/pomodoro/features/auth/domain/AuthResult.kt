package com.example.pomodoro.features.auth.domain

/** Desenlace de un intento de autenticación. */
sealed interface AuthResult {

    data class Success(val user: AuthUser) : AuthResult

    /**
     * El usuario cerró el selector de cuentas. No es un fallo y no debe mostrarse
     * como error: distinguirlo evita el clásico "Error: actividad cancelada".
     */
    data object Cancelled : AuthResult

    /** No hay ninguna cuenta de Google utilizable en el dispositivo. */
    data object NoAccounts : AuthResult

    data class Error(val message: String) : AuthResult
}
