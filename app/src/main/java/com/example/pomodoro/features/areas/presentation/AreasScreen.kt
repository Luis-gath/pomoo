package com.example.pomodoro.features.areas.presentation

import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pomodoro.features.areas.data.Area
import com.example.pomodoro.features.areas.data.AreaType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val AREA_COLORS = listOf(
    "#2E7D57", "#B8862B", "#3D6BA8", "#A8412A", "#6B4E9B", "#2F7C86"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreasScreen(
    onAreaClick: (Area) -> Unit,
    onBack: () -> Unit,
    onUpcomingClick: () -> Unit,
    viewModel: AreasViewModel = hiltViewModel()
) {
    val overviews by viewModel.areaOverviews.collectAsState()
    val deliverables by viewModel.deliverables.collectAsState()
    val showCreate by viewModel.showCreateDialog.collectAsState()
    val remainingFree by viewModel.remainingFreeAreas.collectAsState()
    val limitReached by viewModel.limitReached.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(limitReached) {
        if (limitReached) {
            snackbarHostState.showSnackbar(
                "Has llegado al límite de áreas gratuitas. Hazte premium para crear más."
            )
            viewModel.dismissLimitNotice()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis áreas", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (overviews.size == 1) "1 espacio activo"
                            else "${overviews.size} espacios activos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    // Acceso a la vista transversal: sin esto, la única forma de ver una
                    // entrega es entrar a su área, y "qué tengo esta semana" no tiene respuesta.
                    IconButton(onClick = onUpcomingClick) {
                        Icon(Icons.Default.Event, contentDescription = "Próximas entregas")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (overviews.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::showCreateDialog,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    // Avisar de cuántas quedan antes de tocar el botón evita la sorpresa
                    // de que la app te frene justo cuando ibas a crear algo.
                    text = {
                        Text(
                            if (remainingFree != null && remainingFree!! <= 1) {
                                "Nueva área · queda $remainingFree"
                            } else {
                                "Nueva área"
                            }
                        )
                    }
                )
            }
        }
    ) { padding ->
        if (overviews.isEmpty()) {
            EmptyAreas(
                onCreate = viewModel::showCreateDialog,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AreasOverviewPanel(
                        areaCount = overviews.size,
                        materialCount = overviews.sumOf { it.materialCount },
                        deliveryCount = deliverables.size
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tus espacios", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Pulsa una tarjeta para abrirla",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(overviews, key = { it.area.id }) { overview ->
                    AreaCard(
                        overview = overview,
                        onClick = { onAreaClick(overview.area) }
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateAreaDialog(
            onDismiss = viewModel::hideCreateDialog,
            onCreate = viewModel::createArea
        )
    }
}

@Composable
private fun AreasOverviewPanel(
    areaCount: Int,
    materialCount: Int,
    deliveryCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverviewMetric(areaCount, "Áreas", Modifier.weight(1f))
            MetricDivider()
            OverviewMetric(materialCount, "Materiales", Modifier.weight(1f))
            MetricDivider()
            OverviewMetric(
                value = deliveryCount,
                label = "Entregas",
                modifier = Modifier.weight(1f),
                isAlert = deliveryCount > 0
            )
        }
    }
}

@Composable
private fun OverviewMetric(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(targetState = value, label = "area_metric") { count ->
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.13f))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AreaCard(
    overview: AreaOverviewUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val area = overview.area
    val accent = remember(area.colorHex) { parseAreaColor(area.colorHex) }
    val (typeLabel, typeIcon) = area.type.visuals()

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 170.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    if (overview.deliveryCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.AssignmentLate,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    overview.deliveryCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = area.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(10.dp))

                Text(
                    text = materialCountLabel(overview.materialCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatLastActivity(overview.lastActivityAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (overview.importantCount > 0) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "${overview.importantCount} importantes",
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            overview.importantCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAreas(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(20.dp).size(34.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Crea tu primer espacio",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Organiza cursos, habilidades y proyectos junto con sus apuntes, archivos y entregas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Crear área")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAreaDialog(
    onDismiss: () -> Unit,
    onCreate: (String, AreaType, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AreaType.CURSO) }
    var color by remember { mutableStateOf(AREA_COLORS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva área") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Cálculo II") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Tipo de área", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AreaType.entries.forEach { option ->
                            val selected = type == option
                            Surface(
                                onClick = { type = option },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.visuals().first,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Color", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AREA_COLORS.forEach { hex ->
                            val swatch = remember(hex) { parseAreaColor(hex) }
                            val selected = color == hex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = { color = hex }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 32.dp else 27.dp)
                                        .clip(CircleShape)
                                        .background(swatch),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, type, color) },
                enabled = name.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun AreaType.visuals(): Pair<String, ImageVector> = when (this) {
    AreaType.CURSO -> "Curso" to Icons.Default.School
    AreaType.HABILIDAD -> "Habilidad" to Icons.Default.Psychology
    AreaType.PROYECTO -> "Proyecto" to Icons.Default.RocketLaunch
}

private fun parseAreaColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF3D6BA8))

private fun materialCountLabel(count: Int): String = when (count) {
    0 -> "Sin materiales"
    1 -> "1 material"
    else -> "$count materiales"
}

private fun formatLastActivity(timestamp: Long?): String {
    if (timestamp == null) return "Listo para empezar"

    val now = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return when {
        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) -> "Actualizado hoy"
        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - date.get(Calendar.DAY_OF_YEAR) == 1 -> "Actualizado ayer"
        else -> "Actualizado ${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))}"
    }
}
