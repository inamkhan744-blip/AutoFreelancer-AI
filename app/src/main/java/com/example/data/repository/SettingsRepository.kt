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
    val geminiModel: String = "gemini-3.8-flash",
    val openRouterModel: String = "meta-llama/llama-3.3-70b-instruct:free",
    val serperApiKey: String = "",
    val tavilyApiKey: String = "",
    val resendApiKey: String = "",
    val paymentLinkUrl: String = "",
    val autoPilotEnabled: Boolean = false,
    val searchQuery: String = "looking for SEO audit OR \"need an SEO audit\" site:linkedin.com OR site:reddit.com OR site:x.com",
    val autoRunOnApproval: Boolean = true,
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "21m00Tcm4TlvDq8ikWAM", // Rachel default voice
    val inboundEmailWebhookUrl: String = ""
) {
    val isGeminiConfigured: Boolean
        get() = geminiApiKey.isNotBlank()

    val isOpenRouterConfigured: Boolean
        get() = openRouterApiKey.isNotBlank()

    val isAiConfigured: Boolean
        get() = isGeminiConfigured || isOpenRouterConfigured

    val isSearchConfigured: Boolean
        get() = serperApiKey.isNotBlank() || tavilyApiKey.isNotBlank()

    val isResendConfigured: Boolean
        get() = resendApiKey.isNotBlank()

    val isPaymentLinkConfigured: Boolean
        get() = paymentLinkUrl.isNotBlank()

    val isElevenLabsConfigured: Boolean
        get() = elevenLabsApiKey.isNotBlank()

    val isWebhookConfigured: Boolean
        get() = inboundEmailWebhookUrl.isNotBlank()
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
        val rawGemini = securePrefs.getString("gemini_model", "gemini-3.8-flash").orEmpty()
        val activeGemini = if (rawGemini.isBlank() || rawGemini == "gemini-1.5-flash") {
            "gemini-3.8-flash"
        } else {
            rawGemini
        }

        val rawOpenRouter = securePrefs.getString("openrouter_model", "meta-llama/llama-3.3-70b-instruct:free").orEmpty()
        val activeOpenRouter = if (rawOpenRouter.isBlank() || rawOpenRouter == "deepseek/deepseek-r1:free") {
            "meta-llama/llama-3.3-70b-instruct:free"
        } else {
            rawOpenRouter
        }

        val storedResend = securePrefs.getString("resend_api_key", null)
            ?: securePrefs.getString("email_api_key", "").orEmpty()

        return AppSettings(
            geminiApiKey = securePrefs.getString("gemini_api_key", "").orEmpty(),
            openRouterApiKey = securePrefs.getString("openrouter_api_key", "").orEmpty(),
            geminiModel = activeGemini,
            openRouterModel = activeOpenRouter,
            serperApiKey = securePrefs.getString("serper_api_key", "").orEmpty(),
            tavilyApiKey = securePrefs.getString("tavily_api_key", "").orEmpty(),
            resendApiKey = storedResend,
            paymentLinkUrl = securePrefs.getString("payment_link_url", "").orEmpty(),
            autoPilotEnabled = securePrefs.getBoolean("auto_pilot_enabled", false),
            searchQuery = securePrefs.getString(
                "search_query",
                "looking for SEO audit OR \"need an SEO audit\" site:linkedin.com OR site:reddit.com OR site:x.com"
            ) ?: "looking for SEO audit",
            autoRunOnApproval = securePrefs.getBoolean("auto_run_approval", true),
            elevenLabsApiKey = securePrefs.getString("elevenlabs_api_key", "").orEmpty(),
            elevenLabsVoiceId = securePrefs.getString("elevenlabs_voice_id", "21m00Tcm4TlvDq8ikWAM").orEmpty().ifBlank { "21m00Tcm4TlvDq8ikWAM" },
            inboundEmailWebhookUrl = securePrefs.getString("inbound_webhook_url", "").orEmpty()
        )
    }

    fun saveSettings(settings: AppSettings) {
        securePrefs.edit()
            .putString("gemini_api_key", settings.geminiApiKey.trim())
            .putString("openrouter_api_key", settings.openRouterApiKey.trim())
            .putString("gemini_model", settings.geminiModel.trim())
            .putString("openrouter_model", settings.openRouterModel.trim())
            .putString("serper_api_key", settings.serperApiKey.trim())
            .putString("tavily_api_key", settings.tavilyApiKey.trim())
            .putString("resend_api_key", settings.resendApiKey.trim())
            .putString("email_api_key", settings.resendApiKey.trim())
            .putString("payment_link_url", settings.paymentLinkUrl.trim())
            .putBoolean("auto_pilot_enabled", settings.autoPilotEnabled)
            .putString("search_query", settings.searchQuery.trim())
            .putBoolean("auto_run_approval", settings.autoRunOnApproval)
            .putString("elevenlabs_api_key", settings.elevenLabsApiKey.trim())
            .putString("elevenlabs_voice_id", settings.elevenLabsVoiceId.trim())
            .putString("inbound_webhook_url", settings.inboundEmailWebhookUrl.trim())
            .remove("groq_api_key")
            .remove("groq_model")
            .apply()

        _settingsFlow.value = settings
    }
}
