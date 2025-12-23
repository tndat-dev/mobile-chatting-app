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
            CASE 
                WHEN EXISTS (
                    SELECT 1 FROM messages m4 
                    WHERE m4.groupId IS NULL 
                      AND m4.senderId = f.userId 
                      AND m4.recipientId = :currentUserId 
                      AND m4.isRead = 0
                ) THEN 1 
                ELSE 0 
            END AS hasUnread,
            f.isOnline AS isOnline,
            0 AS isGroup,
            NULL AS groupId
                    FROM friends f
                WHERE EXISTS (
                        SELECT 1 FROM messages m_exist
                        WHERE m_exist.groupId IS NULL
                            AND (
                                        (m_exist.senderId = f.userId AND m_exist.recipientId = :currentUserId)
                                 OR (m_exist.senderId = :currentUserId AND m_exist.recipientId = f.userId)
                            )
                )
        
        UNION ALL
        
        SELECT 
            g.id AS userId,
            g.name AS username,
            (
                SELECT content FROM messages m5
                WHERE m5.groupId = g.id
                ORDER BY m5.timestamp DESC
                LIMIT 1
            ) AS lastMessage,
            (
                SELECT MAX(m6.timestamp) FROM messages m6
                WHERE m6.groupId = g.id
            ) AS lastMessageTime,
            0 AS hasUnread,
            0 AS isOnline,
            1 AS isGroup,
            g.id AS groupId
        FROM groups g
        WHERE EXISTS (
            SELECT 1 FROM group_members gm
            WHERE gm.groupId = g.id AND gm.userId = :currentUserId
        )
        AND EXISTS (
            SELECT 1 FROM messages m_group_exist
            WHERE m_group_exist.groupId = g.id
        )
        
        ORDER BY lastMessageTime DESC
        """
    )
    fun getChatConversations(currentUserId: Int): LiveData<List<ChatConversation>>
}
