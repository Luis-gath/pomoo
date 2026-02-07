package com.example.pomodoro.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.pomodoro.ui.components.MusicFloatingButton
import com.example.pomodoro.ui.components.FocusHeaderControl
import com.example.pomodoro.ui.components.MusicHeaderIcon
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pomodoro.data.datastore.SettingsDataStore
import com.example.pomodoro.data.model.CustomTrack
import com.example.pomodoro.domain.model.PomodoroMode
import com.example.pomodoro.domain.model.Settings
import com.example.pomodoro.ui.components.ModeChip
import com.example.pomodoro.ui.components.PrimaryControls
import com.example.pomodoro.ui.components.ActiveTaskCard
import com.example.pomodoro.ui.components.ActiveTaskHeaderLabel
import com.example.pomodoro.ui.components.TimerRingGamerPremium
import com.example.pomodoro.ui.theme.FocusColor
import com.example.pomodoro.ui.theme.LongBreakColor
import com.example.pomodoro.ui.theme.ShortBreakColor
import com.example.pomodoro.viewmodel.PomodoroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: PomodoroViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeTaskUiState by viewModel.activeTaskUiState.collectAsState()
    val audioSettings by viewModel.audioSettings.collectAsState()
    
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    
    // We observe settings here for background.
    val settings by settingsDataStore.settingsFlow.collectAsState(initial = Settings())

    val modeColor = when (uiState.mode) {
        PomodoroMode.Focus -> FocusColor
        PomodoroMode.ShortBreak -> ShortBreakColor
        PomodoroMode.LongBreak -> LongBreakColor
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Layer
        if (settings.backgroundUri != null) {
            AsyncImage(
                model = settings.backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        } else {
             Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        // Main Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp), 
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // -----------------------------------------------------
            // Header Row: Focus Control + Music
            // -----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Focus Control
                FocusHeaderControl(
                    modeText = uiState.mode.title,
                    isRunning = uiState.isRunning,
                    pomodoroLabel = null, // Moved to Active Task Card
                    onPause = { viewModel.toggleTimer() },
                    onResume = { viewModel.toggleTimer() },
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(12.dp))

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
                
                TimerRingGamerPremium(
                    progress = uiState.progress,
                    timeText = timeText,
                    modeText = uiState.mode.title,
                    mainColor = modeColor,
                    modifier = Modifier.fillMaxWidth(0.85f), // Slightly smaller to fit header
                    strokeWidth = 24.dp,
                    pomodoroProgress = null // Removed from ring
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Mini
            Text(
                text = "Hoy: ${uiState.sessionsToday} | Total: ${uiState.sessionsTotal}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                 IconButton(onClick = { navController.navigate("tasks_list") }) {
                    Icon(Icons.Default.Assignment, contentDescription = "Tareas", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { navController.navigate("stats") }) {
                    Icon(Icons.Default.BarChart, contentDescription = "Estadísticas", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
