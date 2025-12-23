package com.example.myapplication.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentProfileBinding
import com.example.myapplication.data.repository.SessionManager
import com.example.myapplication.ui.activity.MainActivity

class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var sessionManager: SessionManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sessionManager = SessionManager.getInstance(requireContext())
        
        binding.tvUsername.text = sessionManager.getUsername()
        binding.tvUserId.text = "ID: ${sessionManager.getUserId()}"
        
        binding.btnLogout.setOnClickListener {
            (activity as? MainActivity)?.logout()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
