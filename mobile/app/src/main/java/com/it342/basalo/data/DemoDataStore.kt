package com.it342.basalo.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DemoDataStore {

    private val formatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    private val activeLogs = mutableListOf(
        VisitorRecord(
            logId = 101,
            fullName = "Marlon Reyes",
            contactNumber = "09171234567",
            hostName = "Unit 204 - J. Cruz",
            visitorType = "Guest",
            destination = "Room 204",
            purpose = "Family visit",
            isExtendedVisit = false,
            idAttachmentLabel = "drivers_license.jpg",
            timeIn = "Mar 28, 2026 • 08:15 AM",
            createdBy = "Basalo Guard"
        ),
        VisitorRecord(
            logId = 102,
            fullName = "Janine Torres",
            contactNumber = "09984561230",
            hostName = "Unit 112 - M. Ramos",
            visitorType = "Courier",
            destination = "Room 112",
            purpose = "Parcel delivery",
            isExtendedVisit = false,
            idAttachmentLabel = "delivery_id.png",
            timeIn = "Mar 28, 2026 • 09:05 AM",
            createdBy = "Basalo Guard"
        ),
        VisitorRecord(
            logId = 103,
            fullName = "Ruel Mendoza",
            contactNumber = "09199887766",
            hostName = "Maintenance Office",
            visitorType = "Contractor",
            destination = "Utility Area",
            purpose = "Aircon repair",
            isExtendedVisit = true,
            idAttachmentLabel = "contractor_badge.jpeg",
            timeIn = "Mar 28, 2026 • 10:20 AM",
            createdBy = "Basalo Guard"
        )
    )

    private val historicalLogs = mutableListOf(
        VisitorRecord(
            logId = 90,
            fullName = "Angela de Vera",
            contactNumber = "09170000001",
            hostName = "Unit 315 - P. Santos",
            visitorType = "Guest",
            destination = "Room 315",
            purpose = "Study group",
            isExtendedVisit = false,
            idAttachmentLabel = "school_id.jpg",
            timeIn = "Mar 27, 2026 • 03:10 PM",
            timeOut = "Mar 27, 2026 • 05:42 PM",
            status = VisitorStatus.CHECKED_OUT,
            createdBy = "Basalo Guard",
            updatedBy = "Basalo Guard"
        ),
        VisitorRecord(
            logId = 91,
            fullName = "Neil Bautista",
            contactNumber = "09170000002",
            hostName = "Unit 120 - L. Perez",
            visitorType = "Contractor",
            destination = "Room 120",
            purpose = "Internet installation",
            isExtendedVisit = false,
            idAttachmentLabel = "work_permit.png",
            timeIn = "Mar 27, 2026 • 01:00 PM",
            timeOut = "Mar 27, 2026 • 11:59 PM",
            status = VisitorStatus.AUTO_CLOSED,
            createdBy = "Basalo Guard",
            updatedBy = "SYSTEM"
        ),
        VisitorRecord(
            logId = 92,
            fullName = "Paolo Mendoza",
            contactNumber = "09170000003",
            hostName = "Unit 205 - A. Lim",
            visitorType = "Guest",
            destination = "Room 205",
            purpose = "Duplicate entry",
            isExtendedVisit = false,
            idAttachmentLabel = "passport.jpg",
            timeIn = "Mar 26, 2026 • 06:15 PM",
            timeOut = "Mar 26, 2026 • 06:20 PM",
            status = VisitorStatus.VOIDED,
            createdBy = "Night Shift Guard",
            updatedBy = "Admin Angela"
        )
    )

    private var nextLogId = 104

    fun getHolidayStatus(): HolidayStatus {
        return HolidayStatus(
            isHoliday = true,
            holidayName = "Founding Anniversary Restricted Access"
        )
    }

    fun getActiveLogs(query: String = ""): List<VisitorRecord> {
        return activeLogs
            .filter { it.fullName.contains(query, ignoreCase = true) }
            .sortedByDescending { it.logId }
    }

    fun getHistoricalLogs(query: String = ""): List<VisitorRecord> {
        return historicalLogs
            .filter { it.fullName.contains(query, ignoreCase = true) }
            .sortedByDescending { it.logId }
    }

    fun addVisitor(
        fullName: String,
        contactNumber: String,
        hostName: String,
        visitorType: String,
        destination: String,
        purpose: String,
        isExtendedVisit: Boolean,
        idAttachmentLabel: String?,
        createdBy: String
    ): VisitorRecord {
        val record = VisitorRecord(
            logId = nextLogId++,
            fullName = fullName,
            contactNumber = contactNumber,
            hostName = hostName,
            visitorType = visitorType,
            destination = destination,
            purpose = purpose,
            isExtendedVisit = isExtendedVisit,
            idAttachmentLabel = idAttachmentLabel,
            timeIn = formatter.format(Date()),
            createdBy = createdBy
        )
        activeLogs.add(0, record)
        return record
    }

    fun checkOut(logId: Int, updatedBy: String): VisitorRecord? {
        val item = activeLogs.firstOrNull { it.logId == logId } ?: return null
        activeLogs.remove(item)
        val updated = item.copy(
            status = VisitorStatus.CHECKED_OUT,
            timeOut = formatter.format(Date()),
            updatedBy = updatedBy
        )
        historicalLogs.add(0, updated)
        return updated
    }

    fun voidRecord(logId: Int, updatedBy: String): VisitorRecord? {
        val fromHistory = historicalLogs.indexOfFirst { it.logId == logId }
        if (fromHistory != -1) {
            val updated = historicalLogs[fromHistory].copy(
                status = VisitorStatus.VOIDED,
                updatedBy = updatedBy
            )
            historicalLogs[fromHistory] = updated
            return updated
        }

        val fromActive = activeLogs.indexOfFirst { it.logId == logId }
        if (fromActive != -1) {
            val removed = activeLogs.removeAt(fromActive)
            val updated = removed.copy(
                status = VisitorStatus.VOIDED,
                updatedBy = updatedBy
            )
            historicalLogs.add(0, updated)
            return updated
        }

        return null
    }
}
