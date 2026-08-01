package com.example.pomodoro.core.audio

import kotlinx.serialization.Serializable

@Serializable
data class CustomTrack(
    val id: String,
    val name: String,
    val uri: String
)
