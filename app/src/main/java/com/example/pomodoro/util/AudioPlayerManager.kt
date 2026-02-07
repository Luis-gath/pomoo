package com.example.pomodoro.util

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class AudioPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null

    // Track map to resolve resource IDs
    private val tracksMap = mapOf(
        "lofi_beat" to com.example.pomodoro.R.raw.lofi_beat,
        "rain_sounds" to com.example.pomodoro.R.raw.rain_sounds
    )

    fun initializePlayer() {
        if (exoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true) // Handle audio focus
                .build()
            
            exoPlayer?.repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    fun playTrack(trackId: String, customUri: String?, volume: Float, isMuted: Boolean) {
        initializePlayer()
        
        try {
            val mediaItem = if (trackId.startsWith("custom_") && !customUri.isNullOrBlank()) {
                // Custom Track from URI
                MediaItem.fromUri(Uri.parse(customUri))
            } else {
                // Internal Resource Track
                val resourceId = tracksMap[trackId] ?: com.example.pomodoro.R.raw.lofi_beat
                val uri = Uri.parse("android.resource://${context.packageName}/$resourceId")
                MediaItem.fromUri(uri)
            }

            exoPlayer?.let { player ->
                val currentMediaItem = player.currentMediaItem
                // Avoid reloading if it's the same track and playing
                if (currentMediaItem?.localConfiguration?.uri != mediaItem.localConfiguration?.uri || player.playbackState == Player.STATE_IDLE) {
                    player.setMediaItem(mediaItem)
                    player.prepare()
                }
                
                player.volume = if (isMuted) 0f else volume
                
                if (!player.isPlaying) {
                    player.play()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVolume(volume: Float, isMuted: Boolean) {
        exoPlayer?.volume = if (isMuted) 0f else volume
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        if (exoPlayer != null && exoPlayer?.playbackState != Player.STATE_IDLE) {
            exoPlayer?.play()
        }
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
    
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying == true
    }
    
    fun getAvailableTracks(): List<Pair<String, String>> {
        return listOf(
            "lofi_beat" to "Lo-fi Beats",
            "rain_sounds" to "Rain Sounds"
        )
    }
}

