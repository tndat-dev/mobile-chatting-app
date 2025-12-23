package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemFriendBinding
import com.example.myapplication.data.model.Friend

class FriendsAdapter(
    private val onItemClick: (Friend) -> Unit,
    private val onUnfriendClick: (Friend) -> Unit
) : ListAdapter<Friend, FriendsAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemFriendBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(friend: Friend) {
            binding.tvUsername.text = friend.username
            binding.tvStatus.text = if (friend.isOnline) "Online" else "Offline"
            
            // Set indicator visibility and color based on online status
            binding.viewOnlineIndicator.visibility = android.view.View.VISIBLE
            val indicatorDrawable = if (friend.isOnline) {
                R.drawable.indicator_online
            } else {
                R.drawable.indicator_offline
            }
            binding.viewOnlineIndicator.background = ContextCompat.getDrawable(
                binding.root.context,
                indicatorDrawable
            )
            
            binding.root.setOnClickListener {
                onItemClick(friend)
            }
            
            binding.btnUnfriend.setOnClickListener {
                onUnfriendClick(friend)
            }
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<Friend>() {
        override fun areItemsTheSame(oldItem: Friend, newItem: Friend): Boolean {
            return oldItem.userId == newItem.userId
        }
        
        override fun areContentsTheSame(oldItem: Friend, newItem: Friend): Boolean {
            return oldItem == newItem
        }
    }
}
