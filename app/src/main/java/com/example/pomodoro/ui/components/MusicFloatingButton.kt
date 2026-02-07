package com.example.pomodoro.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.pomodoro.data.datastore.AudioSettings

@Composable
fun MusicFloatingButton(
    audioSettings: AudioSettings,
    allTracks: List<Pair<String, String>>,
    onToggleMusic: (Boolean) -> Unit,
    onToggleMute: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTrackSelect: (String) -> Unit,
    onImportClick: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPopup by remember { mutableStateOf(false) }
    
    // Launcher para importar música
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportClick(it) }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // --- Floating Button ---
        FloatingActionButton(
            onClick = { showPopup = !showPopup },
            containerColor = if (audioSettings.isMusicEnabled && !audioSettings.isMuted) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (audioSettings.isMusicEnabled && !audioSettings.isMuted) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            shape = CircleShape,
            modifier = Modifier
                .size(56.dp)
                .shadow(8.dp, CircleShape)
        ) {
            val icon = if (audioSettings.isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.MusicNote
            
            // Animación de pulso si está sonando
            val infiniteTransition = rememberInfiniteTransition(label = "music_pulse")
            val scale by if (audioSettings.isMusicEnabled && !audioSettings.isMuted) {
                infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
            } else {
                remember { mutableFloatStateOf(1f) }
            }

            Icon(
                imageVector = icon,
                contentDescription = "Música",
                modifier = Modifier.scale(scale)
            )
        }

        // --- Popup Bubble ---
        if (showPopup) {
            Popup(
                alignment = Alignment.BottomEnd,
                onDismissRequest = { showPopup = false },
                properties = PopupProperties(focusable = true)
            ) {
                MusicBubblePanel(
                    audioSettings = audioSettings,
                    allTracks = allTracks,
                    onToggleMusic = onToggleMusic,
                    onToggleMute = onToggleMute,
                    onVolumeChange = onVolumeChange,
                    onTrackSelect = onTrackSelect,
                    onImportRequest = { launcher.launch(arrayOf("audio/*")) },
                    onDismiss = { showPopup = false }
                )
            }
        }
    }
}

@Composable
private fun MusicBubblePanel(
    audioSettings: AudioSettings,
    allTracks: List<Pair<String, String>>,
    onToggleMusic: (Boolean) -> Unit,
    onToggleMute: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTrackSelect: (String) -> Unit,
    onImportRequest: () -> Unit,
    onDismiss: () -> Unit
) {
    // Current Track Name
    val currentTrackName = allTracks.find { it.first == audioSettings.selectedTrackId }?.second 
        ?: "Desconocido"

    Card(
        modifier = Modifier
            .padding(bottom = 72.dp, end = 16.dp) // Offset from FAB
            .width(300.dp)
            .heightIn(max = 450.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .border(
                1.dp, 
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                ), 
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- Header: Switch & Mute ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "🎧 Ambiente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mute Button
                    IconButton(
                        onClick = { onToggleMute(!audioSettings.isMuted) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (audioSettings.isMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                            contentDescription = "Mute",
                            tint = if (audioSettings.isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(Modifier.width(8.dp))

                    // Master Switch
                    Switch(
                        checked = audioSettings.isMusicEnabled,
                        onCheckedChange = { onToggleMusic(it) },
                        thumbContent = if (audioSettings.isMusicEnabled) {
                            { Icon(Icons.Rounded.MusicNote, null, Modifier.size(12.dp)) }
                        } else null
                    )
                }
            }
            
            Divider(Modifier.padding(vertical = 12.dp).fillMaxWidth().alpha(0.2f))
            
            // --- Current Track Info ---
            if (audioSettings.isMusicEnabled) {
                Text(
                    "REPRODUCIENDO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    currentTrackName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(12.dp))
                
                // --- Volume Slider ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VolumeDown, 
                        null, 
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = audioSettings.volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Icon(
                        Icons.Default.VolumeUp, 
                        null, 
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "Música desactivada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider(Modifier.padding(vertical = 12.dp).fillMaxWidth().alpha(0.2f))
            
            // --- Track List ---
            Text(
                "Biblioteca",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(allTracks) { (id, name) ->
                    TrackItem(
                        name = name,
                        isSelected = id == audioSettings.selectedTrackId,
                        isPlaying = audioSettings.isMusicEnabled && !audioSettings.isMuted && id == audioSettings.selectedTrackId,
                        onClick = { onTrackSelect(id) }
                    )
                }
                
                item {
                    // Import Button
                    OutlinedButton(
                        onClick = onImportRequest,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        border = null
                    ) {
                        Icon(Icons.Default.UploadFile, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Importar canción...")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackItem(
    name: String,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) 
        else 
            Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Icon(
                        Icons.Rounded.MusicNote, 
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Rounded.Audiotrack, 
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
