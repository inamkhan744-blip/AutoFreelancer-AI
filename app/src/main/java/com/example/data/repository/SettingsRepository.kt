package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val geminiApiKey: String = "",
    val openRouterApiKey: String = "",
    val serperApiKey: String = "",
    val tavilyApiKey: String = "",
    val searchQuery: String = "looking for SEO audit OR \"need an SEO audit\" site:linkedin.com OR site:reddit.com OR site:x.com",
    val emailApiKey: String = "",
    val autoRunOnApproval: Boolean = true
) {
    val isGeminiConfigured: Boolean
        get() = geminiApiKey.isNotBlank()

    val isOpenRouterConfigured: Boolean
        get() = openRouterApiKey.isNotBlank()

    val isAiConfigured: Boolean
        get() = isGeminiConfigured || isOpenRouterConfigured

    val isSearchConfigured: Boolean
        get() = serperApiKey.isNotBlank() || tavilyApiKey.isNotBlank()
}

class SettingsRepository(context: Context) {
    private val tag = "SettingsRepository"
    private val securePrefs: SharedPreferences = createEncryptedPreferences(context)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun createEncryptedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "autofreelancer_secure_keys",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(tag, "EncryptedSharedPreferences unavailable, falling back to standard storage: ${e.message}")
            context.getSharedPreferences("autofreelancer_secure_keys_fallback", Context.MODE_PRIVATE)
        }
    }

    private fun loadSettings(): AppSettings {
        return AppSettings(
            geminiApiKey = securePrefs.getString("gemini_api_key", "").orEmpty(),
            openRouterApiKey = securePrefs.getString("openrouter_api_key", "").orEmpty(),
            serperApiKey = securePrefs.getString("serper_api_key", "").orEmpty(),
            tavilyApiKey = securePrefs.getString("tavily_api_key", "").orEmpty(),
            searchQuery = securePrefs.getString(
                "search_query",
                "looking for SEO audit OR \"need an SEO audit\" site:linkedin.com OR site:reddit.com OR site:x.com"
            ) ?: "looking for SEO audit",
            emailApiKey = securePrefs.getString("email_api_key", "").orEmpty(),
            autoRunOnApproval = securePrefs.getBoolean("auto_run_approval", true)
        )
    }

    fun saveSettings(settings: AppSettings) {
        securePrefs.edit()
            .putString("gemini_api_key", settings.geminiApiKey.trim())
            .putString("openrouter_api_key", settings.openRouterApiKey.trim())
            .putString("serper_api_key", settings.serperApiKey.trim())
            .putString("tavily_api_key", settings.tavilyApiKey.trim())
            .putString("search_query", settings.searchQuery.trim())
            .putString("email_api_key", settings.emailApiKey.trim())
            .putBoolean("auto_run_approval", settings.autoRunOnApproval)
            .remove("groq_api_key")
            .remove("groq_model")
            .apply()

        _settingsFlow.value = settings
    }
}
