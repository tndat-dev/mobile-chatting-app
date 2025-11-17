package com.example.myapplication.ui.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val senderId: Int,
    val message: String,
    val timestamp: Long,
    val isMine: Boolean
)

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    
    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val messageContainer: LinearLayout = view.findViewById(R.id.messageContainer)
        
        fun bind(message: ChatMessage) {
            tvMessage.text = message.message
            tvTimestamp.text = formatTimestamp(message.timestamp)
            
            val layoutParams = messageContainer.layoutParams as LinearLayout.LayoutParams
            if (message.isMine) {
                layoutParams.gravity = Gravity.END
                messageContainer.setBackgroundResource(R.drawable.bg_message_sent)
            } else {
                layoutParams.gravity = Gravity.START
                messageContainer.setBackgroundResource(R.drawable.bg_message_received)
            }
            messageContainer.layoutParams = layoutParams
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp * 1000))
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount() = messages.size
}
