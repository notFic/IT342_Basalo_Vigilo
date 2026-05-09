package com.it342.basalo.core.data

import android.content.Context

class SessionManager(context: Context) {

    private val sharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(token: String, profile: StaffProfile) {
        sharedPreferences.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, profile.id)
            .putString(KEY_FIRST_NAME, profile.firstName)
            .putString(KEY_LAST_NAME, profile.lastName)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_ROLE, RoleUtils.normalize(profile.role))
            .apply()
    }

    fun getToken(): String? = sharedPreferences.getString(KEY_TOKEN, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank() && getProfile() != null

    fun getProfile(): StaffProfile? {
        val id = getStoredUserId()
        val firstName = sharedPreferences.getString(KEY_FIRST_NAME, null)
        val lastName = sharedPreferences.getString(KEY_LAST_NAME, null)
        val email = sharedPreferences.getString(KEY_EMAIL, null)
        val role = sharedPreferences.getString(KEY_ROLE, null)

        return if (id != -1L && firstName != null && lastName != null && email != null && role != null) {
            StaffProfile(
                id = id,
                firstName = firstName,
                lastName = lastName,
                email = email,
                role = RoleUtils.normalize(role)
            )
        } else {
            null
        }
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }

    private fun getStoredUserId(): Long {
        return try {
            sharedPreferences.getLong(KEY_USER_ID, -1L)
        } catch (_: ClassCastException) {
            val legacyId = sharedPreferences.getInt(KEY_USER_ID, -1)
            if (legacyId != -1) {
                sharedPreferences.edit().putLong(KEY_USER_ID, legacyId.toLong()).apply()
                legacyId.toLong()
            } else {
                -1L
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "VigiloPrefs"
        private const val KEY_TOKEN = "JWT_TOKEN"
        private const val KEY_USER_ID = "USER_ID"
        private const val KEY_FIRST_NAME = "FIRST_NAME"
        private const val KEY_LAST_NAME = "LAST_NAME"
        private const val KEY_EMAIL = "EMAIL"
        private const val KEY_ROLE = "ROLE"
    }
}
