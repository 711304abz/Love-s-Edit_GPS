package com.example.lovesedit_gps

import android.content.Context

object PrefsManager {
    private const val PREFS_NAME = "location_bridge_prefs"
    private const val KEY_PROVIDER = "provider_type"
    private const val KEY_API_KEY = "api_key"

    fun saveConfig(context: Context, provider: String, apiKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROVIDER, provider)
            .putString(KEY_API_KEY, apiKey)
            .apply()
    }

    fun getProvider(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROVIDER, "AMAP") ?: "AMAP"
    }

    fun getApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API_KEY, "") ?: ""
    }
}