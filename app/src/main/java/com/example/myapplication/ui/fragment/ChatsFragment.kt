package com.example.myapplication.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.ui.activity.ChatActivity
import com.example.myapplication.ui.adapter.ChatListAdapter
import com.example.myapplication.ui.viewmodel.ChatViewModel
import com.example.myapplication.databinding.FragmentChatsBinding

class ChatsFragment : Fragment() {
    
    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatListAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = ChatListAdapter { chat ->
            // Navigate to ChatActivity to view full conversation
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("friendId", chat.userId)
            intent.putExtra("friendName", chat.username)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.getChatConversations().observe(viewLifecycleOwner) { chats ->
            adapter.submitList(chats)
            binding.tvEmpty.visibility = if (chats.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
