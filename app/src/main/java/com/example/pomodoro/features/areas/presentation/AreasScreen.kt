package com.example.pomodoro.features.areas.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pomodoro.features.areas.data.Area
import com.example.pomodoro.features.areas.data.AreaType

private val AREA_COLORS = listOf(
    "#2E7D57", "#B8862B", "#3D6BA8", "#A8412A", "#6B4E9B", "#2F7C86"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreasScreen(
    onAreaClick: (Area) -> Unit,
    onBack: () -> Unit,
    viewModel: AreasViewModel = hiltViewModel()
) {
    val areas by viewModel.areas.collectAsState()
    val deliverables by viewModel.deliverables.collectAsState()
    val showCreate by viewModel.showCreateDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis áreas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva área") }
            )
        }
    ) { padding ->
        if (areas.isEmpty()) {
            EmptyAreas(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (deliverables.isNotEmpty()) {
                    item {
                        Text(
                            "${deliverables.size} entrega(s) pendiente(s)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                items(areas, key = { it.id }) { area ->
                    AreaRow(area = area, onClick = { onAreaClick(area) })
                }
            }
        }
    }

    if (showCreate) {
        CreateAreaDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name, type, color -> viewModel.createArea(name, type, color) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AreaRow(area: Area, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(runCatching { Color(android.graphics.Color.parseColor(area.colorHex)) }
                        .getOrDefault(MaterialTheme.colorScheme.primary))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(area.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    when (area.type) {
                        AreaType.CURSO -> "Curso"
                        AreaType.HABILIDAD -> "Habilidad"
                        AreaType.PROYECTO -> "Proyecto"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyAreas(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Aún no tienes áreas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Un área es un curso, una habilidad que estés aprendiendo o un proyecto. " +
                "Dentro guardas sus apuntes, PDFs, vídeos y enlaces.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                    placeholder = { Text("Ej: Cálculo II") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AreaType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = {
                                Text(
                                    when (option) {
                                        AreaType.CURSO -> "Curso"
                                        AreaType.HABILIDAD -> "Habilidad"
                                        AreaType.PROYECTO -> "Proyecto"
                                    }
                                )
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AREA_COLORS.forEach { hex ->
                        val c = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(if (color == hex) 32.dp else 26.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { color = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, type, color) },
                enabled = name.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
