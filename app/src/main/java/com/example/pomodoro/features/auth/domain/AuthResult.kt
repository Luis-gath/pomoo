package com.example.pomodoro.features.auth.domain

/** Desenlace de un intento de autenticación. */
sealed interface AuthResult {

    data class Success(val user: AuthUser) : AuthResult

    /**
     * El usuario cerró el selector de cuentas. No es un fallo y no debe mostrarse
     * como error: distinguirlo evita el clásico "Error: actividad cancelada".
     */
    data object Cancelled : AuthResult

    /**
     * Credential Manager no pudo entregar ninguna credencial.
     *
     * No significa necesariamente que falten cuentas en el dispositivo: también ocurre
     * cuando Google rechaza la petición, por ejemplo si el proveedor está deshabilitado
     * en Firebase. Por eso el mensaje al usuario no debe afirmar la causa.
     */
    data object NoCredential : AuthResult

    data class Error(val message: String) : AuthResult
}
