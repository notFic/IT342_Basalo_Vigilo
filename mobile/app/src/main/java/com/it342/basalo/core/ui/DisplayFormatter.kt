package com.it342.basalo.core.ui

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DisplayFormatter {
    private val outputFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    fun formatDateTime(value: String?): String {
        if (value.isNullOrBlank()) return "N/A"
        return try {
            LocalDateTime.parse(value).format(outputFormatter)
        } catch (_: Exception) {
            value
        }
    }

    fun formatStatus(status: String?): String {
        if (status.isNullOrBlank()) return "Unknown"
        return status
    }
}
