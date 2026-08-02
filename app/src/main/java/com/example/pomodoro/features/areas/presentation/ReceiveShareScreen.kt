package com.example.pomodoro.features.areas.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pomodoro.features.areas.data.ItemMark

/**
 * Pantalla que aparece cuando el usuario comparte algo con la app desde WhatsApp, Drive,
 * el navegador o cualquier otra. Es el atajo que evita tener que entrar a buscar archivos:
 * se archivan en el momento en que llegan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveShareScreen(
    onDone: () -> Unit,
    viewModel: ReceiveShareViewModel = hiltViewModel()
) {
    val incoming by viewModel.incoming.collectAsState()
    val areas by viewModel.areas.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val result by viewModel.result.collectAsState()

    var selectedAreaId by remember { mutableStateOf<Int?>(null) }
    var mark by remember { mutableStateOf(ItemMark.NINGUNA) }
    var newAreaName by remember { mutableStateOf("") }

    LaunchedEffect(result) {
        if (result != null) onDone()
    }

    val count = incoming.uris.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guardar en un área", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.discard(); onDone() }) {
                        Icon(Icons.Default.Close, contentDescription = "Descartar")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val id = selectedAreaId
                            if (id != null) {
                                viewModel.save(id, mark, null)
                            } else if (newAreaName.isNotBlank()) {
                                viewModel.createAreaAndSave(newAreaName, mark, null)
                            }
                        },
                        enabled = !saving && (selectedAreaId != null || newAreaName.isNotBlank())
                    ) {
                        Text(if (saving) "Guardando…" else "Guardar")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    when {
                        count > 1 -> "$count archivos"
                        count == 1 -> "1 archivo"
                        else -> "Texto compartido"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                incoming.text?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it.take(140),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mark == ItemMark.NINGUNA,
                        onClick = { mark = ItemMark.NINGUNA },
                        label = { Text("Sin marca") }
                    )
                    FilterChip(
                        selected = mark == ItemMark.IMPORTANTE,
                        onClick = { mark = ItemMark.IMPORTANTE },
                        label = { Text("Importante") }
                    )
                    FilterChip(
                        selected = mark == ItemMark.ENTREGA,
                        onClick = { mark = ItemMark.ENTREGA },
                        label = { Text("Entrega") }
                    )
                }
            }

            if (areas.isEmpty()) {
                item {
                    OutlinedTextField(
                        value = newAreaName,
                        onValueChange = { newAreaName = it },
                        label = { Text("Nombre del área") },
                        placeholder = { Text("Ej: Cálculo II") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(areas, key = { it.id }) { area ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedAreaId == area.id,
                                onClick = { selectedAreaId = area.id }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAreaId == area.id,
                            onClick = { selectedAreaId = area.id }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(area.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
