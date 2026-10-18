package com.example.eyeconnect.data.remote

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "VISUALLY_IMPAIRED"
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String // "VISUALLY_IMPAIRED" or "VOLUNTEER"
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val data: UserDto?
)

interface AuthService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
}

object RetrofitClient {
    // 10.0.2.2 trỏ về localhost máy tính khi dùng Android Emulator.
    // Nếu chạy máy thật, đổi thành IPv4 máy tính (ví dụ: "http://192.168.1.15:5000/")
    private const val BASE_URL = "http://192.168.1.6:5000/"

    val authService: AuthService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthService::class.java)
    }
}