package com.it342.basalo.data

import android.content.Context

class SessionManager(context: Context) {

    private val sharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(token: String, profile: StaffProfile) {
        sharedPreferences.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, profile.id)
            .putString(KEY_FIRST_NAME, profile.firstName)
            .putString(KEY_LAST_NAME, profile.lastName)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_ROLE, profile.role)
            .apply()
    }

    fun getToken(): String? = sharedPreferences.getString(KEY_TOKEN, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun getProfile(): StaffProfile? {
        val id = sharedPreferences.getInt(KEY_USER_ID, -1)
        val firstName = sharedPreferences.getString(KEY_FIRST_NAME, null)
        val lastName = sharedPreferences.getString(KEY_LAST_NAME, null)
        val email = sharedPreferences.getString(KEY_EMAIL, null)
        val role = sharedPreferences.getString(KEY_ROLE, null)

        return if (id != -1 && firstName != null && lastName != null && email != null && role != null) {
            StaffProfile(
                id = id,
                firstName = firstName,
                lastName = lastName,
                email = email,
                role = role
            )
        } else {
            null
        }
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
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
