package com.example.pomodoro.features.timer.presentation

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pomodoro.R
import com.example.pomodoro.features.timer.domain.PomodoroMode
import com.example.pomodoro.shared.ui.theme.BackdropPanel
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

        // El degradado conserva detalle en la parte alta y refuerza el contraste justo donde
        // están la tarea, las métricas y la navegación.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.48f),
                        0.52f to Color.Black.copy(alpha = 0.60f),
                        1f to Color.Black.copy(alpha = 0.76f)
                    )
                )
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
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = BackdropPanel,
                    border = BorderStroke(1.dp, OnBackdrop.copy(alpha = 0.10f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(modeColor, CircleShape)
                        )
                        Text(
                            text = "Ciclo ${uiState.currentCycle}",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnBackdrop
                        )
                    }
                }

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

            ActiveTaskHeaderLabel(modifier = Modifier.fillMaxWidth())
            
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
                    .padding(bottom = 10.dp)
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val gaugeSize = minOf(maxWidth * 0.88f, maxHeight).coerceAtMost(360.dp)
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
                    modifier = Modifier.size(gaugeSize)
                )
            }

            SessionSummary(
                today = uiState.sessionsToday,
                total = uiState.sessionsTotal
            )

            Spacer(modifier = Modifier.height(10.dp))

            HomeBottomNavigation(
                onTasks = { navController.navigate("tasks_list") },
                onAreas = { navController.navigate("areas") },
                onStats = { navController.navigate("stats") },
                onSettings = { navController.navigate("settings") }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 82.dp)
        )
    }
}

@Composable
private fun SessionSummary(today: Int, total: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = BackdropPanel.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, OnBackdrop.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryValue(value = today, label = "hoy")
            Box(
                Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(OnBackdrop.copy(alpha = 0.16f))
            )
            SummaryValue(value = total, label = "en total")
        }
    }
}

@Composable
private fun SummaryValue(value: Int, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = OnBackdrop,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = OnBackdropMuted
        )
    }
}

@Composable
private fun HomeBottomNavigation(
    onTasks: () -> Unit,
    onAreas: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = BackdropPanel,
        border = BorderStroke(1.dp, OnBackdrop.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeNavigationItem(Icons.Default.Assignment, "Tareas", onTasks, Modifier.weight(1f))
            HomeNavigationItem(Icons.Default.Folder, "Áreas", onAreas, Modifier.weight(1f))
            HomeNavigationItem(Icons.Default.BarChart, "Progreso", onStats, Modifier.weight(1f))
            HomeNavigationItem(Icons.Default.Settings, "Ajustes", onSettings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HomeNavigationItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnBackdrop,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OnBackdropMuted,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}
