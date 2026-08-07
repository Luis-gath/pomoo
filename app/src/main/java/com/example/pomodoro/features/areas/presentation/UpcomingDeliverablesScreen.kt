package com.example.pomodoro.features.areas.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pomodoro.features.areas.domain.DueBucket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingDeliverablesScreen(
    onBack: () -> Unit,
    onAreaClick: (Int) -> Unit,
    viewModel: UpcomingDeliverablesViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Próximas entregas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tienes entregas pendientes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        // Se agrupa una sola vez y no una vez por franja dentro del cuerpo de la lista,
        // que se reevalúa en cada recomposición.
        val byBucket = remember(rows) { rows.groupBy { it.bucket } }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DueBucket.entries.forEach { bucket ->
                val ofBucket = byBucket[bucket].orEmpty()
                if (ofBucket.isEmpty()) return@forEach

                item(key = "header_$bucket") {
                    Text(
                        text = bucket.label(),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (bucket == DueBucket.VENCIDA) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                items(ofBucket, key = { it.item.id }) { row ->
                    ElevatedCard(onClick = { onAreaClick(row.item.areaId) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.item.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${row.areaName} · ${formatDue(row.item.dueAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { viewModel.markDone(row.item) }) {
                                Text("Entregado")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DueBucket.label(): String = when (this) {
    DueBucket.VENCIDA -> "Vencidas"
    DueBucket.HOY -> "Hoy"
    DueBucket.ESTA_SEMANA -> "Esta semana"
    DueBucket.MAS_ADELANTE -> "Más adelante"
}

private fun formatDue(millis: Long?): String {
    if (millis == null) return ""
    return SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()).format(Date(millis))
}
