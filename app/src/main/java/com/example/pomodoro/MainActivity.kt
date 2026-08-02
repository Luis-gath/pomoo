package com.example.pomodoro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import com.example.pomodoro.core.share.ShareInbox
import com.example.pomodoro.features.timer.data.SettingsRepository
import com.example.pomodoro.features.timer.domain.Settings
import com.example.pomodoro.navigation.NavGraph
import com.example.pomodoro.shared.ui.theme.AppTone
import com.example.pomodoro.shared.ui.theme.PomodoroTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var shareInbox: ShareInbox

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        shareInbox.offer(intent)

        setContent {
            // El tono elegido en Ajustes manda sobre toda la app.
            val settings by settingsRepository.settingsFlow
                .collectAsState(initial = Settings())
            val tone = remember(settings.toneName) {
                runCatching { AppTone.valueOf(settings.toneName) }.getOrDefault(AppTone.CALIDA)
            }

            PomodoroTheme(tone = tone) {
                RequestNotificationPermission()

                val navController = rememberNavController()

                // Cuando llega algo compartido, se va a la pantalla de archivar. Sirve tanto
                // si la app estaba cerrada como si ya estaba abierta (ver onNewIntent).
                val pending by shareInbox.pending.collectAsState()
                LaunchedEffect(pending) {
                    if (!pending.isEmpty) {
                        navController.navigate(NavGraph.RECEIVE_SHARE) {
                            launchSingleTop = true
                        }
                    }
                }

                NavGraph(navController = navController)
            }
        }
    }

    /** La app ya estaba abierta y el usuario compartió algo con ella. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shareInbox.offer(intent)
    }
}

/**
 * Desde Android 13 (API 33) las notificaciones requieren permiso en tiempo de ejecución.
 * Sin concederlo el sistema descarta en silencio tanto la notificación del temporizador
 * como los recordatorios de tareas: no hay error ni aviso, simplemente no aparecen.
 */
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Si se deniega, la app sigue funcionando pero sin avisos. */ }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
