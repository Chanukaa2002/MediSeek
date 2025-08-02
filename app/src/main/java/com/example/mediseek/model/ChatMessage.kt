package com.example.mediseek.model

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val type: String = "text"
)