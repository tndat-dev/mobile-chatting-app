package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.ChatConversation
import com.example.myapplication.databinding.ItemChatBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val onChatClick: (ChatConversation) -> Unit,
    private val onChatLongClick: (ChatConversation) -> Unit
) : ListAdapter<ChatConversation, ChatListAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding, onChatClick, onChatLongClick)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatViewHolder(
        private val binding: ItemChatBinding,
        private val onChatClick: (ChatConversation) -> Unit,
        private val onChatLongClick: (ChatConversation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatConversation) {
            binding.apply {
                tvUsername.text = chat.username
                tvLastMessage.text = chat.lastMessage ?: ""
                if (chat.lastMessageTime != null && chat.lastMessageTime > 0L) {
                    tvTime.visibility = android.view.View.VISIBLE
                    tvTime.text = formatTime(chat.lastMessageTime!!)
                } else {
                    tvTime.visibility = android.view.View.GONE
                }
                
                // Show read/unread status
                if (chat.hasUnread) {
                    tvReadStatus.isVisible = true
                    tvReadStatus.text = "UNREAD"
                    tvReadStatus.setBackgroundColor(0xFF2196F3.toInt()) // Blue color for unread
                } else {
                    tvReadStatus.isVisible = false
                }
                
                root.setOnClickListener {
                    onChatClick(chat)
                }
                
                root.setOnLongClickListener {
                    onChatLongClick(chat)
                    true
                }
            }
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60000 -> "Just now" // Less than 1 minute
                diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
                diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp)) // Today
                diff < 604800000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp)) // This week
                else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp)) // Older
            }
        }
    }

    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatConversation>() {
        override fun areItemsTheSame(oldItem: ChatConversation, newItem: ChatConversation): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: ChatConversation, newItem: ChatConversation): Boolean {
            return oldItem == newItem
        }
    }
}
