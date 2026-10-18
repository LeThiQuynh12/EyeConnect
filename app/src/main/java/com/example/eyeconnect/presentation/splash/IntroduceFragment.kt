package com.example.eyeconnect.presentation.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.eyeconnect.R
import com.example.eyeconnect.databinding.FragmentIntroduceBinding

class IntroduceFragment : Fragment() {

    private var _binding: FragmentIntroduceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntroduceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Chọn: Người khiếm thị
        binding.btnNeedHelp.setOnClickListener {
            val bundle = bundleOf("SELECTED_ROLE" to "VISUALLY_IMPAIRED")
            findNavController().navigate(
                R.id.action_introduceFragment_to_loginFragment,
                bundle
            )
        }

        // 2. Chọn: Tình nguyện viên
        binding.btnVolunteer.setOnClickListener {
            val bundle = bundleOf("SELECTED_ROLE" to "VOLUNTEER")
            findNavController().navigate(
                R.id.action_introduceFragment_to_loginFragment,
                bundle
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}