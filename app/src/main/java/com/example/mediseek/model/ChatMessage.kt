package com.example.mediseek.model

data class ChatMessage(
    val senderId: String = "",
    val text:String="",
    val timestamp: Long = 0L
)
