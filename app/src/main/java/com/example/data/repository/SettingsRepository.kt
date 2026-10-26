package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.SearchProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val searchApiKey: String = "",
    val emailApiKey: String = "",
    val searchProvider: SearchProvider = SearchProvider.SERPER,
    val googleCseId: String = "",
    val searchQuery: String = "looking for SEO audit OR \"need an SEO audit\" site:linkedin.com OR site:reddit.com",
    val senderEmail: String = "",
    val autoCreateTasks: Boolean = true
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("autofreelancer_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): AppSettings {
        val providerStr = prefs.getString("search_provider", SearchProvider.SERPER.name) ?: SearchProvider.SERPER.name
        val provider = try {
            SearchProvider.valueOf(providerStr)
        } catch (e: Exception) {
            SearchProvider.SERPER
        }

        return AppSettings(
            searchApiKey = prefs.getString("search_api_key", "").orEmpty(),
            emailApiKey = prefs.getString("email_api_key", "").orEmpty(),
            searchProvider = provider,
            googleCseId = prefs.getString("google_cse_id", "").orEmpty(),
            searchQuery = prefs.getString(
                "search_query",
                "looking for SEO audit OR \"need an SEO audit\" site:linkedin.com OR site:reddit.com"
            ) ?: "looking for SEO audit",
            senderEmail = prefs.getString("sender_email", "").orEmpty(),
            autoCreateTasks = prefs.getBoolean("auto_create_tasks", true)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("search_api_key", settings.searchApiKey)
            .putString("email_api_key", settings.emailApiKey)
            .putString("search_provider", settings.searchProvider.name)
            .putString("google_cse_id", settings.googleCseId)
            .putString("search_query", settings.searchQuery)
            .putString("sender_email", settings.senderEmail)
            .putBoolean("auto_create_tasks", settings.autoCreateTasks)
            .apply()

        _settingsFlow.value = settings
    }
}
