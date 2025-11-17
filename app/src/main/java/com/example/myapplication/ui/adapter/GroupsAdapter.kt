package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemGroupBinding

data class Group(
    val groupId: Int,
    val groupName: String,
    val memberCount: Int = 0
)

class GroupsAdapter(
    private val onItemClick: (Group) -> Unit,
    private val onLeaveClick: (Group) -> Unit
) : ListAdapter<Group, GroupsAdapter.GroupViewHolder>(GroupDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class GroupViewHolder(
        private val binding: ItemGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(group: Group) {
            binding.tvGroupName.text = group.groupName
            binding.tvMemberCount.text = if (group.memberCount > 0) 
                "${group.memberCount} members" 
            else 
                "Group"
            
            binding.root.setOnClickListener {
                onItemClick(group)
            }
            
            binding.btnLeave.setOnClickListener {
                onLeaveClick(group)
            }
        }
    }
    
    private class GroupDiffCallback : DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem.groupId == newItem.groupId
        }
        
        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem == newItem
        }
    }
}
