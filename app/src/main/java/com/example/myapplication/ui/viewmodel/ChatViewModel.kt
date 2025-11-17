package com.example.myapplication.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.database.ChatDatabase
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.repository.ChatRepository
import com.example.myapplication.network.NetworkManager
import com.example.myapplication.utils.ActivityLogger
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
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
        networkManager
    )
    
    private val _currentChatUser = MutableLiveData<Int>()
    val currentChatUser: LiveData<Int> = _currentChatUser
    
    private val _currentGroup = MutableLiveData<Int>()
    val currentGroup: LiveData<Int> = _currentGroup
    
    private val _sendMessageStatus = MutableLiveData<Boolean>()
    val sendMessageStatus: LiveData<Boolean> = _sendMessageStatus
    
    fun setCurrentChatUser(userId: Int) {
        _currentChatUser.value = userId
    }
    
    fun setCurrentGroup(groupId: Int) {
        _currentGroup.value = groupId
    }
    
    fun getMessages(currentUserId: Int, otherUserId: Int): LiveData<List<Message>> {
        return repository.getConversationMessages(currentUserId, otherUserId)
    }
    
    fun getGroupMessages(groupId: Int): LiveData<List<Message>> {
        return repository.getGroupMessages(groupId)
    }
    
    fun sendMessage(recipientId: Int, content: String) {
        viewModelScope.launch {
            try {
                logger.logMessage("SEND", networkManager.getUserId(), recipientId)
                val success = repository.sendMessage(recipientId, content)
                _sendMessageStatus.postValue(success)
                
                if (success) {
                    logger.log(ActivityLogger.LogLevel.INFO, "CHAT", "Message sent to user $recipientId")
                }
            } catch (e: Exception) {
                logger.logError("CHAT", "Error sending message", e)
                _sendMessageStatus.postValue(false)
            }
        }
    }
    
    fun sendGroupMessage(groupId: Int, content: String) {
        viewModelScope.launch {
            try {
                logger.logGroupAction("SEND_MESSAGE", groupId, networkManager.getUserId(), true)
                val success = repository.sendGroupMessage(groupId, content)
                _sendMessageStatus.postValue(success)
                
                if (success) {
                    logger.log(ActivityLogger.LogLevel.INFO, "GROUP", "Message sent to group $groupId")
                }
            } catch (e: Exception) {
                logger.logError("GROUP", "Error sending group message", e)
                _sendMessageStatus.postValue(false)
            }
        }
    }
    
    fun markMessagesAsRead(otherUserId: Int) {
        viewModelScope.launch {
            repository.markMessagesAsRead(otherUserId)
        }
    }

    fun receiveMessage(senderId: Int, content: String, timestamp: Long, groupId: Int? = null) {
        viewModelScope.launch {
            repository.receiveMessage(senderId, content, timestamp, groupId)
        }
    }
}
