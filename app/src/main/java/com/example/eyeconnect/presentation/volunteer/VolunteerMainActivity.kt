package com.example.eyeconnect.presentation.volunteer

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.eyeconnect.MainActivity
import com.example.eyeconnect.R
import com.example.eyeconnect.databinding.ActivityVolunteerMainBinding

class VolunteerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVolunteerMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVolunteerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()
        setupBottomNavTabs()
        setupClickEvents()
    }

    /**
     * 1. Hiển thị thông tin tên và trạng thái tài khoản từ SharedPreferences
     */
    private fun loadUserData() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("NAME", "Tình nguyện viên")

        binding.tvVolunteerName.text = userName
        binding.tvJoinDate.text = "Sẵn sàng hỗ trợ trực tuyến"
    }

    /**
     * 2. Xử lý logic chọn 3 Tab thủ công: Trang chủ, Danh bạ, Cài đặt
     */
    private fun setupBottomNavTabs() {
        binding.tabHome.setOnClickListener {
            selectTab(isHome = true, isDirectory = false, isSettings = false)
            // Hiển thị giao diện trang chủ
            binding.scrollVolunteerContent.visibility = android.view.View.VISIBLE
        }

        binding.tabDirectory.setOnClickListener {
            selectTab(isHome = false, isDirectory = true, isSettings = false)
            // Tạm thời hiển thị Toast (hoặc mở Fragment/Activity danh bạ sau)
            Toast.makeText(this, "Tính năng Danh bạ đang hoàn thiện", Toast.LENGTH_SHORT).show()
        }

        binding.tabSettings.setOnClickListener {
            selectTab(isHome = false, isDirectory = false, isSettings = true)
            // Xử lý Cài đặt / Đăng xuất
            showSettingsOrLogoutDialog()
        }
    }

    /**
     * Cập nhật màu sắc của các Tab khi được click
     */
    private fun selectTab(isHome: Boolean, isDirectory: Boolean, isSettings: Boolean) {
        val activeColor = ContextCompat.getColor(this, R.color.main)
        val inactiveColor = Color.parseColor("#78858B")

        // Tab Trang chủ
        binding.ivTabHome.setColorFilter(if (isHome) activeColor else inactiveColor)
        binding.tvTabHome.setTextColor(if (isHome) activeColor else inactiveColor)

        // Tab Danh bạ
        binding.ivTabDirectory.setColorFilter(if (isDirectory) activeColor else inactiveColor)
        binding.tvTabDirectory.setTextColor(if (isDirectory) activeColor else inactiveColor)

        // Tab Cài đặt
        binding.ivTabSettings.setColorFilter(if (isSettings) activeColor else inactiveColor)
        binding.tvTabSettings.setTextColor(if (isSettings) activeColor else inactiveColor)
    }

    /**
     * 3. Các sự kiện bấm khác trong màn hình
     */
    private fun setupClickEvents() {
        // Nút hướng dẫn trả lời cuộc gọi
        binding.btnGuideCall.setOnClickListener {
            Toast.makeText(
                this,
                "Hãy giữ bình tĩnh, mô tả rõ ràng vật thể, văn bản hoặc phương hướng cho người khiếm thị.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Hộp thoại Đăng xuất để xóa Session và quay lại màn hình chính
     */
    private fun showSettingsOrLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Tài khoản Tình nguyện viên")
            .setMessage("Bạn có muốn đăng xuất khỏi tài khoản này?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                // Xóa token và session
                getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().apply()

                // Điều hướng về MainActivity (Splash/Login)
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Hủy") { _, _ ->
                // Quay lại tab Home
                selectTab(isHome = true, isDirectory = false, isSettings = false)
            }
            .show()
    }
}