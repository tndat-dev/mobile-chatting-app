package com.example.myapplication.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.example.myapplication.data.model.ChatConversation

@Dao
interface ConversationDao {
    @Query(
        """
        SELECT 
            f.userId AS userId,
            f.username AS username,
            (
                SELECT content FROM messages m2 
                WHERE m2.groupId IS NULL 
                  AND (
                        (m2.senderId = f.userId AND m2.recipientId = :currentUserId)
                     OR (m2.senderId = :currentUserId AND m2.recipientId = f.userId)
                  )
                ORDER BY m2.timestamp DESC
                LIMIT 1
            ) AS lastMessage,
            (
                SELECT MAX(m3.timestamp) FROM messages m3 
                WHERE m3.groupId IS NULL 
                  AND (
                        (m3.senderId = f.userId AND m3.recipientId = :currentUserId)
                     OR (m3.senderId = :currentUserId AND m3.recipientId = f.userId)
                  )
            ) AS lastMessageTime,
            (
                SELECT COUNT(*) FROM messages m4 
                WHERE m4.groupId IS NULL 
                  AND m4.senderId = f.userId 
                  AND m4.recipientId = :currentUserId 
                  AND m4.isRead = 0
            ) AS unreadCount,
            f.isOnline AS isOnline
        FROM friends f
        WHERE EXISTS (
            SELECT 1 FROM messages m 
            WHERE m.groupId IS NULL 
              AND (
                    (m.senderId = f.userId AND m.recipientId = :currentUserId)
                 OR (m.senderId = :currentUserId AND m.recipientId = f.userId)
              )
        )
        ORDER BY lastMessageTime DESC
        """
    )
    fun getChatConversations(currentUserId: Int): LiveData<List<ChatConversation>>
}
