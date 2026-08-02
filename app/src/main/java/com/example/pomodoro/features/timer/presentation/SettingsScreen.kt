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
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.pomodoro.core.focus.DoNotDisturbController
import com.example.pomodoro.shared.ui.theme.AppTone
import com.example.pomodoro.shared.ui.theme.palette
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

    // El permiso de No molestar se comprueba al volver a la pantalla, porque se concede
    // fuera de la app y no hay forma de que nos avise cuando cambia.
    val context = LocalContext.current
    val dnd = remember { DoNotDisturbController(context) }
    val dndSettingsIntent = remember { dnd.accessSettingsIntent() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var dndGranted by remember { mutableStateOf(dnd.hasAccess()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) dndGranted = dnd.hasAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            Text("Modo enfoque serio", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            SwitchSetting("Pantalla despejada durante el foco", settings.focusModeEnabled) {
                viewModel.updateSettings(settings.copy(focusModeEnabled = it))
            }
            Text(
                "Durante los bloques de enfoque solo se ve el temporizador. Los controles " +
                    "aparecen al tocar la pantalla.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (settings.focusModeEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                SwitchSetting("Silenciar el móvil (No molestar)", settings.blockNotificationsInFocus) {
                    viewModel.updateSettings(settings.copy(blockNotificationsInFocus = it))
                }

                // Bloquear avisos de otras apps exige un permiso que solo se concede a mano
                // en una pantalla del sistema: no se puede pedir con un diálogo normal.
                if (settings.blockNotificationsInFocus && !dndGranted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Falta conceder el acceso a No molestar. Sin él, el modo enfoque " +
                            "funciona igual pero no silencia las demás aplicaciones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = { context.startActivity(dndSettingsIntent) }) {
                        Text("Conceder acceso")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Tono de la interfaz", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            AppTone.entries.forEach { tone ->
                val selected = settings.toneName == tone.name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            onClick = { viewModel.updateSettings(settings.copy(toneName = tone.name)) }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { viewModel.updateSettings(settings.copy(toneName = tone.name)) }
                    )
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tone.label)
                        Text(
                            tone.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Muestra de los tres colores del tono.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            tone.palette.focus,
                            tone.palette.shortBreak,
                            tone.palette.longBreak
                        ).forEach { swatch ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                            )
                        }
                    }
                }
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
                        Text("Restaurar predeterminado")
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
