package com.example.pomodoro.features.timer.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.pomodoro.core.audio.AudioSettings

@Composable
fun MusicHeaderIcon(
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

    // Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportClick(it) }
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = { showPopup = !showPopup },
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (audioSettings.isMusicEnabled && !audioSettings.isMuted) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                    } else Modifier
                )
        ) {
            val icon = when {
                !audioSettings.isMusicEnabled -> Icons.Rounded.MusicOff
                audioSettings.isMuted -> Icons.Rounded.VolumeOff
                else -> Icons.Rounded.MusicNote
            }
            
            val tint = if (audioSettings.isMusicEnabled && !audioSettings.isMuted) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

            Icon(
                imageVector = icon,
                contentDescription = "Música",
                tint = tint
            )
        }

        if (showPopup) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, 50), // Baja un poco
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
                    onDismiss = { showPopup = false },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
