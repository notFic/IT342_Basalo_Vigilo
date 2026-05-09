package com.it342.basalo.network

import com.it342.basalo.data.VisitorRecord
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface VisitorApi {
    @GET("logs/active")
    fun getActiveLogs(): Call<List<VisitorRecord>>

    @GET("logs/history")
    fun getHistoricalLogs(@Query("page") page: Int = 0, @Query("size") size: Int = 50): Call<HistoricalResponse>

    @Multipart
    @POST("logs/check-in")
    fun checkInVisitor(
        @Part("fullName") fullName: RequestBody,
        @Part("contactNumber") contactNumber: RequestBody,
        @Part("hostName") hostName: RequestBody,
        @Part("visitorType") visitorType: RequestBody,
        @Part("destinationRoom") destinationRoom: RequestBody,
        @Part("purpose") purpose: RequestBody,
        @Part("extendedVisit") extendedVisit: Boolean,
        @Part("createdByEmail") createdByEmail: RequestBody,
        @Part idImage: MultipartBody.Part
    ): Call<VisitorRecord>

    @PUT("logs/{logId}/check-out")
    fun checkOutVisitor(@Path("logId") logId: Int, @Query("updatedByEmail") updatedByEmail: String): Call<VisitorRecord>

    @PUT("logs/{logId}/void")
    fun voidVisitorLog(@Path("logId") logId: Int, @Query("updatedByEmail") updatedByEmail: String): Call<VisitorRecord>
}

data class HistoricalResponse(
    val content: List<VisitorRecord>
)
