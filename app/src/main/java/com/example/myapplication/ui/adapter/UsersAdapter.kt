package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

data class UserItem(
    val userId: Int,
    val username: String,
    val isFriend: Boolean,
    val isOnline: Boolean = false
)

class UsersAdapter(
    private val users: List<UserItem>,
    private val onUserClick: (UserItem) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {
    
    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val viewOnlineIndicator: View = view.findViewById(R.id.viewOnlineIndicator)
        
        fun bind(user: UserItem) {
            tvUsername.text = user.username
            tvStatus.text = if (user.isFriend) "✓ Friend" else "Add Friend"
            tvStatus.setTextColor(
                if (user.isFriend) 
                    itemView.context.getColor(android.R.color.holo_green_dark)
                else 
                    itemView.context.getColor(android.R.color.holo_blue_dark)
            )
            
            // Set online indicator
            val indicatorDrawable = if (user.isOnline) {
                R.drawable.indicator_online
            } else {
                R.drawable.indicator_offline
            }
            viewOnlineIndicator.background = ContextCompat.getDrawable(
                itemView.context,
                indicatorDrawable
            )
            
            itemView.setOnClickListener {
                onUserClick(user)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }
    
    override fun getItemCount() = users.size
}
