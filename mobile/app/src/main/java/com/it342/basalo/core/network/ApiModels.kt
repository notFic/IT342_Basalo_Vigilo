package com.it342.basalo.core.network

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val role: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val data: UserPayload? = null
)

data class UserPayload(
    val id: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String
)
