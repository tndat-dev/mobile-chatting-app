package com.example.myapplication.data.model

data class ChatConversation(
    val userId: Int,
    val username: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val isOnline: Boolean
)
