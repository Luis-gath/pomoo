package com.example.pomodoro.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class AudioPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    fun playFile(uri: Uri) {
        stop()
        player = MediaPlayer.create(context, uri).apply {
            start()
            setOnCompletionListener {
                stop()
            }
        }
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
    }

    fun isPlaying(): Boolean = player?.isPlaying ?: false
}
