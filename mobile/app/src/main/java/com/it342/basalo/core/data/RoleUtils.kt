package com.it342.basalo.core.data

object RoleUtils {
    fun normalize(role: String?): String {
        val normalized = role?.trim()?.uppercase().orEmpty()
        return when {
            normalized.contains("ADMIN") -> "ADMIN"
            normalized.contains("STAFF") -> "STAFF"
            normalized.isBlank() -> "STAFF"
            else -> normalized
        }
    }

    fun isAdmin(role: String?): Boolean = normalize(role) == "ADMIN"
}
