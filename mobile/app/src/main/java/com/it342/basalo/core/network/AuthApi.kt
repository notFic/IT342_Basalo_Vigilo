package com.it342.basalo.core.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    fun loginUser(
        @Body request: LoginRequest
    ): Call<AuthResponse>

    @POST("auth/register")
    fun registerUser(
        @Body request: RegisterRequest
    ): Call<AuthResponse>
}
