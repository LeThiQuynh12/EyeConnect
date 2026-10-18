package com.example.eyeconnect.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.eyeconnect.R
import com.example.eyeconnect.data.remote.RegisterRequest
import com.example.eyeconnect.data.remote.RetrofitClient
import com.example.eyeconnect.databinding.FragmentRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    // Biến nhận vai trò được truyền từ LoginFragment sang
    private var selectedRole: String = "VISUALLY_IMPAIRED"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Nhận Role từ Bundle truyền sang (mặc định VISUALLY_IMPAIRED nếu null)
        selectedRole = arguments?.getString("SELECTED_ROLE") ?: "VISUALLY_IMPAIRED"

        // Quay lại màn đăng nhập (kèm lại Role)
        binding.btnToLogin.setOnClickListener {
            val bundle = bundleOf("SELECTED_ROLE" to selectedRole)
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment, bundle)
        }

        // Bấm Đăng ký
        binding.btnRegister.setOnClickListener {
            val name = binding.etFullName.text?.toString()?.trim().orEmpty()
            val email = binding.etRegisterEmail.text?.toString()?.trim().orEmpty()
            val pass = binding.etRegisterPassword.text?.toString()?.trim().orEmpty()
            val confirmPass = binding.etConfirmPassword.text?.toString()?.trim().orEmpty()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(requireContext(), "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            register(name, email, pass, selectedRole)
        }
    }

    private fun register(name: String, email: String, pass: String, role: String) {
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 2. Gán đúng role nhận được vào request gửi lên backend
                val req = RegisterRequest(
                    name = name,
                    email = email,
                    password = pass,
                    role = role
                )
                val response = RetrofitClient.authService.register(req)

                withContext(Dispatchers.Main) {
                    binding.btnRegister.isEnabled = true
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(requireContext(), "Đăng ký thành công! Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()

                        // 3. Chuyển về màn hình đăng nhập và tiếp tục giữ lại role này
                        val bundle = bundleOf("SELECTED_ROLE" to role)
                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment, bundle)
                    } else {
                        val err = response.body()?.message ?: "Đăng ký thất bại"
                        Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnRegister.isEnabled = true
                    Toast.makeText(requireContext(), "Lỗi kết nối: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}