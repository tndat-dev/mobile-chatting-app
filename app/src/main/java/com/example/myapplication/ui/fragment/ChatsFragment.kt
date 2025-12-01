package com.example.myapplication.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.network.NetworkManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.model.ChatConversation
import com.example.myapplication.ui.activity.ChatActivity
import com.example.myapplication.ui.adapter.ChatListAdapter
import com.example.myapplication.ui.viewmodel.ChatViewModel
import com.example.myapplication.databinding.FragmentChatsBinding

class ChatsFragment : Fragment(), NetworkManager.MessageCallback {
    
    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var networkManager: NetworkManager
    private lateinit var adapter: ChatListAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = ChatListAdapter(
            onChatClick = { chat ->
                if (chat.isGroup) {
                    // Navigate to GroupChatActivity
                    val intent = Intent(requireContext(), com.example.myapplication.ui.activity.GroupChatActivity::class.java)
                    intent.putExtra("groupId", chat.groupId)
                    intent.putExtra("groupName", chat.username)
                    startActivity(intent)
                } else {
                    // Navigate to ChatActivity to view full conversation
                    val intent = Intent(requireContext(), ChatActivity::class.java)
                    intent.putExtra("friendId", chat.userId)
                    intent.putExtra("friendName", chat.username)
                    startActivity(intent)
                }
            },
            onChatLongClick = { chat ->
                if (!chat.isGroup) {
                    showDeleteConversationDialog(chat)
                }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.getChatConversations().observe(viewLifecycleOwner) { chats ->
            adapter.submitList(chats)
            binding.tvEmpty.visibility = if (chats.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        // Ensure this fragment receives network callbacks while visible and consume cached messages
        networkManager = NetworkManager.getInstance()
        networkManager.setCallback(this)
        (activity as? com.example.myapplication.ui.activity.MainActivity)?.consumePendingMessages(this)
    }

    override fun onResume() {
        super.onResume()
        networkManager = NetworkManager.getInstance()
        networkManager.setCallback(this)
    }

    override fun onMessageReceived(messageType: Int, payload: String) {
        when (messageType) {
            NetworkManager.MessageType.DIRECT_MESSAGE -> handleIncomingDirectMessage(payload)
            NetworkManager.MessageType.GROUP_MESSAGE -> handleIncomingGroupMessage(payload)
            NetworkManager.MessageType.SUCCESS -> {
                if (payload.startsWith("count=") && payload.contains("&from0=") && payload.contains("&to0=")) {
                    handleConversationHistory(payload)
                }
                if (payload.contains("groupId=") && payload.contains("from0=")) {
                    handleGroupHistorySuccess(payload)
                }
            }
            else -> {
                // ignore other types here
            }
        }
    }

    private fun handleIncomingDirectMessage(payload: String) {
        try {
            val pairs = payload.split("&").mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()

            val senderId = pairs["senderId"]?.toIntOrNull() ?: return
            val message = pairs["message"] ?: return
            val timestamp = pairs["timestamp"]?.toLongOrNull() ?: (System.currentTimeMillis() / 1000)
            val tsMillis = if (timestamp < 2000000000L) timestamp * 1000 else timestamp
            viewModel.receiveMessage(senderId, message, tsMillis)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun handleIncomingGroupMessage(payload: String) {
        try {
            val pairs = payload.split("&").mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()

            val gid = pairs["groupId"]?.toIntOrNull() ?: return
            val senderId = pairs["fromUserId"]?.toIntOrNull() ?: pairs["senderId"]?.toIntOrNull() ?: return
            val message = pairs["message"] ?: return
            val timestamp = pairs["timestamp"]?.toLongOrNull() ?: (System.currentTimeMillis() / 1000)
            val tsMillis = if (timestamp < 2000000000L) timestamp * 1000 else timestamp
            viewModel.receiveMessage(senderId, message, tsMillis, groupId = gid)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun handleConversationHistory(payload: String) {
        try {
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
                viewModel.receiveHistoryMessage(fromUserId, toUserId, content, timestamp)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun handleGroupHistorySuccess(payload: String) {
        try {
            val pairs = payload.split("&").mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()
            val count = pairs["count"]?.toIntOrNull() ?: 0
            val groupId = pairs["groupId"]?.toIntOrNull()
            for (i in 0 until count) {
                val fromUserId = pairs["from$i"]?.toIntOrNull() ?: continue
                val content = pairs["content$i"] ?: continue
                val timestamp = pairs["timestamp$i"]?.toLongOrNull() ?: continue
                val gid = groupId ?: pairs["gid"]?.toIntOrNull()
                if (gid != null) {
                    val tsMillis = if (timestamp < 2000000000L) timestamp * 1000 else timestamp
                    viewModel.receiveMessage(fromUserId, content, tsMillis, groupId = gid)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    
    private fun showDeleteConversationDialog(chat: ChatConversation) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Conversation")
            .setMessage("Delete conversation with ${chat.username}? This will remove all messages.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteConversation(chat.userId)
                android.widget.Toast.makeText(requireContext(), "Conversation deleted", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
