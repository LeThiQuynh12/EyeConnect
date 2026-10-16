package com.example.eyeconnect.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.eyeconnect.databinding.FragmentCallVolunteerBinding

class SupportFragment : Fragment() {

    private var _binding: FragmentCallVolunteerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallVolunteerBinding.inflate(inflater, container, false)
        return binding.root
    }
}


