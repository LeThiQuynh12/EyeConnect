package com.example.eyeconnect.presentation.auth

import android.content.Context
import android.content.Intent
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
import com.example.eyeconnect.data.remote.LoginRequest
import com.example.eyeconnect.data.remote.RetrofitClient
import com.example.eyeconnect.databinding.FragmentLoginBinding
import com.example.eyeconnect.presentation.volunteer.VolunteerMainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Mặc định là VISUALLY_IMPAIRED nếu không có dữ liệu truyền sang
    private var selectedRole: String = "VISUALLY_IMPAIRED"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy Role được chọn từ màn IntroduceFragment
        selectedRole = arguments?.getString("SELECTED_ROLE") ?: "VISUALLY_IMPAIRED"

        // Chuyển sang màn Đăng ký (truyền tiếp vai trò đã chọn sang RegisterFragment)
        binding.btnToRegister.setOnClickListener {
            val bundle = bundleOf("SELECTED_ROLE" to selectedRole)
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment, bundle)
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(email, password)
        }
    }

    private fun login(email: String, pass: String) {
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.authService.login(LoginRequest(email, pass))
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()!!
                        val user = body.data
                        val actualRole = user?.role ?: selectedRole

                        // Lưu Session
                        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
                            .putString("TOKEN", body.token)
                            .putString("USER_ID", user?.id)
                            .putString("ROLE", actualRole)
                            .putString("NAME", user?.name)
                            .apply()

                        Toast.makeText(requireContext(), "Xin chào, ${user?.name}!", Toast.LENGTH_SHORT).show()

                        // Điều hướng dựa theo Role thực tế của tài khoản từ server
                        if (actualRole == "VOLUNTEER") {
                            // 1. Chuyển sang màn hình Tình nguyện viên (Activity riêng)
                            val intent = Intent(
                                requireContext(),
                                VolunteerMainActivity::class.java
                            ).apply {
                                // Xoá ngăn xếp MainActivity cũ để khi bấm nút Back không bị quay lại màn hình Login
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            requireActivity().finish()
                        } else {
                            // 2. Người khiếm thị: Tiếp tục trong MainActivity (Dùng NavController vào màn Hỗ trợ hoặc AI)
                            findNavController().navigate(R.id.action_loginFragment_to_nav_support)
                        }

                    } else {
                        val err = response.body()?.message ?: "Đăng nhập thất bại"
                        Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
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