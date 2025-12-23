package com.example.myapplication.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.myapplication.data.dao.*
import com.example.myapplication.data.model.*
import com.example.myapplication.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepository(
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val friendDao: FriendDao,
    private val friendRequestDao: FriendRequestDao,
    private val groupDao: GroupDao,
    private val groupMemberDao: GroupMemberDao,
    private val conversationDao: ConversationDao,
    private val networkManager: NetworkManager
) {
    
    companion object {
        private const val TAG = "ChatRepository"
    }
    
    // Message operations
    fun getConversationMessages(currentUserId: Int, otherUserId: Int): LiveData<List<Message>> {
        return messageDao.getConversationMessages(currentUserId, otherUserId)
    }
    
    fun getGroupMessages(groupId: Int): LiveData<List<Message>> {
        return messageDao.getGroupMessages(groupId)
    }

    fun getChatConversations(currentUserId: Int): LiveData<List<ChatConversation>> {
        return conversationDao.getChatConversations(currentUserId)
    }
    
    suspend fun sendMessage(recipientId: Int, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "sendMessage: Inserting message to Room - recipient=$recipientId, content=$content")
            val message = Message(
                senderId = networkManager.getUserId(),
                recipientId = recipientId,
                content = content,
                timestamp = System.currentTimeMillis(),
                isSent = false
            )
            
            val messageId = messageDao.insertMessage(message)
            Log.d(TAG, "sendMessage: Message inserted with id=$messageId")
            val sent = networkManager.sendChatMessage(recipientId, content)
            
            if (sent) {
                messageDao.markAsSent(messageId)
            }
            
            sent
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            false
        }
    }
    
    suspend fun sendGroupMessage(groupId: Int, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val message = Message(
                senderId = networkManager.getUserId(),
                recipientId = -1,
                groupId = groupId,
                content = content,
                timestamp = System.currentTimeMillis(),
                isSent = false
            )
            
            val messageId = messageDao.insertMessage(message)
            val sent = networkManager.sendGroupMessage(groupId, content)
            
            if (sent) {
                messageDao.markAsSent(messageId)
            }
            
            sent
        } catch (e: Exception) {
            Log.e(TAG, "Error sending group message", e)
            false
        }
    }
    
    suspend fun receiveMessage(senderId: Int, content: String, timestamp: Long, groupId: Int? = null, recipientId: Int? = null) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "receiveMessage: sender=$senderId, recipient=${recipientId ?: networkManager.getUserId()}, content=$content, timestamp=$timestamp, groupId=$groupId")

                // If this is a group message, ensure we have a local Group and GroupMember record
                if (groupId != null) {
                    try {
                        val existingGroup = groupDao.getGroup(groupId)
                        if (existingGroup == null) {
                            // Create a lightweight placeholder Group so the UI can show it
                            val placeholder = Group(id = groupId, name = "Group $groupId", creatorId = senderId, memberCount = 1)
                            groupDao.insertGroup(placeholder)
                            Log.d(TAG, "receiveMessage: Inserted placeholder group id=$groupId")
                        }

                        val existingMember = groupMemberDao.getMember(groupId, networkManager.getUserId())
                        if (existingMember == null) {
                            val gm = GroupMember(
                                groupId = groupId,
                                userId = networkManager.getUserId(),
                                username = "Me",
                                nickname = null,
                                isAdmin = false,
                                joinedAt = System.currentTimeMillis()
                            )
                            groupMemberDao.insertMember(gm)
                            Log.d(TAG, "receiveMessage: Inserted local group member record for group=$groupId user=${networkManager.getUserId()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error ensuring local group membership", e)
                    }
                }

                // Deduplicate incoming live messages by checking for a recent matching message
                if (groupId != null) {
                    // For group messages, dedupe by senderId + groupId + content
                    val existing = messageDao.findLatestMatchingGroupMessage(senderId, groupId, content)
                    if (existing != null) {
                        val diff = kotlin.math.abs(existing.timestamp - timestamp)
                        if (diff < 15000) {
                            if (!existing.isDelivered || !existing.isSent) {
                                val updated = existing.copy(isSent = true, isDelivered = true)
                                messageDao.updateMessage(updated)
                                Log.d(TAG, "receiveMessage: Updated existing group message id=${existing.id} (dedup)")
                            } else {
                                Log.d(TAG, "receiveMessage: Duplicate live group message ignored id=${existing.id}")
                            }
                        } else {
                            val message = Message(
                                senderId = senderId,
                                recipientId = networkManager.getUserId(),
                                groupId = groupId,
                                content = content,
                                timestamp = timestamp,
                                isSent = true,
                                isDelivered = true
                            )
                            val insertedId = messageDao.insertMessage(message)
                            Log.d(TAG, "receiveMessage: Inserted new group message id=$insertedId")
                        }
                    } else {
                        val message = Message(
                            senderId = senderId,
                            recipientId = networkManager.getUserId(),
                            groupId = groupId,
                            content = content,
                            timestamp = timestamp,
                            isSent = true,
                            isDelivered = true
                        )
                        val insertedId = messageDao.insertMessage(message)
                        Log.d(TAG, "receiveMessage: Group message inserted id=$insertedId")
                    }
                } else {
                    val targetRecipient = recipientId ?: networkManager.getUserId()
                    val existing = messageDao.findLatestMatchingMessage(senderId, targetRecipient, content)
                    if (existing != null) {
                        val diff = kotlin.math.abs(existing.timestamp - timestamp)
                        if (diff < 15000) {
                            if (!existing.isDelivered || !existing.isSent) {
                                val updated = existing.copy(isSent = true, isDelivered = true)
                                messageDao.updateMessage(updated)
                                Log.d(TAG, "receiveMessage: Updated existing message id=${existing.id} (dedup)")
                            } else {
                                Log.d(TAG, "receiveMessage: Duplicate live message ignored id=${existing.id}")
                            }
                        } else {
                            val message = Message(
                                senderId = senderId,
                                recipientId = targetRecipient,
                                groupId = null,
                                content = content,
                                timestamp = timestamp,
                                isSent = true,
                                isDelivered = true
                            )
                            val insertedId = messageDao.insertMessage(message)
                            Log.d(TAG, "receiveMessage: Inserted new message id=$insertedId")
                        }
                    } else {
                        val message = Message(
                            senderId = senderId,
                            recipientId = targetRecipient,
                            groupId = null,
                            content = content,
                            timestamp = timestamp,
                            isSent = true,
                            isDelivered = true
                        )
                        val insertedId = messageDao.insertMessage(message)
                        Log.d(TAG, "receiveMessage: Message inserted id=$insertedId")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error receiving message", e)
            }
        }
    }

    suspend fun receiveHistoryMessage(senderId: Int, recipientId: Int, content: String, timestamp: Long) {
        withContext(Dispatchers.IO) {
            try {
                val existing = messageDao.findLatestMatchingMessage(senderId, recipientId, content)
                if (existing != null) {
                    // If timestamps differ but within 15s, treat as same message; update flags & keep earliest timestamp
                    val diff = kotlin.math.abs(existing.timestamp - timestamp)
                    if (diff < 15000) {
                        if (!existing.isSent || !existing.isDelivered) {
                            val updated = existing.copy(isSent = true, isDelivered = true)
                            messageDao.updateMessage(updated)
                            Log.d(TAG, "receiveHistoryMessage: Updated existing message id=${existing.id} (dedup)")
                        } else {
                            Log.d(TAG, "receiveHistoryMessage: Duplicate within threshold ignored id=${existing.id}")
                        }
                        return@withContext
                    }
                }
                val message = Message(
                    senderId = senderId,
                    recipientId = recipientId,
                    content = content,
                    timestamp = timestamp,
                    isSent = true,
                    isDelivered = true
                )
                val id = messageDao.insertMessage(message)
                Log.d(TAG, "receiveHistoryMessage: Inserted new history message id=$id")
            } catch (e: Exception) {
                Log.e(TAG, "Error receiving history message", e)
            }
        }
    }
    
    suspend fun markMessagesAsRead(otherUserId: Int) = withContext(Dispatchers.IO) {
        messageDao.markMessagesAsRead(networkManager.getUserId(), otherUserId)
    }
    
    suspend fun deleteConversation(otherUserId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "deleteConversation: Deleting conversation with user $otherUserId")
            // Delete from local database
            messageDao.deleteConversation(networkManager.getUserId(), otherUserId)
            // Delete from server
            val success = networkManager.deleteConversation(otherUserId)
            if (success) {
                Log.d(TAG, "deleteConversation: Successfully deleted conversation")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting conversation", e)
            false
        }
    }
    
    // Friend operations
    fun getAllFriends(): LiveData<List<Friend>> = friendDao.getAllFriends()
    
    fun getOnlineFriends(): LiveData<List<Friend>> = friendDao.getOnlineFriends()
    
    suspend fun sendFriendRequest(targetUserId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // Look up username by id from local caches (friends or users)
            val cachedFriend = friendDao.getFriend(targetUserId)
            val username = cachedFriend?.username ?: userDao.getUser(targetUserId)?.username
            if (username != null) {
                networkManager.sendFriendRequest(username)
            } else {
                Log.e(TAG, "No username found for userId=$targetUserId; cannot send request")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request", e)
            false
        }
    }
    
    suspend fun acceptFriendRequest(userId: Int, username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val success = networkManager.acceptFriendRequest(userId)
            if (success) {
                val friend = Friend(
                    userId = userId,
                    username = username,
                    isOnline = false,
                    lastSeen = System.currentTimeMillis()
                )
                friendDao.insertFriend(friend)
                // Remove pending request from local DB
                friendRequestDao.deleteByUsers(userId, networkManager.getUserId())
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request", e)
            false
        }
    }
    
    suspend fun declineFriendRequest(userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val success = networkManager.declineFriendRequest(userId)
            if (success) {
                // Remove pending request from local DB
                friendRequestDao.deleteByUsers(userId, networkManager.getUserId())
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error declining friend request", e)
            false
        }
    }
    
    suspend fun unfriend(userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val success = networkManager.unfriend(userId)
            if (success) {
                friendDao.deleteFriendById(userId)
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error unfriending", e)
            false
        }
    }
    
    suspend fun updateFriendStatus(userId: Int, isOnline: Boolean) = withContext(Dispatchers.IO) {
        friendDao.updateFriendStatus(userId, isOnline, System.currentTimeMillis())
    }
    
    suspend fun addFriend(friend: Friend) = withContext(Dispatchers.IO) {
        friendDao.insertFriend(friend)
    }
    
    // Friend request operations
    fun getPendingRequests(userId: Int): LiveData<List<FriendRequest>> {
        return friendRequestDao.getPendingRequests(userId)
    }
    
    suspend fun addFriendRequest(request: FriendRequest) = withContext(Dispatchers.IO) {
        friendRequestDao.insertRequest(request)
    }
    
    // Group operations
    fun getAllGroups(userId: Int): LiveData<List<Group>> = groupDao.getAllGroupsForUser(userId)
    
    fun getGroupMembers(groupId: Int): LiveData<List<GroupMember>> {
        return groupMemberDao.getGroupMembers(groupId)
    }
    
    suspend fun createGroup(groupName: String, memberIds: List<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            networkManager.createGroup(groupName, memberIds)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating group", e)
            false
        }
    }
    
    suspend fun addGroup(group: Group) = withContext(Dispatchers.IO) {
        groupDao.insertGroup(group)
    }
    
    suspend fun addGroupMember(member: GroupMember) = withContext(Dispatchers.IO) {
        groupMemberDao.insertMember(member)
    }
    
    suspend fun inviteToGroup(groupId: Int, userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            networkManager.inviteToGroup(groupId, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error inviting to group", e)
            false
        }
    }
    
    suspend fun removeFromGroup(groupId: Int, userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val success = networkManager.removeFromGroup(groupId, userId)
            if (success) {
                groupMemberDao.removeMember(groupId, userId)
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from group", e)
            false
        }
    }
    
    suspend fun leaveGroup(groupId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val success = networkManager.leaveGroup(groupId)
            if (success) {
                groupMemberDao.removeMember(groupId, networkManager.getUserId())
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving group", e)
            false
        }
    }
}
