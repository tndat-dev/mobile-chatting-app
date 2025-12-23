package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.model.Message
import com.example.myapplication.databinding.ItemMessageReceivedBinding
import com.example.myapplication.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val currentUserId: Int
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {
    
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
    
    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding)
            }
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }
    
    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: Message) {
            binding.tvMessage.text = message.content
            binding.tvTimestamp.text = formatTime(message.timestamp)
        }
    }
    
    inner class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: Message) {
            binding.tvMessage.text = message.content
            binding.tvTimestamp.text = formatTime(message.timestamp)
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
