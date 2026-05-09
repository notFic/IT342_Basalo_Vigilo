package com.it342.basalo.core.network

import com.it342.basalo.core.data.AuditRecord
import com.it342.basalo.core.data.LocationRecord
import com.it342.basalo.core.data.SystemUser
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AdminApi {
    @GET("users")
    fun getUsers(): Call<List<SystemUser>>

    @GET("locations")
    fun getLocations(): Call<List<LocationRecord>>

    @POST("locations")
    fun addLocation(@Body request: LocationRequest): Call<LocationRecord>

    @GET("audit")
    fun getAuditLogs(): Call<List<AuditRecord>>
}

data class LocationRequest(
    val areaName: String,
    val roomNumber: String,
    val floorLevel: String
)
