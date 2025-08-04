package com.example.mediseek.model

data class ChatItem(
    val userId: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val hasNewMessage: Boolean = false
)
