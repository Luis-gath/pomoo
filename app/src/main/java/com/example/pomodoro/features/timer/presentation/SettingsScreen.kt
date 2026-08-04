package com.example.pomodoro.features.timer.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pomodoro.core.focus.DoNotDisturbController
import com.example.pomodoro.features.auth.presentation.AuthViewModel
import com.example.pomodoro.navigation.NavGraph
import com.example.pomodoro.shared.ui.theme.AppTone
import com.example.pomodoro.shared.ui.theme.palette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: PomodoroViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    // El permiso de No molestar se concede fuera de la app; se vuelve a consultar al regresar.
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
        onResult = viewModel::updateBackground
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SettingsSection(
                    title = "Cuenta",
                    description = "Inicia sesión para conservar tu progreso y participar en la comunidad."
                ) {
                    val user = authState.user
                    if (user == null || user.isAnonymous) {
                        Text(
                            text = if (user == null) "No has iniciado sesión"
                            else "Estás como invitado: tu progreso solo vive en este dispositivo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = { navController.navigate(NavGraph.SIGN_IN) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (user == null) "Iniciar sesión" else "Vincular con Google")
                        }
                    } else {
                        Text(
                            text = user.displayName ?: user.email.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        // El correo solo como subtítulo: si no hay nombre ya se muestra arriba.
                        user.email?.takeIf { user.displayName != null }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = authViewModel::signOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text("Cerrar sesión")
                        }
                    }
                }

                SettingsSection(
                    title = "Premium",
                    description = "Apoya el desarrollo y desbloquea las funciones avanzadas."
                ) {
                    Button(
                        onClick = { navController.navigate("premium") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver opciones premium")
                    }
                }

                SettingsSection(
                    title = "Temporizador",
                    description = "Define la duración de cada fase y el ritmo de tus ciclos."
                ) {
                    SliderSetting(
                        label = "Enfoque",
                        value = settings.focusDurationMinutes,
                        min = 1f,
                        max = 90f
                    ) { viewModel.updateSettings(settings.copy(focusDurationMinutes = it)) }

                    SettingDivider()

                    SliderSetting(
                        label = "Descanso corto",
                        value = settings.shortBreakDurationMinutes,
                        min = 1f,
                        max = 30f
                    ) { viewModel.updateSettings(settings.copy(shortBreakDurationMinutes = it)) }

                    SettingDivider()

                    SliderSetting(
                        label = "Descanso largo",
                        value = settings.longBreakDurationMinutes,
                        min = 1f,
                        max = 60f
                    ) { viewModel.updateSettings(settings.copy(longBreakDurationMinutes = it)) }

                    SettingDivider()

                    SliderSetting(
                        label = "Descanso largo cada",
                        value = settings.longBreakEveryNCycles,
                        min = 2f,
                        max = 8f,
                        valueLabel = { "$it ciclos" }
                    ) { viewModel.updateSettings(settings.copy(longBreakEveryNCycles = it)) }
                }

                SettingsSection(
                    title = "Enfoque serio",
                    description = "Reduce las distracciones mientras el temporizador está en marcha."
                ) {
                    SwitchSetting(
                        label = "Pantalla despejada durante el foco",
                        checked = settings.focusModeEnabled
                    ) { viewModel.updateSettings(settings.copy(focusModeEnabled = it)) }

                    Text(
                        "Durante el enfoque solo se ve el temporizador. Toca la pantalla para mostrar los controles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (settings.focusModeEnabled) {
                        SettingDivider()

                        SwitchSetting(
                            label = "Silenciar el móvil",
                            checked = settings.blockNotificationsInFocus
                        ) { viewModel.updateSettings(settings.copy(blockNotificationsInFocus = it)) }

                        Text(
                            "Activa No molestar mientras dura un bloque de enfoque.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (settings.blockNotificationsInFocus && !dndGranted) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(
                                        "Falta acceso a No molestar",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Sin este permiso, el modo funciona pero no silencia otras aplicaciones.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    TextButton(onClick = { context.startActivity(dndSettingsIntent) }) {
                                        Text("Conceder acceso")
                                    }
                                }
                            }
                        }
                    }
                }

                SettingsSection(
                    title = "Tono de la interfaz",
                    description = "El acento se aplica al temporizador y al resto de la app."
                ) {
                    AppTone.entries.forEachIndexed { index, tone ->
                        ToneOption(
                            tone = tone,
                            selected = settings.toneName == tone.name,
                            onClick = {
                                viewModel.updateSettings(settings.copy(toneName = tone.name))
                            }
                        )
                        if (index != AppTone.entries.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }

                SettingsSection(
                    title = "Fondo",
                    description = "Personaliza la imagen que acompaña al temporizador."
                ) {
                    if (settings.backgroundUri != null) {
                        AsyncImage(
                            model = settings.backgroundUri,
                            contentDescription = "Vista previa del fondo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (settings.backgroundUri == null) "Elegir imagen" else "Cambiar imagen")
                    }

                    if (settings.backgroundUri != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.updateBackground(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Restaurar fondo predeterminado")
                        }
                    }
                }

                SettingsSection(title = "Comportamiento") {
                    SwitchSetting("Iniciar automáticamente la siguiente fase", settings.autoStartNext) {
                        viewModel.updateSettings(settings.copy(autoStartNext = it))
                    }
                    SettingDivider()
                    SwitchSetting("Sonido al terminar", settings.soundEnabled) {
                        viewModel.updateSettings(settings.copy(soundEnabled = it))
                    }
                    SettingDivider()
                    SwitchSetting("Vibración al terminar", settings.vibrationEnabled) {
                        viewModel.updateSettings(settings.copy(vibrationEnabled = it))
                    }
                    SettingDivider()
                    SwitchSetting("Mantener la pantalla encendida", settings.keepScreenOn) {
                        viewModel.updateSettings(settings.copy(keepScreenOn = it))
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (description != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(9.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingDivider() {
    Divider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun SliderSetting(
    label: String,
    value: Int,
    min: Float,
    max: Float,
    valueLabel: (Int) -> String = { "$it min" },
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = valueLabel(value),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = min..max,
            steps = (max - min).toInt() - 1
        )
    }
}

@Composable
fun SwitchSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ToneOption(
    tone: AppTone,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tone.label, style = MaterialTheme.typography.labelLarge)
                Text(
                    tone.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    tone.palette.focus,
                    tone.palette.shortBreak,
                    tone.palette.longBreak
                ).forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(swatch)
                    )
                }
            }
        }
    }
}
