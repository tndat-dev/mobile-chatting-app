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
import com.example.myapplication.ui.adapter.GroupMessage
import com.example.myapplication.ui.adapter.GroupMessageAdapter
import com.example.myapplication.data.repository.SessionManager
import com.google.android.material.appbar.MaterialToolbar

class GroupChatActivity : AppCompatActivity() {
    private lateinit var networkManager: NetworkManager
    private lateinit var sessionManager: SessionManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: GroupMessageAdapter
    private val messagesList = mutableListOf<GroupMessage>()
    
    private var groupId: Int = 0
    private var groupName: String = ""
    private var myUserId: Int = 0
    private var myUsername: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)
        
        networkManager = NetworkManager.getInstance()
        sessionManager = SessionManager.getInstance(this)
        
        groupId = intent.getIntExtra("groupId", 0)
        groupName = intent.getStringExtra("groupName") ?: "Group"
        myUserId = sessionManager.getUserId()
        myUsername = sessionManager.getUsername() ?: "Me"
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = groupName
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        recyclerView = findViewById(R.id.recyclerView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        
        adapter = GroupMessageAdapter(messagesList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        setupNetworkCallback()
        setupSendButton()
    }
    
    private fun setupNetworkCallback() {
        networkManager.setCallback(object : NetworkManager.MessageCallback {
            override fun onMessageReceived(messageType: Int, payload: String) {
                runOnUiThread {
                    when (messageType) {
                        NetworkManager.MessageType.GROUP_MESSAGE -> handleIncomingMessage(payload)
                        NetworkManager.MessageType.SUCCESS -> {
                            // Message sent successfully
                        }
                        NetworkManager.MessageType.ERROR -> {
                            Toast.makeText(this@GroupChatActivity, "Error: $payload", Toast.LENGTH_SHORT).show()
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
        if (networkManager.sendGroupMessage(groupId, message)) {
            val groupMessage = GroupMessage(
                senderId = myUserId,
                senderName = myUsername,
                message = message,
                timestamp = System.currentTimeMillis() / 1000,
                isMine = true
            )
            messagesList.add(groupMessage)
            adapter.notifyItemInserted(messagesList.size - 1)
            recyclerView.scrollToPosition(messagesList.size - 1)
        } else {
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleIncomingMessage(payload: String) {
        val pairs = payload.split("&").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
        
        val msgGroupId = pairs["groupId"]?.toIntOrNull() ?: return
        if (msgGroupId != groupId) return // Not for this group
        
        val senderId = pairs["senderId"]?.toIntOrNull() ?: return
        val message = pairs["message"] ?: return
        val timestamp = pairs["timestamp"]?.toLongOrNull() ?: (System.currentTimeMillis() / 1000)
        
        // Don't show our own messages again (already added in sendMessage)
        if (senderId != myUserId) {
            val senderName = "User $senderId" // TODO: Get actual username
            val groupMessage = GroupMessage(
                senderId = senderId,
                senderName = senderName,
                message = message,
                timestamp = timestamp,
                isMine = false
            )
            messagesList.add(groupMessage)
            adapter.notifyItemInserted(messagesList.size - 1)
            recyclerView.scrollToPosition(messagesList.size - 1)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
