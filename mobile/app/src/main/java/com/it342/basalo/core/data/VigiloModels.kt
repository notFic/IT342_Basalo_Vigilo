package com.it342.basalo.core.data

import com.google.gson.annotations.SerializedName

data class StaffProfile(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String
) {
    val fullName: String
        get() = "$firstName $lastName"

    val normalizedRole: String
        get() = RoleUtils.normalize(role)
}

data class HolidayStatus(
    val isHoliday: Boolean,
    val holidayName: String?
)

data class VisitorRecord(
    @SerializedName("id")
    val id: Long,
    val fullName: String,
    val contactNumber: String,
    val hostName: String,
    val visitorType: String,
    @SerializedName("destinationRoom")
    val destinationRoom: String,
    val purpose: String,
    @SerializedName("extendedVisit")
    val extendedVisit: Boolean,
    @SerializedName("idImagePath")
    val idImagePath: String?,
    val timeIn: String,
    val timeOut: String? = null,
    val status: String = "Active",
    @SerializedName("createdByEmail")
    val createdByEmail: String = "",
    @SerializedName("updatedByEmail")
    val updatedByEmail: String? = null
)

data class SystemUser(
    val id: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val createdAt: String? = null
) {
    val fullName: String
        get() = "$firstName $lastName"

    val normalizedRole: String
        get() = RoleUtils.normalize(role)
}

data class LocationRecord(
    val id: Long,
    val areaName: String,
    val roomNumber: String,
    val floorLevel: String
) {
    val displayName: String
        get() = "$roomNumber - $areaName"
}

data class AuditRecord(
    val id: Long,
    val timestamp: String,
    val userEmail: String,
    val actionPerformed: String,
    val details: String
)
