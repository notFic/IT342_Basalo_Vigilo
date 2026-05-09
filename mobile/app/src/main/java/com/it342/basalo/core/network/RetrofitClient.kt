package com.it342.basalo.core.network
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val HOLIDAY_BASE_URL = "https://date.nager.at/api/v3/"

    private fun retrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiConfigManager.getBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    private val holidayRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(HOLIDAY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val visitorApi: VisitorApi
        get() = retrofit().create(VisitorApi::class.java)

    val authApi: AuthApi
        get() = retrofit().create(AuthApi::class.java)

    val adminApi: AdminApi
        get() = retrofit().create(AdminApi::class.java)

    val publicHolidayApi: PublicHolidayApi by lazy {
        holidayRetrofit.create(PublicHolidayApi::class.java)
    }
}
