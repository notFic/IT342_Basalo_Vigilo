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
    val data: AuthDataPayload? = null
)

data class AuthDataPayload(
    val id: Long?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val role: String?,
    val user: UserPayload?,
    val accessToken: String?
) {
    fun getUserPayload(): UserPayload? {
        if (user != null) return user
        if (id != null && email != null && firstName != null && lastName != null && role != null) {
            return UserPayload(id, email, firstName, lastName, role)
        }
        return null
    }
}

data class UserPayload(
    val id: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String
)
