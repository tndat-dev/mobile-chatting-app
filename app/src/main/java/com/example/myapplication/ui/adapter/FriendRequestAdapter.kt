package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemFriendRequestBinding
import com.example.myapplication.data.model.FriendRequest

class FriendRequestAdapter(
    private val onAcceptClick: (FriendRequest) -> Unit,
    private val onDeclineClick: (FriendRequest) -> Unit
) : ListAdapter<FriendRequest, FriendRequestAdapter.ViewHolder>(FriendRequestDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFriendRequestBinding.inflate(
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
        private val binding: ItemFriendRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(request: FriendRequest) {
            binding.tvUsername.text = request.fromUsername
            binding.tvSubtext.text = "wants to be friends"
            
            binding.btnAccept.setOnClickListener {
                onAcceptClick(request)
            }
            
            binding.btnDecline.setOnClickListener {
                onDeclineClick(request)
            }
        }
    }
    
    private class FriendRequestDiffCallback : DiffUtil.ItemCallback<FriendRequest>() {
        override fun areItemsTheSame(oldItem: FriendRequest, newItem: FriendRequest): Boolean {
            return oldItem.fromUserId == newItem.fromUserId && oldItem.toUserId == newItem.toUserId
        }
        
        override fun areContentsTheSame(oldItem: FriendRequest, newItem: FriendRequest): Boolean {
            return oldItem == newItem
        }
    }
}
