package com.example.pomodoro.features.auth.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Pantalla de acceso: entrar con Google o seguir como invitado.
 *
 * Si el usuario ya estaba como invitado, entrar con Google vincula esa misma cuenta en
 * lugar de crear otra, de modo que no pierde su historial.
 */
@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    onSkip: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.signedIn.collect { onSignedIn() }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val isGuest = state.user?.isAnonymous == true

            Text(
                text = "Tu progreso, en cualquier dispositivo",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isGuest) {
                    "Vincula tu cuenta para no perder lo que ya llevas hecho como invitado."
                } else {
                    "Inicia sesión para conservar tus estadísticas y participar en la comunidad."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 32.dp)
            )

            if (state.isWorking) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else {
                Button(
                    onClick = { activity?.let(viewModel::signInWithGoogle) },
                    enabled = activity != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isGuest) "Vincular con Google" else "Continuar con Google")
                }

                // Ya siendo invitado no tiene sentido ofrecer volver a entrar como tal.
                if (!isGuest) {
                    OutlinedButton(
                        onClick = viewModel::continueAsGuest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text("Entrar como invitado")
                    }
                }

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Ahora no")
                }
            }
        }
    }
}

/**
 * Credential Manager dibuja el selector de cuentas sobre una Activity, así que no vale
 * cualquier Context: hay que desenvolver los ContextWrapper hasta encontrarla.
 */
private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
