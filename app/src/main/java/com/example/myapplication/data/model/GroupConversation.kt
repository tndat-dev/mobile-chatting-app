package com.example.myapplication.data.model

data class GroupConversation(
    val groupId: Int,
    val groupName: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val memberCount: Int
)
