package com.example.myapplication.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.database.ChatDatabase
import com.example.myapplication.data.model.Friend
import com.example.myapplication.data.model.FriendRequest
import com.example.myapplication.data.model.RequestStatus
import com.example.myapplication.data.repository.ChatRepository
import com.example.myapplication.network.NetworkManager
import com.example.myapplication.utils.ActivityLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FriendViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = ChatDatabase.getDatabase(application)
    private val networkManager = NetworkManager.getInstance()
    private val logger = ActivityLogger.getInstance(application)
    
    private val repository = ChatRepository(
        database.userDao(),
        database.messageDao(),
        database.friendDao(),
        database.friendRequestDao(),
        database.groupDao(),
        database.groupMemberDao(),
        database.conversationDao(),
        networkManager
    )
    
    private val _operationStatus = MutableLiveData<Pair<Boolean, String>>()
    val operationStatus: LiveData<Pair<Boolean, String>> = _operationStatus
    
    fun getAllFriends(): LiveData<List<Friend>> {
        return repository.getAllFriends()
    }
    
    fun getOnlineFriends(): LiveData<List<Friend>> {
        return repository.getOnlineFriends()
    }
    
    fun getPendingRequests(userId: Int): LiveData<List<FriendRequest>> {
        return repository.getPendingRequests(userId)
    }
    
    fun sendFriendRequest(targetUserId: Int) {
        viewModelScope.launch {
            try {
                logger.logFriendAction("SEND_REQUEST", networkManager.getUserId(), targetUserId, false)
                val success = repository.sendFriendRequest(targetUserId)
                
                if (success) {
                    logger.logFriendAction("SEND_REQUEST", networkManager.getUserId(), targetUserId, true)
                    _operationStatus.postValue(Pair(true, "Friend request sent"))
                } else {
                    _operationStatus.postValue(Pair(false, "Failed to send friend request"))
                }
            } catch (e: Exception) {
                logger.logError("FRIEND", "Error sending friend request", e)
                _operationStatus.postValue(Pair(false, "Error: ${e.message}"))
            }
        }
    }
    
    fun acceptFriendRequest(userId: Int, username: String) {
        viewModelScope.launch {
            try {
                logger.logFriendAction("ACCEPT_REQUEST", networkManager.getUserId(), userId, false)
                val success = repository.acceptFriendRequest(userId, username)
                
                if (success) {
                    logger.logFriendAction("ACCEPT_REQUEST", networkManager.getUserId(), userId, true)
                    _operationStatus.postValue(Pair(true, "Friend request accepted"))
                    // Force refresh to clear any stale pending and update friends
                    networkManager.getFriendsList()
                } else {
                    _operationStatus.postValue(Pair(false, "Failed to accept friend request"))
                }
            } catch (e: Exception) {
                logger.logError("FRIEND", "Error accepting friend request", e)
                _operationStatus.postValue(Pair(false, "Error: ${e.message}"))
            }
        }
    }
    
    fun declineFriendRequest(userId: Int) {
        viewModelScope.launch {
            try {
                logger.logFriendAction("DECLINE_REQUEST", networkManager.getUserId(), userId, false)
                val success = repository.declineFriendRequest(userId)
                
                if (success) {
                    logger.logFriendAction("DECLINE_REQUEST", networkManager.getUserId(), userId, true)
                    _operationStatus.postValue(Pair(true, "Friend request declined"))
                } else {
                    _operationStatus.postValue(Pair(false, "Failed to decline friend request"))
                }
            } catch (e: Exception) {
                logger.logError("FRIEND", "Error declining friend request", e)
                _operationStatus.postValue(Pair(false, "Error: ${e.message}"))
            }
        }
    }
    
    fun unfriend(userId: Int) {
        viewModelScope.launch {
            try {
                logger.logFriendAction("UNFRIEND", networkManager.getUserId(), userId, false)
                val success = repository.unfriend(userId)
                
                if (success) {
                    logger.logFriendAction("UNFRIEND", networkManager.getUserId(), userId, true)
                    _operationStatus.postValue(Pair(true, "Friend removed"))
                } else {
                    _operationStatus.postValue(Pair(false, "Failed to remove friend"))
                }
            } catch (e: Exception) {
                logger.logError("FRIEND", "Error unfriending", e)
                _operationStatus.postValue(Pair(false, "Error: ${e.message}"))
            }
        }
    }
    
    fun addPendingFriendRequest(fromUserId: Int, fromUsername: String) {
        viewModelScope.launch {
            try {
                val sessionManager = com.example.myapplication.data.repository.SessionManager.getInstance(getApplication())
                val currentUserId = networkManager.getUserId().takeIf { it != 0 } ?: sessionManager.getUserId()
                withContext(Dispatchers.IO) {
                    // Avoid adding pending if already friends
                    val alreadyFriend = database.friendDao().getFriend(fromUserId) != null
                    if (!alreadyFriend) {
                        val friendRequest = FriendRequest(
                            fromUserId = fromUserId,
                            toUserId = currentUserId,
                            fromUsername = fromUsername,
                            status = RequestStatus.PENDING,
                            timestamp = System.currentTimeMillis()
                        )
                        database.friendRequestDao().insertRequest(friendRequest)
                        logger.log(ActivityLogger.LogLevel.INFO, "FRIEND", "Added pending request from $fromUsername")
                    } else {
                        logger.log(ActivityLogger.LogLevel.INFO, "FRIEND", "Skip pending from $fromUsername because already friend")
                    }
                }
            } catch (e: Exception) {
                logger.logError("FRIEND", "Error adding pending request", e)
            }
        }
    }

    fun clearPendingRequestsForCurrentUser() {
        viewModelScope.launch {
            try {
                val sessionManager = com.example.myapplication.data.repository.SessionManager.getInstance(getApplication())
                val currentUserId = networkManager.getUserId().takeIf { it != 0 } ?: sessionManager.getUserId()
                withContext(Dispatchers.IO) {
                    database.friendRequestDao().clearPendingFor(currentUserId)
                }
            } catch (e: Exception) {
                logger.logError("FRIEND", "Error clearing pending requests", e)
            }
        }
    }

    fun replacePendingRequestsForCurrentUser(requests: List<Pair<Int, String>>) {
        viewModelScope.launch {
            try {
                val sessionManager = com.example.myapplication.data.repository.SessionManager.getInstance(getApplication())
                val currentUserId = networkManager.getUserId().takeIf { it != 0 } ?: sessionManager.getUserId()
                withContext(Dispatchers.IO) {
                    database.friendRequestDao().clearPendingFor(currentUserId)
                    requests.forEach { (fromId, fromName) ->
                        // Skip if already friends (to prevent re-adding accepted requests)
                        val alreadyFriend = database.friendDao().getFriend(fromId) != null
                        if (!alreadyFriend) {
                            val fr = FriendRequest(
                                fromUserId = fromId,
                                toUserId = currentUserId,
                                fromUsername = fromName,
                                status = RequestStatus.PENDING,
                                timestamp = System.currentTimeMillis()
                            )
                            database.friendRequestDao().insertRequest(fr)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.logError("FRIEND", "Error replacing pending requests", e)
            }
        }
    }

    fun replaceFriends(friends: List<Friend>) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.friendDao().clearAll()
                    if (friends.isNotEmpty()) {
                        database.friendDao().insertFriends(friends)
                    }
                }
            } catch (e: Exception) {
                logger.logError("FRIEND", "Error replacing friends list", e)
            }
        }
    }

    fun removePendingRequest(fromUserId: Int) {
        viewModelScope.launch {
            try {
                val sessionManager = com.example.myapplication.data.repository.SessionManager.getInstance(getApplication())
                val currentUserId = networkManager.getUserId().takeIf { it != 0 } ?: sessionManager.getUserId()
                withContext(Dispatchers.IO) {
                    database.friendRequestDao().deleteByUsers(fromUserId, currentUserId)
                }
            } catch (e: Exception) {
                logger.logError("FRIEND", "Error removing pending request", e)
            }
        }
    }
}
