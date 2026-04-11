package com.it342.basalo.data

enum class VisitorStatus {
    ACTIVE,
    CHECKED_OUT,
    AUTO_CLOSED,
    VOIDED
}

data class StaffProfile(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String
) {
    val fullName: String
        get() = "$firstName $lastName"
}

data class HolidayStatus(
    val isHoliday: Boolean,
    val holidayName: String?
)

data class VisitorRecord(
    val logId: Int,
    val fullName: String,
    val contactNumber: String,
    val hostName: String,
    val visitorType: String,
    val destination: String,
    val purpose: String,
    val isExtendedVisit: Boolean,
    val idAttachmentLabel: String?,
    val timeIn: String,
    val timeOut: String? = null,
    val status: VisitorStatus = VisitorStatus.ACTIVE,
    val createdBy: String = "",
    val updatedBy: String? = null
)
