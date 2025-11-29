package com.example.myapplication.network

import android.util.Log

/**
 * NetworkManager using C++ JNI for all networking operations
 * This replaces the pure Kotlin socket implementation with native C++ code
 */
class NetworkManager private constructor() {
    
    companion object {
        private const val TAG = "NetworkManager"
        
        init {
            try {
                System.loadLibrary("chatapp")
                Log.d(TAG, "Native library 'chatapp' loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library 'chatapp'", e)
                throw e
            }
        }
        
        @Volatile
        private var instance: NetworkManager? = null
        
        fun getInstance(): NetworkManager {
            return instance ?: synchronized(this) {
                instance ?: NetworkManager().also { 
                    instance = it
                    it.nativeInit()
                }
            }
        }
    }
    
    // Message types matching C++ enum
    object MessageType {
        const val REGISTER = 0x01
        const val LOGIN = 0x02
        const val LOGOUT = 0x03
        const val SEARCH_USER = 0x04
        const val GET_ALL_USERS = 0x05
        const val FRIEND_REQUEST = 0x10
        const val FRIEND_ACCEPT = 0x11
        const val FRIEND_DECLINE = 0x12
        const val UNFRIEND = 0x13
        const val GET_FRIENDS_LIST = 0x14
        const val DIRECT_MESSAGE = 0x20
        const val MESSAGE_RECEIVED = 0x21
        const val TYPING_STATUS = 0x22
        const val GET_CONVERSATION_HISTORY = 0x23
        const val CREATE_GROUP = 0x30
        const val INVITE_TO_GROUP = 0x31
        const val REMOVE_FROM_GROUP = 0x32
        const val LEAVE_GROUP = 0x33
        const val GROUP_MESSAGE = 0x34
        const val USER_ONLINE = 0x40
        const val USER_OFFLINE = 0x41
        const val SUCCESS = 0xF0
        const val ERROR = 0xF1
        const val HEARTBEAT = 0xFF
    }
    
    interface MessageCallback {
        fun onMessageReceived(messageType: Int, payload: String)
    }
    
    private var callback: MessageCallback? = null
    private var currentUserId: Int = 0
    
    init {
        // nativeInit() will be called in getInstance()
    }
    
    // Native methods - implemented in C++
    private external fun nativeInit()
    private external fun nativeDestroy()
    private external fun nativeSetCallback(callback: MessageCallback)
    private external fun nativeConnect(host: String, port: Int): Boolean
    private external fun nativeDisconnect()
    private external fun nativeIsConnected(): Boolean
    private external fun nativeSendMessage(messageType: Int, payload: String, userId: Int): Boolean
    private external fun nativeSha256(data: String): String
    private external fun nativeSerializeLogin(username: String, password: String): String
    private external fun nativeSerializeRegister(username: String, password: String, email: String): String
    private external fun nativeSerializeChatMessage(recipientId: Int, message: String): String
    
    fun setCallback(callback: MessageCallback) {
        this.callback = callback
        nativeSetCallback(callback)
    }
    
    fun connect(host: String, port: Int): Boolean {
        Log.d(TAG, "Connecting to $host:$port via JNI")
        return nativeConnect(host, port)
    }
    
    fun disconnect() {
        Log.d(TAG, "Disconnecting via JNI")
        nativeDisconnect()
    }
    
    fun isConnected(): Boolean {
        return nativeIsConnected()
    }
    
    fun setUserId(userId: Int) {
        currentUserId = userId
        Log.d(TAG, "User ID set to $userId")
    }
    
    fun getUserId(): Int {
        return currentUserId
    }
    
    private fun sendMessage(messageType: Int, payload: String): Boolean {
        if (!isConnected()) {
            Log.e(TAG, "Not connected")
            return false
        }
        
        Log.d(TAG, "Sending via JNI: type=$messageType, payload=$payload")
        return nativeSendMessage(messageType, payload, currentUserId)
    }
    
    // High-level API methods
    fun login(username: String, password: String): Boolean {
        val hashedPassword = sha256(password)
        val payload = serializeLogin(username, hashedPassword)
        return sendMessage(MessageType.LOGIN, payload)
    }
    
