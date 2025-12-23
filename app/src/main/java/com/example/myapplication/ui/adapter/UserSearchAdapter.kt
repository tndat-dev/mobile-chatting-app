package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemUserSearchBinding
import com.example.myapplication.data.model.User

class UserSearchAdapter(
    private val onAddClick: (User) -> Unit
) : ListAdapter<User, UserSearchAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserSearchBinding.inflate(
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
        private val binding: ItemUserSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(user: User) {
            binding.tvUsername.text = user.username
            binding.tvEmail.text = user.email
            
            binding.btnAdd.setOnClickListener {
                onAddClick(user)
            }
        }
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
