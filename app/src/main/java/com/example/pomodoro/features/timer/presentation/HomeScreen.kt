package com.example.pomodoro.features.timer.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pomodoro.R
import com.example.pomodoro.core.audio.CustomTrack
import com.example.pomodoro.features.timer.domain.PomodoroMode
import com.example.pomodoro.shared.ui.theme.LocalAppPalette
import com.example.pomodoro.shared.ui.theme.OnBackdrop
import com.example.pomodoro.shared.ui.theme.OnBackdropMuted
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: PomodoroViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeTaskUiState by viewModel.activeTaskUiState.collectAsState()
    val audioSettings by viewModel.audioSettings.collectAsState()
    
    // Los ajustes llegan desde el ViewModel: la pantalla ya no instancia el DataStore.
    val settings by viewModel.settings.collectAsState()

    // Esta pantalla siempre tiene fondo oscuro (imagen + velo), así que sus iconos de sistema
    // deben ser claros aunque el móvil esté en tema claro. Al salir se restaura lo que
    // corresponda al tema, porque el resto de pantallas sí siguen el modo del sistema.
    val view = LocalView.current
    val systemInDark = isSystemInDarkTheme()
    DisposableEffect(systemInDark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = !systemInDark
            controller?.isAppearanceLightNavigationBars = !systemInDark
        }
    }

    // Ajuste "Mantener pantalla encendida": antes el interruptor no hacía nada.
    DisposableEffect(settings.keepScreenOn) {
        val window = (view.context as? Activity)?.window
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Errores de operaciones asíncronas (p. ej. importar audio): antes se tragaban en silencio.
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeErrorMessage()
        }
    }

    // Colores del tono elegido en Ajustes.
    val palette = LocalAppPalette.current
    val modeColor = when (uiState.mode) {
        PomodoroMode.Focus -> palette.focus
        PomodoroMode.ShortBreak -> palette.shortBreak
        PomodoroMode.LongBreak -> palette.longBreak
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo: la imagen que haya elegido el usuario o, si no hay ninguna, la que trae la app.
        if (settings.backgroundUri != null) {
            AsyncImage(
                model = settings.backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(R.drawable.bg_default),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Velo oscuro común a ambos casos: sin él el temporizador no se lee sobre el cielo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // Modo enfoque serio: mientras corre un bloque de foco la pantalla se queda solo con
        // el temporizador. Todo lo demás desaparece, incluida la navegación, porque cada
        // elemento visible es una invitación a salirse.
        val inSeriousFocus = settings.focusModeEnabled &&
            uiState.isRunning &&
            uiState.mode == PomodoroMode.Focus

        if (inSeriousFocus) {
            SeriousFocusLayer(
                remaining = uiState.progress,
                timeText = "%02d:%02d".format(
                    (uiState.currentTimeMillis / 1000) / 60,
                    (uiState.currentTimeMillis / 1000) % 60
                ),
                modeText = uiState.mode.title,
                accent = modeColor,
                onPause = { viewModel.toggleTimer() }
            )
            return@Box
        }

        // El contenido pasa por debajo de las barras del sistema, ahora transparentes:
        // este margen evita que la fila de iconos quede tapada por la barra de gestos.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // -----------------------------------------------------
            // Header Row: Focus Control + Music
            // -----------------------------------------------------
            // El control de play/pausa vive ahora en el centro del cuadrante, así que la
            // cabecera se queda solo con la música. El nombre del modo tampoco hace falta
            // aquí: ya se lee dentro del temporizador.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Music Icon
                MusicHeaderIcon(
                    audioSettings = audioSettings,
                    allTracks = viewModel.getAllTracks(audioSettings.customTracks),
                    onToggleMusic = { viewModel.setMusicEnabled(it) },
                    onToggleMute = { viewModel.setMuted(it) },
                    onVolumeChange = { viewModel.setVolume(it) },
                    onTrackSelect = { viewModel.selectTrack(it) },
                    onImportClick = { viewModel.importAudio(it) }
                )
            }
            
            // -----------------------------------------------------
            // Active Task Header & Card (NEW)
            // -----------------------------------------------------
            // Only show label if we have a card or if we want to prompt
            ActiveTaskHeaderLabel()
            
            ActiveTaskCard(
                state = activeTaskUiState,
                onSelectTask = { navController.navigate("tasks_list") },
                onOpenTask = {
                     if (activeTaskUiState.hasActiveTask && uiState.activeTaskId != null) {
                         navController.navigate("task_editor?taskId=${uiState.activeTaskId}")
                     } else {
                         navController.navigate("tasks_list")
                     }
                },
                onClearTask = { viewModel.clearActiveTask() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // -----------------------------------------------------
            // Center: Timer Ring (Wait, if card is big, ring needs space)
            // -----------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val timeText = "%02d:%02d".format(
                    (uiState.currentTimeMillis / 1000) / 60,
                    (uiState.currentTimeMillis / 1000) % 60
                )
                
                // uiState.progress es la fracción de tiempo que QUEDA (va de 1 a 0).
                TimerGauge(
                    remaining = uiState.progress,
                    timeText = timeText,
                    modeText = uiState.mode.title,
                    accent = modeColor,
                    isRunning = uiState.isRunning,
                    onToggleRunning = { viewModel.toggleTimer() },
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Mini
            Text(
                text = "Hoy: ${uiState.sessionsToday} | Total: ${uiState.sessionsTotal}",
                style = MaterialTheme.typography.labelMedium,
                color = OnBackdropMuted
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                 IconButton(onClick = { navController.navigate("tasks_list") }) {
                    Icon(Icons.Default.Assignment, contentDescription = "Tareas", tint = OnBackdrop)
                }
                IconButton(onClick = { navController.navigate("areas") }) {
                    Icon(Icons.Default.Folder, contentDescription = "Mis áreas", tint = OnBackdrop)
                }
                IconButton(onClick = { navController.navigate("stats") }) {
                    Icon(Icons.Default.BarChart, contentDescription = "Estadísticas", tint = OnBackdrop)
                }
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = OnBackdrop)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
