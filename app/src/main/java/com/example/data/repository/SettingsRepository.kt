package com.example.data.repository

import android.content.Context
import android.util.Log
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
    val brevoApiKey: String = "",
    val paymentLinkUrl: String = "",
    val autoPilotEnabled: Boolean = false,
    val searchQuery: String = "looking for SEO audit OR \"need an SEO audit\" site:linkedin.com OR site:reddit.com OR site:x.com",
    val autoRunOnApproval: Boolean = true,
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "21m00Tcm4TlvDq8ikWAM", // Rachel default voice
    val inboundEmailWebhookUrl: String = "",
    // World-Class SaaS Agency Extensions
    val bookingLinkUrl: String = "",
    val agencyName: String = "Apex Growth Agency",
    val agencyLogoUrl: String = "",
    val customEmailFooter: String = "",
    val twilioAccountSid: String = "",
    val twilioAuthToken: String = "",
    val twilioPhoneNumber: String = ""
) {
    val isGeminiConfigured: Boolean
        get() = geminiApiKey.isNotBlank()

    val isOpenRouterConfigured: Boolean
        get() = openRouterApiKey.isNotBlank()

    val isAiConfigured: Boolean
        get() = isGeminiConfigured || isOpenRouterConfigured

    val isSearchConfigured: Boolean
        get() = serperApiKey.isNotBlank() || tavilyApiKey.isNotBlank()

    val isBrevoConfigured: Boolean
        get() = brevoApiKey.isNotBlank()

    val isPaymentLinkConfigured: Boolean
        get() = paymentLinkUrl.isNotBlank()

    val isElevenLabsConfigured: Boolean
        get() = elevenLabsApiKey.isNotBlank()

    val isWebhookConfigured: Boolean
        get() = inboundEmailWebhookUrl.isNotBlank()

    val isBookingLinkConfigured: Boolean
        get() = bookingLinkUrl.isNotBlank()

    val isTwilioConfigured: Boolean
        get() = twilioAccountSid.isNotBlank() && twilioAuthToken.isNotBlank() && twilioPhoneNumber.isNotBlank()
}

class SettingsRepository(
    context: Context
) {
    private val preferences = context.applicationContext.getSharedPreferences("autofreelancer_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadLocalSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadLocalSettings(): AppSettings {
        val rawGemini = preferences.getString("gemini_model", "gemini-3.8-flash").orEmpty()
        val activeGemini = if (rawGemini.isBlank() || rawGemini == "gemini-1.5-flash") {
            "gemini-3.8-flash"
        } else {
            rawGemini
        }

        val rawOpenRouter = preferences.getString("openrouter_model", "meta-llama/llama-3.3-70b-instruct:free").orEmpty()
        val activeOpenRouter = if (rawOpenRouter.isBlank() || rawOpenRouter == "deepseek/deepseek-r1:free") {
            "meta-llama/llama-3.3-70b-instruct:free"
        } else {
            rawOpenRouter
        }

        return AppSettings(
            // Ab saari API keys local memory se load hongi
            geminiApiKey = preferences.getString("gemini_api_key", "").orEmpty(),
            openRouterApiKey = preferences.getString("openrouter_api_key", "").orEmpty(),
            serperApiKey = preferences.getString("serper_api_key", "").orEmpty(),
            tavilyApiKey = preferences.getString("tavily_api_key", "").orEmpty(),
            elevenLabsApiKey = preferences.getString("elevenlabs_api_key", "").orEmpty(),
            brevoApiKey = preferences.getString("brevo_api_key", "").orEmpty(),
            paymentLinkUrl = preferences.getString("payment_link_url", "").orEmpty(),
            twilioAccountSid = preferences.getString("twilio_account_sid", "").orEmpty(),
            twilioAuthToken = preferences.getString("twilio_auth_token", "").orEmpty(),
            
            geminiModel = activeGemini,
            openRouterModel = activeOpenRouter,
            autoPilotEnabled = preferences.getBoolean("auto_pilot_enabled", false),
            searchQuery = preferences.getString(
                "search_query",
                "looking for SEO audit OR \"need an SEO audit\" site:linkedin.com OR site:reddit.com OR site:x.com"
            ) ?: "looking for SEO audit",
            autoRunOnApproval = preferences.getBoolean("auto_run_approval", true),
            elevenLabsVoiceId = preferences.getString("elevenlabs_voice_id", "21m00Tcm4TlvDq8ikWAM").orEmpty().ifBlank { "21m00Tcm4TlvDq8ikWAM" },
            inboundEmailWebhookUrl = preferences.getString("inbound_webhook_url", "").orEmpty(),
            bookingLinkUrl = preferences.getString("booking_link_url", "").orEmpty(),
            agencyName = preferences.getString("agency_name", "Apex Growth Agency").orEmpty().ifBlank { "Apex Growth Agency" },
            agencyLogoUrl = preferences.getString("agency_logo_url", "").orEmpty(),
            customEmailFooter = preferences.getString("custom_email_footer", "").orEmpty(),
            twilioPhoneNumber = preferences.getString("twilio_phone_number", "").orEmpty()
        )
    }

    // DashboardViewModel isko call karta hai, toh humein isay break nahi karna
    suspend fun loadCloudSettings() {
        // Ab cloud nahi, seedha local memory se load hoga
        _settingsFlow.value = loadLocalSettings()
        Log.d("SettingsRepository", "Loaded settings from local storage.")
    }

    suspend fun saveSettings(settings: AppSettings) {
        // Ab yahan koi login check nahi hai. Seedha local memory mein save hoga.
        preferences.edit()
            .putString("gemini_api_key", settings.geminiApiKey.trim())
            .putString("openrouter_api_key", settings.openRouterApiKey.trim())
            .putString("serper_api_key", settings.serperApiKey.trim())
            .putString("tavily_api_key", settings.tavilyApiKey.trim())
            .putString("elevenlabs_api_key", settings.elevenLabsApiKey.trim())
            .putString("brevo_api_key", settings.brevoApiKey.trim())
            .putString("payment_link_url", settings.paymentLinkUrl.trim())
            .putString("twilio_account_sid", settings.twilioAccountSid.trim())
            .putString("twilio_auth_token", settings.twilioAuthToken.trim())
            
            .putString("gemini_model", settings.geminiModel.trim())
            .putString("openrouter_model", settings.openRouterModel.trim())
            .putBoolean("auto_pilot_enabled", settings.autoPilotEnabled)
            .putString("search_query", settings.searchQuery.trim())
            .putBoolean("auto_run_approval", settings.autoRunOnApproval)
            .putString("elevenlabs_voice_id", settings.elevenLabsVoiceId.trim())
            .putString("inbound_webhook_url", settings.inboundEmailWebhookUrl.trim())
            .putString("booking_link_url", settings.bookingLinkUrl.trim())
            .putString("agency_name", settings.agencyName.trim())
            .putString("agency_logo_url", settings.agencyLogoUrl.trim())
            .putString("custom_email_footer", settings.customEmailFooter.trim())
            .putString("twilio_phone_number", settings.twilioPhoneNumber.trim())
            .apply()
            
        _settingsFlow.value = settings
        Log.d("SettingsRepository", "Settings saved successfully to local storage.")
    }
}
