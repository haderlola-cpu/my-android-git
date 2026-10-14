package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppPreferences(
    val isLoggedIn: Boolean = false,
    val adminUsername: String = "admin",
    val adminPassword: String = "admin",
    val languageCode: String = "ar",
    val originalExportQuality: Boolean = false,
    val continuousScan: Boolean = true
)

class PreferencesRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("elbeit_settings_prefs", Context.MODE_PRIVATE)

    private val _preferencesFlow = MutableStateFlow(loadPreferences())
    val preferencesFlow: StateFlow<AppPreferences> = _preferencesFlow.asStateFlow()

    private fun loadPreferences(): AppPreferences {
        return AppPreferences(
            isLoggedIn = prefs.getBoolean("is_logged_in", false),
            adminUsername = prefs.getString("admin_username", "admin") ?: "admin",
            adminPassword = prefs.getString("admin_password", "admin") ?: "admin",
            languageCode = prefs.getString("language_code", "ar") ?: "ar",
            originalExportQuality = prefs.getBoolean("original_export_quality", false),
            continuousScan = prefs.getBoolean("continuous_scan", true)
        )
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean("is_logged_in", loggedIn).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(isLoggedIn = loggedIn)
    }

    fun toggleLanguage(current: String) {
        val next = if (current == "ar") "en" else "ar"
        setLanguage(next)
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language_code", lang).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(languageCode = lang)
    }

    fun setOriginalExportQuality(enabled: Boolean) {
        prefs.edit().putBoolean("original_export_quality", enabled).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(originalExportQuality = enabled)
    }

    fun setContinuousScan(enabled: Boolean) {
        prefs.edit().putBoolean("continuous_scan", enabled).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(continuousScan = enabled)
    }

    fun updateAdminPassword(newPassword: String) {
        if (newPassword.isNotBlank()) {
            val clean = newPassword.trim()
            prefs.edit().putString("admin_password", clean).apply()
            _preferencesFlow.value = _preferencesFlow.value.copy(adminPassword = clean)
        }
    }
}
