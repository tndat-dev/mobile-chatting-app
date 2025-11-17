package com.example.myapplication.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentGroupsBinding
import com.example.myapplication.ui.adapter.GroupsAdapter
import com.example.myapplication.ui.adapter.Group
import com.example.myapplication.ui.viewmodel.GroupViewModel
import com.example.myapplication.ui.activity.GroupChatActivity
import com.google.android.material.textfield.TextInputEditText

class GroupsFragment : Fragment() {
    
    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GroupViewModel by viewModels()
    private lateinit var adapter: GroupsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        adapter = GroupsAdapter(
            onItemClick = { group ->
                val intent = Intent(requireContext(), GroupChatActivity::class.java)
                intent.putExtra("groupId", group.groupId)
                intent.putExtra("groupName", group.groupName)
                startActivity(intent)
            },
            onLeaveClick = { group ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Leave Group")
                    .setMessage("Are you sure you want to leave ${group.groupName}?")
                    .setPositiveButton("Leave") { _, _ ->
                        viewModel.leaveGroup(group.groupId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        
        binding.recyclerView.apply {
            this.adapter = this@GroupsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupFab() {
        binding.fabCreateGroup.setOnClickListener {
            showCreateGroupDialog()
        }
    }
    
    private fun showCreateGroupDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(com.example.myapplication.R.layout.dialog_create_group, null)
        val etGroupName = dialogView.findViewById<TextInputEditText>(com.example.myapplication.R.id.etGroupName)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Create Group")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val groupName = etGroupName.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    // Create with no members initially
                    viewModel.createGroup(groupName, emptyList())
                } else {
                    Toast.makeText(requireContext(), "Please enter group name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun observeViewModel() {
        // Observe groups from database
        viewModel.getAllGroups().observe(viewLifecycleOwner) { dbGroups ->
            val groups = dbGroups.map { dbGroup ->
                com.example.myapplication.ui.adapter.Group(
                    groupId = dbGroup.id,
                    groupName = dbGroup.name,
                    memberCount = dbGroup.memberCount
                )
            }
            adapter.submitList(groups)
            binding.tvEmpty.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
        }
        
        // Observe operation status
        viewModel.operationStatus.observe(viewLifecycleOwner) { (success, message) ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
