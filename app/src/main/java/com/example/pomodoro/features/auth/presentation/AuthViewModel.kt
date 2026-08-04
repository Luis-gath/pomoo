package com.example.pomodoro.features.auth.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.features.auth.data.AuthRepository
import com.example.pomodoro.features.auth.domain.AuthResult
import com.example.pomodoro.features.auth.domain.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val user: AuthUser? = null,
    val isWorking: Boolean = false,
    val error: String? = null
) {
    val isSignedIn: Boolean get() = user != null
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val transient = MutableStateFlow(AuthUiState())

    val uiState: StateFlow<AuthUiState> = combine(
        transient,
        authRepository.currentUser
    ) { state, user ->
        state.copy(user = user)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AuthUiState(user = authRepository.currentUserOrNull())
    )

    /**
     * Si ya hay una sesión de invitado, vincula esa cuenta en lugar de crear otra nueva:
     * así el usuario conserva lo que hizo antes de registrarse.
     */
    fun signInWithGoogle(activityContext: Context) = run {
        val isGuest = authRepository.currentUserOrNull()?.isAnonymous == true
        launchAuth {
            if (isGuest) authRepository.linkAnonymousToGoogle(activityContext)
            else authRepository.signInWithGoogle(activityContext)
        }
    }

    fun continueAsGuest() = launchAuth { authRepository.signInAnonymously() }

    fun signOut() {
        authRepository.signOut()
        transient.value = AuthUiState()
    }

    fun dismissError() {
        transient.value = transient.value.copy(error = null)
    }

    private fun launchAuth(block: suspend () -> AuthResult) {
        transient.value = transient.value.copy(isWorking = true, error = null)
        viewModelScope.launch {
            val message = when (val result = block()) {
                is AuthResult.Success -> null
                // Cerrar el selector es una decisión del usuario, no un fallo que reportar.
                AuthResult.Cancelled -> null
                AuthResult.NoAccounts ->
                    "No hay ninguna cuenta de Google en este dispositivo. Añade una en los ajustes de Android."
                is AuthResult.Error -> result.message
            }
            transient.value = transient.value.copy(isWorking = false, error = message)
        }
    }
}
