package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.model.GroupMember

class MemberNicknameAdapter(
    private val items: List<GroupMember>,
    private val onEdit: (GroupMember) -> Unit
) : RecyclerView.Adapter<MemberNicknameAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        val tvNickname: TextView = view.findViewById(R.id.tvNickname)
        val tvRealName: TextView = view.findViewById(R.id.tvRealName)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_member_nickname, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val gm = items[position]
        // Placeholder avatar; real app may load from profile
        holder.ivAvatar.setImageResource(R.drawable.ic_user_placeholder)
        holder.tvNickname.text = gm.nickname ?: gm.username
        holder.tvRealName.text = gm.username
        holder.btnEdit.setOnClickListener { onEdit(gm) }
    }

    override fun getItemCount(): Int = items.size
}
