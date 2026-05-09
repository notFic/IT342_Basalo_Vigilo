package com.it342.basalo.core.network

import android.content.Context
import com.it342.basalo.BuildConfig

object ApiConfigManager {
    private const val PREFS_NAME = "VigiloApiConfig"
    private const val KEY_BASE_URL = "BASE_URL"

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun getBaseUrl(): String {
        val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return normalize(preferences.getString(KEY_BASE_URL, BuildConfig.API_BASE_URL))
    }

    fun saveBaseUrl(value: String) {
        val normalized = normalize(value)
        val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString(KEY_BASE_URL, normalized).apply()
    }

    fun resetBaseUrl() {
        saveBaseUrl(BuildConfig.API_BASE_URL)
    }

    fun isEmulatorAlias(): Boolean = getBaseUrl().contains("10.0.2.2")

    private fun normalize(value: String?): String {
        var normalized = value?.trim().orEmpty()
        if (normalized.isBlank()) normalized = BuildConfig.API_BASE_URL
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        if (!normalized.endsWith("/")) {
            normalized += "/"
        }
        return normalized
    }
}
