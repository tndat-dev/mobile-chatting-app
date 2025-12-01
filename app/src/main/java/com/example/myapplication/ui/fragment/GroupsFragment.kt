package com.example.myapplication.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentGroupsBinding
import com.example.myapplication.ui.adapter.GroupsAdapter
import com.example.myapplication.ui.adapter.Group
import com.example.myapplication.ui.viewmodel.GroupViewModel
import com.example.myapplication.ui.activity.GroupChatActivity
import com.example.myapplication.ui.activity.CreateGroupActivity

class GroupsFragment : Fragment(), com.example.myapplication.network.NetworkManager.MessageCallback {
    
    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GroupViewModel by viewModels()
    private lateinit var adapter: GroupsAdapter
    private lateinit var networkManager: com.example.myapplication.network.NetworkManager
    private var requestedUserList: Boolean = false
    
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
        networkManager = com.example.myapplication.network.NetworkManager.getInstance()
        // Ensure this fragment receives network callbacks while visible
        networkManager.setCallback(this)
        // Ask server for groups proactively so Room is populated if needed
        try {
            if (networkManager.isConnected()) {
                networkManager.getUserGroups()
            }
        } catch (e: Exception) {
            // ignore
        }
        // Consume any pending messages cached in MainActivity (so we don't miss an earlier GET_USER_GROUPS)
        (activity as? com.example.myapplication.ui.activity.MainActivity)?.consumePendingMessages(this)
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    override fun onMessageReceived(messageType: Int, payload: String) {
        // Handle only group-list success payloads here; other types are ignored
        if (messageType == com.example.myapplication.network.NetworkManager.MessageType.SUCCESS) {
            if (payload.contains("groupId0=") && payload.contains("name0=")) {
                // Reuse the same persistence logic as MainActivity.handleGroupListPayload
                try {
                    val pairs = payload.split("&").mapNotNull {
                        val parts = it.split("=", limit = 2)
                        if (parts.size == 2) parts[0] to parts[1] else null
                    }.toMap()

                    val count = pairs["count"]?.toIntOrNull() ?: 0
                    // Move DB operations to IO coroutine since DAO methods are suspend
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = com.example.myapplication.data.database.ChatDatabase.getDatabase(requireContext())
                        for (i in 0 until count) {
                            val gid = pairs["groupId$i"]?.toIntOrNull() ?: continue
                            val gname = pairs["name$i"] ?: continue
                            val membersCsv = pairs["members$i"] ?: ""
                            val memberIds = if (membersCsv.isNotEmpty()) membersCsv.split(',').mapNotNull { it.toIntOrNull() } else emptyList()
                            val serverMemberCount = pairs["memberCount$i"]?.toIntOrNull()

                            val group = com.example.myapplication.data.model.Group(
                                id = gid,
                                name = gname,
                                creatorId = com.example.myapplication.data.repository.SessionManager.getInstance(requireContext()).getUserId(),
                                memberCount = serverMemberCount ?: if (memberIds.size > 0) memberIds.size else 1
                            )
                            try { db.groupDao().insertGroup(group) } catch (e: Exception) { }

                            for (uid in memberIds) {
                                val uname = try {
                                    val f = db.friendDao().getFriend(uid)
                                    f?.username ?: db.userDao().getUser(uid)?.username ?: "User $uid"
                                } catch (e: Exception) { "User $uid" }
                                val member = com.example.myapplication.data.model.GroupMember(
                                    groupId = gid,
                                    userId = uid,
                                    username = uname,
                                    nickname = null,
                                    isAdmin = (uid == com.example.myapplication.data.repository.SessionManager.getInstance(requireContext()).getUserId()),
                                    joinedAt = System.currentTimeMillis()
                                )
                                try { db.groupMemberDao().insertMember(member) } catch (e: Exception) { }
                            }

                            try {
                                if (networkManager.isConnected()) networkManager.getGroupHistory(gid, 100)
                            } catch (e: Exception) { }

                            try {
                                val myId = com.example.myapplication.data.repository.SessionManager.getInstance(requireContext()).getUserId()
                                val existing = db.groupMemberDao().getMember(gid, myId)
                                if (existing == null) {
                                    val myMember = com.example.myapplication.data.model.GroupMember(
                                        groupId = gid,
                                        userId = myId,
                                        username = com.example.myapplication.data.repository.SessionManager.getInstance(requireContext()).getUsername() ?: "Me",
                                        nickname = null,
                                        isAdmin = false,
                                        joinedAt = System.currentTimeMillis()
                                    )
                                    db.groupMemberDao().insertMember(myMember)
                                }
                            } catch (e: Exception) { }
                        }
                    }
                } catch (e: Exception) {
                    // ignore parse errors
                }
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = GroupsAdapter(
            onItemClick = { group ->
                val intent = Intent(requireContext(), GroupChatActivity::class.java)
                intent.putExtra("groupId", group.groupId)
                intent.putExtra("groupName", group.groupName)
                startActivity(intent)
            }
        )
        
        binding.recyclerView.apply {
            this.adapter = this@GroupsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupFab() {
        binding.fabCreateGroup.setOnClickListener {
            val intent = Intent(requireContext(), CreateGroupActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun observeViewModel() {
        // Observe groups from database
        viewModel.getAllGroups().observe(viewLifecycleOwner) { dbGroups ->
            val groups = dbGroups.map { dbGroup ->
                Group(
                    groupId = dbGroup.id,
                    groupName = dbGroup.name,
                    memberCount = dbGroup.memberCount
                )
            }
            adapter.submitList(groups)
            binding.tvEmpty.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
            // If any group member usernames are placeholders (e.g. "User {id}"), request full user list
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    if (groups.isEmpty() || requestedUserList) return@launch
                    val db = com.example.myapplication.data.database.ChatDatabase.getDatabase(requireContext())
                    var needFetch = false
                    for (g in dbGroups) {
                        val members = db.groupMemberDao().getMembersList(g.id)
                        for (m in members) {
                            if (m.username.startsWith("User ")) {
                                needFetch = true
                                break
                            }
                        }
                        if (needFetch) break
                    }
                    if (needFetch) {
                        requestedUserList = true
                        if (!this@GroupsFragment::networkManager.isInitialized) {
                            networkManager = com.example.myapplication.network.NetworkManager.getInstance()
                        }
                        if (networkManager.isConnected()) {
                            try { networkManager.getAllUsers() } catch (e: Exception) { }
                            try { networkManager.getUserGroups() } catch (e: Exception) { }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
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
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            // Attempt to refresh group list from ViewModel's LiveData
            viewModel.getAllGroups().value?.let { dbGroups ->
                val groups = dbGroups.map { dbGroup ->
                    Group(
                        groupId = dbGroup.id,
                        groupName = dbGroup.name,
                        memberCount = dbGroup.memberCount
                    )
                }
                adapter.submitList(groups)
            }
        }
    }
}
