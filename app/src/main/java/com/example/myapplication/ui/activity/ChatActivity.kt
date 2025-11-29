package com.example.myapplication.ui.activity

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.network.NetworkManager
import com.example.myapplication.ui.adapter.ChatAdapter
import com.example.myapplication.ui.adapter.ChatMessage
import com.google.android.material.appbar.MaterialToolbar
import androidx.activity.viewModels
import com.example.myapplication.ui.viewmodel.ChatViewModel
import com.example.myapplication.data.repository.SessionManager

class ChatActivity : AppCompatActivity() {
    private lateinit var networkManager: NetworkManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: ChatAdapter
    private val messagesList = mutableListOf<ChatMessage>()
    
    private var friendId: Int = 0
    private var friendName: String = ""
    private var myUserId: Int = 0
    private var historyLoaded: Boolean = false
    private val chatViewModel: ChatViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        friendId = intent.getIntExtra("friendId", 0)
        friendName = intent.getStringExtra("friendName") ?: "Friend"
        
        networkManager = NetworkManager.getInstance()
        // Đảm bảo userId đúng từ SessionManager
        val sessionManager = SessionManager.getInstance(this)
        val savedUserId = sessionManager.getUserId()
        if (networkManager.getUserId() == 0 && savedUserId > 0) {
            networkManager.setUserId(savedUserId)
        }
        myUserId = networkManager.getUserId()
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = friendName
        
        recyclerView = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ChatAdapter(messagesList)
        recyclerView.adapter = adapter
        
        setupObservers()
        setupNetworkCallback()
        setupSendButton()
    }

    private fun setupObservers() {
        // Load conversation history from server only once
        if (!historyLoaded) {
            chatViewModel.loadConversationHistory(friendId)
        }
        
        // Quan sát tin nhắn từ Room để hiển thị lịch sử bền vững
        chatViewModel.getMessages(myUserId, friendId).observe(this) { list ->
            messagesList.clear()
            list.forEach { m ->
                val isMine = m.senderId == myUserId
                val tsSec = if (m.timestamp > 2000000000L) m.timestamp / 1000 else m.timestamp
                messagesList.add(
                    ChatMessage(
                        senderId = m.senderId,
                        message = m.content,
                        timestamp = tsSec,
                        isMine = isMine
                    )
                )
            }
            adapter.notifyDataSetChanged()
            if (messagesList.isNotEmpty()) recyclerView.scrollToPosition(messagesList.size - 1)
        }
    }
    
    private fun setupNetworkCallback() {
        networkManager.setCallback(object : NetworkManager.MessageCallback {
            override fun onMessageReceived(messageType: Int, payload: String) {
                runOnUiThread {
                    when (messageType) {
                        NetworkManager.MessageType.DIRECT_MESSAGE -> handleIncomingMessage(payload)
                        NetworkManager.MessageType.SUCCESS -> {
                            android.util.Log.d("ChatActivity", "SUCCESS received: historyLoaded=$historyLoaded, payload=$payload")
                            // Conversation history is handled separately, not from generic SUCCESS
                            // Only handle conversation history if it contains the specific pattern AND we haven't loaded it yet
                            if (!historyLoaded && payload.startsWith("count=") && payload.contains("&from0=") && payload.contains("&to0=")) {
                                android.util.Log.d("ChatActivity", "Processing conversation history")
                                handleConversationHistory(payload)
                                historyLoaded = true
                            }
                        }
                        NetworkManager.MessageType.ERROR -> {
                            Toast.makeText(this@ChatActivity, "Error: $payload", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
    
    private fun setupSendButton() {
        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                etMessage.text.clear()
            }
        }
    }
    
    private fun sendMessage(message: String) {
        // Lưu và gửi qua ViewModel/Repository để có lịch sử bền vững
        chatViewModel.sendMessage(friendId, message)
    }
    
    private fun handleIncomingMessage(payload: String) {
        val pairs = payload.split("&").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
        
        val senderId = pairs["senderId"]?.toIntOrNull() ?: return
        val message = pairs["message"] ?: return
        val timestamp = pairs["timestamp"]?.toLongOrNull() ?: (System.currentTimeMillis() / 1000)
        
        // Lưu vào DB, UI tự cập nhật qua LiveData
        if (senderId == friendId) {
            val tsMillis = if (timestamp < 2000000000L) timestamp * 1000 else timestamp
            chatViewModel.receiveMessage(senderId, message, tsMillis)
        }
    }
    
    private fun handleConversationHistory(payload: String) {
        val pairs = payload.split("&").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
        
        val count = pairs["count"]?.toIntOrNull() ?: 0
        
        for (i in 0 until count) {
            val fromUserId = pairs["from$i"]?.toIntOrNull() ?: continue
            val toUserId = pairs["to$i"]?.toIntOrNull() ?: continue
            val content = pairs["content$i"] ?: continue
            val timestamp = pairs["timestamp$i"]?.toLongOrNull() ?: continue
            
            // Deduplicated history insertion
            chatViewModel.receiveHistoryMessage(fromUserId, toUserId, content, timestamp)
        }
        
        if (count > 0) {
            Toast.makeText(this, "Loaded $count messages from history", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
