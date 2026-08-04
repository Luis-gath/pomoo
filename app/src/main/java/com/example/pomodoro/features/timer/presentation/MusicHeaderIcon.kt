package com.example.pomodoro.features.timer.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.pomodoro.core.audio.AudioSettings
import com.example.pomodoro.shared.ui.theme.BackdropPanel
import com.example.pomodoro.shared.ui.theme.OnBackdropMuted

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
    val popupOffset = with(LocalDensity.current) { 52.dp.roundToPx() }

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
                .background(BackdropPanel)
                .border(
                    width = 1.dp,
                    color = if (audioSettings.isMusicEnabled && !audioSettings.isMuted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.66f)
                    } else {
                        OnBackdropMuted.copy(alpha = 0.16f)
                    },
                    shape = CircleShape
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
                OnBackdropMuted

            Icon(
                imageVector = icon,
                contentDescription = when {
                    !audioSettings.isMusicEnabled -> "Abrir música ambiente, desactivada"
                    audioSettings.isMuted -> "Abrir música ambiente, silenciada"
                    else -> "Abrir música ambiente, reproduciendo"
                },
                tint = tint
            )
        }

        if (showPopup) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, popupOffset),
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