    fun register(username: String, password: String, email: String, phone: String = ""): Boolean {
        val hashedPassword = sha256(password)
        val payload = serializeRegister(username, hashedPassword, email, phone)
        return sendMessage(MessageType.REGISTER, payload)
    }
    
    fun sendChatMessage(recipientId: Int, message: String): Boolean {
        val payload = serializeChatMessage(recipientId, message)
        return sendMessage(MessageType.DIRECT_MESSAGE, payload)
    }
    
    fun sendGroupMessage(groupId: Int, message: String): Boolean {
        val payload = "groupId=$groupId&message=$message"
        return sendMessage(MessageType.GROUP_MESSAGE, payload)
    }
    
    fun sendFriendRequest(friendUsername: String): Boolean {
        val payload = "username=$friendUsername"
        return sendMessage(MessageType.FRIEND_REQUEST, payload)
    }
    
    fun acceptFriendRequest(friendId: Int): Boolean {
        val payload = "userId=$friendId"
        return sendMessage(MessageType.FRIEND_ACCEPT, payload)
    }
    
    fun declineFriendRequest(friendId: Int): Boolean {
        val payload = "userId=$friendId"
        return sendMessage(MessageType.FRIEND_DECLINE, payload)
    }
    
    fun unfriend(friendId: Int): Boolean {
        val payload = "userId=$friendId"
        return sendMessage(MessageType.UNFRIEND, payload)
    }
    
    fun getFriendsList(): Boolean {
        return sendMessage(MessageType.GET_FRIENDS_LIST, "")
    }
    
    fun getConversationHistory(otherUserId: Int, limit: Int = 50): Boolean {
        val payload = "otherUserId=$otherUserId&limit=$limit"
        return sendMessage(MessageType.GET_CONVERSATION_HISTORY, payload)
    }
    
    fun getAllUsers(): Boolean {
        return sendMessage(MessageType.GET_ALL_USERS, "")
    }
    
    fun searchUsers(query: String): Boolean {
        val payload = "query=$query"
        return sendMessage(MessageType.SEARCH_USER, payload)
    }
    
    fun createGroup(groupName: String, memberIds: List<Int>): Boolean {
        val members = memberIds.joinToString(",")
        val payload = "name=$groupName&members=$members"
        return sendMessage(MessageType.CREATE_GROUP, payload)
    }
    
    fun inviteToGroup(groupId: Int, userId: Int): Boolean {
        val payload = "groupId=$groupId&userId=$userId"
        return sendMessage(MessageType.INVITE_TO_GROUP, payload)
    }
    
    fun removeFromGroup(groupId: Int, userId: Int): Boolean {
        val payload = "groupId=$groupId&userId=$userId"
        return sendMessage(MessageType.REMOVE_FROM_GROUP, payload)
    }
    
    fun leaveGroup(groupId: Int): Boolean {
        val payload = "groupId=$groupId"
        return sendMessage(MessageType.LEAVE_GROUP, payload)
    }
    
    fun sendTypingStatus(recipientId: Int, isTyping: Boolean): Boolean {
        val payload = "recipientId=$recipientId&typing=${if (isTyping) "1" else "0"}"
        return sendMessage(MessageType.TYPING_STATUS, payload)
    }
    
    fun sendHeartbeat(): Boolean {
        return sendMessage(MessageType.HEARTBEAT, "")
    }
    
    fun logout(): Boolean {
        val result = sendMessage(MessageType.LOGOUT, "")
        disconnect()
        currentUserId = 0
        return result
    }
    
    // Utility methods using native implementations
    fun sha256(input: String): String {
        return nativeSha256(input)
    }
    
    private fun serializeLogin(username: String, hashedPassword: String): String {
        return nativeSerializeLogin(username, hashedPassword)
    }
    
    private fun serializeRegister(username: String, hashedPassword: String, email: String, phone: String = ""): String {
        return "username=$username&password=$hashedPassword&email=$email&phone=$phone"
    }
    
    private fun serializeChatMessage(recipientId: Int, message: String): String {
        return nativeSerializeChatMessage(recipientId, message)
    }
    
    // Cleanup when done
    fun destroy() {
        disconnect()
        nativeDestroy()
    }
}
