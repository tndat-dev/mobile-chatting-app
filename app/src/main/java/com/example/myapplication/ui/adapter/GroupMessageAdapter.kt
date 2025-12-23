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

data class GroupMessage(
    val senderId: Int,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val isMine: Boolean,
    val isSystemMessage: Boolean = false
)

class GroupMessageAdapter(
    private val messages: List<GroupMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    companion object {
        private const val VIEW_TYPE_MESSAGE = 0
        private const val VIEW_TYPE_SYSTEM = 1
    }
    
    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSenderName: TextView = view.findViewById(R.id.tvSenderName)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val messageContainer: LinearLayout = view.findViewById(R.id.messageContainer)
        
        fun bind(message: GroupMessage) {
            tvMessage.text = message.message
            tvTimestamp.text = formatTimestamp(message.timestamp)
            
            val layoutParams = messageContainer.layoutParams as LinearLayout.LayoutParams
            if (message.isMine) {
                layoutParams.gravity = Gravity.END
                messageContainer.setBackgroundResource(R.drawable.bg_message_sent)
                // For own messages we do not show the sender name (message only)
                tvSenderName.visibility = View.GONE
            } else {
                layoutParams.gravity = Gravity.START
                messageContainer.setBackgroundResource(R.drawable.bg_message_received)
                tvSenderName.visibility = View.VISIBLE
                tvSenderName.text = message.senderName
                // align sender name to start for others
                tvSenderName.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            }
            messageContainer.layoutParams = layoutParams
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
    
    inner class SystemMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSystemMessage: TextView = view.findViewById(R.id.tvSystemMessage)
        
        fun bind(message: GroupMessage) {
            tvSystemMessage.text = message.message
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSystemMessage) VIEW_TYPE_SYSTEM else VIEW_TYPE_MESSAGE
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SYSTEM) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_system_message, parent, false)
            SystemMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_group_message, parent, false)
            MessageViewHolder(view)
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MessageViewHolder -> holder.bind(messages[position])
            is SystemMessageViewHolder -> holder.bind(messages[position])
        }
    }
    
    override fun getItemCount() = messages.size
}
