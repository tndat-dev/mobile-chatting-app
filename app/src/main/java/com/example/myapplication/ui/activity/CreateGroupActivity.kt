package com.example.myapplication.ui.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityCreateGroupBinding
import com.example.myapplication.ui.adapter.FriendSelectAdapter
import com.example.myapplication.ui.viewmodel.FriendViewModel
import com.example.myapplication.ui.viewmodel.GroupViewModel

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding
    private val friendViewModel: FriendViewModel by viewModels()
    private val groupViewModel: GroupViewModel by viewModels()
    private lateinit var adapter: FriendSelectAdapter
    private var selectedMemberIds = setOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnCreate.setOnClickListener {
            createGroup()
        }
        
        // Initially disabled
        updateCreateButtonState(false)
    }

    private fun setupRecyclerView() {
        adapter = FriendSelectAdapter { selectedIds ->
            selectedMemberIds = selectedIds
            updateCreateButtonState(selectedIds.isNotEmpty())
        }

        binding.rvFriends.apply {
            this.adapter = this@CreateGroupActivity.adapter
            layoutManager = LinearLayoutManager(this@CreateGroupActivity)
        }
    }

    private var allFriends = listOf<com.example.myapplication.data.model.Friend>()
    
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.lowercase() ?: ""
                filterFriends(query)
            }
        })
    }
    
    private fun filterFriends(query: String) {
        if (query.isEmpty()) {
            adapter.submitList(allFriends)
        } else {
            val filtered = allFriends.filter { friend ->
                friend.username.lowercase().contains(query)
            }
            adapter.submitList(filtered)
        }
    }

    private fun observeViewModel() {
        // Observe friends list
        friendViewModel.getAllFriends().observe(this) { friends ->
            allFriends = friends
            adapter.submitList(friends)
        }

        // Observe group creation result
        groupViewModel.operationStatus.observe(this) { (success, message) ->
            if (success) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCreateButtonState(enabled: Boolean) {
        binding.btnCreate.apply {
            isEnabled = enabled
            isClickable = enabled
            setTextColor(
                if (enabled) {
                    ContextCompat.getColor(this@CreateGroupActivity, android.R.color.holo_blue_dark)
                } else {
                    ContextCompat.getColor(this@CreateGroupActivity, android.R.color.darker_gray)
                }
            )
        }
    }

    private fun createGroup() {
        val groupName = binding.etGroupName.text.toString().trim()
        val memberIds = selectedMemberIds.toList()

        if (memberIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one member", Toast.LENGTH_SHORT).show()
            return
        }

        val finalGroupName = if (groupName.isEmpty()) {
            "Group ${System.currentTimeMillis()}"
        } else {
            groupName
        }

        groupViewModel.createGroup(finalGroupName, memberIds)
    }
}
