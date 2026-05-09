package com.it342.basalo.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Android emulator localhost mapping for Spring Boot backend.
    private const val BASE_URL = "http://10.0.2.2:8080/api/v1/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val visitorApi: VisitorApi by lazy {
        retrofit.create(VisitorApi::class.java)
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }
}
