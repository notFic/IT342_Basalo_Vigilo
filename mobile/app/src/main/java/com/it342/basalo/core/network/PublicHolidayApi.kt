package com.it342.basalo.core.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

data class PublicHolidayDto(
    val date: String,
    val name: String
)

interface PublicHolidayApi {
    @GET("PublicHolidays/{year}/PH")
    fun getPhilippinePublicHolidays(
        @Path("year") year: Int
    ): Call<List<PublicHolidayDto>>
}
