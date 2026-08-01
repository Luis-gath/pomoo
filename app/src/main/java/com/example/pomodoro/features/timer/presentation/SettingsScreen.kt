package com.example.pomodoro.features.timer.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: PomodoroViewModel = hiltViewModel()
) {
    // Los ajustes se leen y escriben a través del ViewModel, no del DataStore directamente.
    val settings by viewModel.settings.collectAsState()

    // Errores al cambiar el fondo: antes se tragaban en silencio.
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeErrorMessage()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            viewModel.updateBackground(uri)
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Duraciones (minutos)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            SliderSetting("Enfoque", settings.focusDurationMinutes, 1f, 90f) {
                viewModel.updateSettings(settings.copy(focusDurationMinutes = it))
            }
            SliderSetting("Descanso Corto", settings.shortBreakDurationMinutes, 1f, 30f) {
                viewModel.updateSettings(settings.copy(shortBreakDurationMinutes = it))
            }
            SliderSetting("Descanso Largo", settings.longBreakDurationMinutes, 1f, 60f) {
                viewModel.updateSettings(settings.copy(longBreakDurationMinutes = it))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ciclos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            SliderSetting("Descanso largo cada N ciclos", settings.longBreakEveryNCycles, 2f, 8f) {
                viewModel.updateSettings(settings.copy(longBreakEveryNCycles = it))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Fondo (Background)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { 
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text("Elegir Imagen")
                }
                
                if (settings.backgroundUri != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { viewModel.updateBackground(null) }) {
                        Text("Quitar Fondo")
                    }
                }
            }
            
            if (settings.backgroundUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Vista previa (min):", style = MaterialTheme.typography.bodySmall)
                AsyncImage(
                    model = settings.backgroundUri,
                    contentDescription = "Preview",
                    modifier = Modifier.size(100.dp).padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Opciones", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            SwitchSetting("Auto-iniciar siguiente fase", settings.autoStartNext) {
                viewModel.updateSettings(settings.copy(autoStartNext = it))
            }
            SwitchSetting("Sonido", settings.soundEnabled) {
                viewModel.updateSettings(settings.copy(soundEnabled = it))
            }
            SwitchSetting("Vibración", settings.vibrationEnabled) {
                viewModel.updateSettings(settings.copy(vibrationEnabled = it))
            }
            SwitchSetting("Mantener pantalla encendida", settings.keepScreenOn) {
                viewModel.updateSettings(settings.copy(keepScreenOn = it))
            }
        }
    }
}

@Composable
fun SliderSetting(label: String, value: Int, min: Float, max: Float, onValueChange: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(value.toString())
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = min..max,
            steps = (max - min).toInt() - 1 // Discrete steps
        )
    }
}

@Composable
fun SwitchSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
