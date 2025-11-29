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

    fun getChatConversations(): LiveData<List<ChatConversation>> {
        return conversationDao.getChatConversations(networkManager.getUserId())
    }
    
    suspend fun sendMessage(recipientId: Int, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val message = Message(
                senderId = networkManager.getUserId(),
                recipientId = recipientId,
                content = content,
                timestamp = System.currentTimeMillis(),
                isSent = false
            )
            
            val messageId = messageDao.insertMessage(message)
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
                val message = Message(
                    senderId = senderId,
                    recipientId = recipientId ?: networkManager.getUserId(),
                    groupId = groupId,
                    content = content,
                    timestamp = timestamp,
                    isSent = true,
                    isDelivered = true
                )
                
                messageDao.insertMessage(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error receiving message", e)
            }
        }
    }
    
    suspend fun markMessagesAsRead(otherUserId: Int) = withContext(Dispatchers.IO) {
        messageDao.markMessagesAsRead(networkManager.getUserId(), otherUserId)
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
    fun getAllGroups(): LiveData<List<Group>> = groupDao.getAllGroups()
    
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
