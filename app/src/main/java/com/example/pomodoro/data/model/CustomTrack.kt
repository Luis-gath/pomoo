package com.example.pomodoro.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomTrack(
    val id: String,
    val name: String,
    val uri: String
)
