package com.example.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class AppSettings(
    val geminiApiKey: String = "",
    val openRouterApiKey: String = "",
    val geminiModel: String = "gemini-3.8-flash",
    val openRouterModel: String = "meta-llama/llama-3.3-70b-instruct:free",
    val serperApiKey: String = "",
    val tavilyApiKey: String = "",
    val braveSearchApiKey: String = "",
    val googleCustomSearchApiKey: String = "",
    val googleCseCx: String = "",
    val dailyLeadTarget: Int = 5,
    val dailyRevenueTarget: Double = 100.0,
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

    val isSerperConfigured: Boolean
        get() = serperApiKey.isNotBlank()

    val isTavilyConfigured: Boolean
        get() = tavilyApiKey.isNotBlank()

    val isBraveConfigured: Boolean
        get() = braveSearchApiKey.isNotBlank()

    val isGoogleCseConfigured: Boolean
        get() = googleCustomSearchApiKey.isNotBlank() && googleCseCx.isNotBlank()

    val isSearchConfigured: Boolean
        get() = isSerperConfigured || isTavilyConfigured || isBraveConfigured || isGoogleCseConfigured

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
    context: Context,
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull(),
    private val auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()
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

        val savedRevenueTargetLong = preferences.getLong(
            "daily_revenue_target",
            java.lang.Double.doubleToLongBits(100.0)
        )
        val activeRevenueTarget = java.lang.Double.longBitsToDouble(savedRevenueTargetLong)

        return AppSettings(
            geminiApiKey = preferences.getString("gemini_api_key", "").orEmpty(),
            openRouterApiKey = preferences.getString("openrouter_api_key", "").orEmpty(),
            geminiModel = activeGemini,
            openRouterModel = activeOpenRouter,
            serperApiKey = preferences.getString("serper_api_key", "").orEmpty(),
            tavilyApiKey = preferences.getString("tavily_api_key", "").orEmpty(),
            braveSearchApiKey = preferences.getString("brave_search_api_key", "").orEmpty(),
            googleCustomSearchApiKey = preferences.getString("google_custom_search_api_key", "").orEmpty(),
            googleCseCx = preferences.getString("google_cse_cx", "").orEmpty(),
            dailyLeadTarget = preferences.getInt("daily_lead_target", 5),
            dailyRevenueTarget = activeRevenueTarget,
            brevoApiKey = preferences.getString("brevo_api_key", "").orEmpty(),
            paymentLinkUrl = preferences.getString("payment_link_url", "").orEmpty(),
            autoPilotEnabled = preferences.getBoolean("auto_pilot_enabled", false),
            searchQuery = preferences.getString(
                "search_query",
                "looking for SEO audit OR \"need an SEO audit\" site:linkedin.com OR site:reddit.com OR site:x.com"
            ) ?: "looking for SEO audit",
            autoRunOnApproval = preferences.getBoolean("auto_run_approval", true),
            elevenLabsApiKey = preferences.getString("elevenlabs_api_key", "").orEmpty(),
            elevenLabsVoiceId = preferences.getString("elevenlabs_voice_id", "21m00Tcm4TlvDq8ikWAM").orEmpty().ifBlank { "21m00Tcm4TlvDq8ikWAM" },
            inboundEmailWebhookUrl = preferences.getString("inbound_webhook_url", "").orEmpty(),
            bookingLinkUrl = preferences.getString("booking_link_url", "").orEmpty(),
            agencyName = preferences.getString("agency_name", "Apex Growth Agency").orEmpty().ifBlank { "Apex Growth Agency" },
            agencyLogoUrl = preferences.getString("agency_logo_url", "").orEmpty(),
            customEmailFooter = preferences.getString("custom_email_footer", "").orEmpty(),
            twilioAccountSid = preferences.getString("twilio_account_sid", "").orEmpty(),
            twilioAuthToken = preferences.getString("twilio_auth_token", "").orEmpty(),
            twilioPhoneNumber = preferences.getString("twilio_phone_number", "").orEmpty()
        )
    }

    suspend fun loadCloudSettings() {
        val userId = runCatching { auth?.currentUser?.uid }.getOrNull()
        val db = firestore
        if (userId == null || db == null) {
            return
        }

        try {
            val document = db.collection("users").document(userId).collection("settings").document("api_keys")
                .get().await()
            if (document.exists()) {
                val current = _settingsFlow.value
                val cloudGemini = document.getString("gemini_api_key")
                val cloudOpenRouter = document.getString("openrouter_api_key")
                val cloudSerper = document.getString("serper_api_key")
                val cloudTavily = document.getString("tavily_api_key")
                val cloudBrave = document.getString("brave_search_api_key")
                val cloudGoogleCustom = document.getString("google_custom_search_api_key")
                val cloudGoogleCx = document.getString("google_cse_cx")
                val cloudDailyLead = document.getLong("daily_lead_target")?.toInt()
                val cloudDailyRevenue = document.getDouble("daily_revenue_target")
                val cloudElevenLabs = document.getString("elevenlabs_api_key") ?: document.getString("elevenLabs_api_key")
                val cloudBrevo = document.getString("brevo_api_key")
                val cloudPaymentLink = document.getString("payment_link_url")

                _settingsFlow.value = current.copy(
                    geminiApiKey = cloudGemini ?: current.geminiApiKey,
                    openRouterApiKey = cloudOpenRouter ?: current.openRouterApiKey,
                    serperApiKey = cloudSerper ?: current.serperApiKey,
                    tavilyApiKey = cloudTavily ?: current.tavilyApiKey,
                    braveSearchApiKey = cloudBrave ?: current.braveSearchApiKey,
                    googleCustomSearchApiKey = cloudGoogleCustom ?: current.googleCustomSearchApiKey,
                    googleCseCx = cloudGoogleCx ?: current.googleCseCx,
                    dailyLeadTarget = cloudDailyLead ?: current.dailyLeadTarget,
                    dailyRevenueTarget = cloudDailyRevenue ?: current.dailyRevenueTarget,
                    elevenLabsApiKey = cloudElevenLabs ?: current.elevenLabsApiKey,
                    brevoApiKey = cloudBrevo ?: current.brevoApiKey,
                    paymentLinkUrl = cloudPaymentLink ?: current.paymentLinkUrl,
                    bookingLinkUrl = document.getString("booking_link_url") ?: current.bookingLinkUrl,
                    agencyName = document.getString("agency_name") ?: current.agencyName,
                    agencyLogoUrl = document.getString("agency_logo_url") ?: current.agencyLogoUrl,
                    customEmailFooter = document.getString("custom_email_footer") ?: current.customEmailFooter,
                    twilioAccountSid = document.getString("twilio_account_sid") ?: current.twilioAccountSid,
                    twilioAuthToken = document.getString("twilio_auth_token") ?: current.twilioAuthToken,
                    twilioPhoneNumber = document.getString("twilio_phone_number") ?: current.twilioPhoneNumber
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("SettingsRepository", "Cloud API keys read failed: ${e.message}")
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        preferences.edit()
            .putString("gemini_api_key", settings.geminiApiKey.trim())
            .putString("openrouter_api_key", settings.openRouterApiKey.trim())
            .putString("gemini_model", settings.geminiModel.trim())
            .putString("openrouter_model", settings.openRouterModel.trim())
            .putString("serper_api_key", settings.serperApiKey.trim())
            .putString("tavily_api_key", settings.tavilyApiKey.trim())
            .putString("brave_search_api_key", settings.braveSearchApiKey.trim())
            .putString("google_custom_search_api_key", settings.googleCustomSearchApiKey.trim())
            .putString("google_cse_cx", settings.googleCseCx.trim())
            .putInt("daily_lead_target", settings.dailyLeadTarget)
            .putLong("daily_revenue_target", java.lang.Double.doubleToLongBits(settings.dailyRevenueTarget))
            .putString("brevo_api_key", settings.brevoApiKey.trim())
            .putString("payment_link_url", settings.paymentLinkUrl.trim())
            .putBoolean("auto_pilot_enabled", settings.autoPilotEnabled)
            .putString("search_query", settings.searchQuery.trim())
            .putBoolean("auto_run_approval", settings.autoRunOnApproval)
            .putString("elevenlabs_api_key", settings.elevenLabsApiKey.trim())
            .putString("elevenlabs_voice_id", settings.elevenLabsVoiceId.trim())
            .putString("inbound_webhook_url", settings.inboundEmailWebhookUrl.trim())
            .putString("booking_link_url", settings.bookingLinkUrl.trim())
            .putString("agency_name", settings.agencyName.trim())
            .putString("agency_logo_url", settings.agencyLogoUrl.trim())
            .putString("custom_email_footer", settings.customEmailFooter.trim())
            .putString("twilio_account_sid", settings.twilioAccountSid.trim())
            .putString("twilio_auth_token", settings.twilioAuthToken.trim())
            .putString("twilio_phone_number", settings.twilioPhoneNumber.trim())
            .apply()

        _settingsFlow.value = settings

        val userId = auth?.currentUser?.uid
        val db = firestore
        if (userId != null && db != null) {
            try {
                db.collection("users").document(userId).collection("settings").document("api_keys")
                    .set(
                        mapOf(
                            "gemini_api_key" to settings.geminiApiKey.trim(),
                            "openrouter_api_key" to settings.openRouterApiKey.trim(),
                            "serper_api_key" to settings.serperApiKey.trim(),
                            "tavily_api_key" to settings.tavilyApiKey.trim(),
                            "brave_search_api_key" to settings.braveSearchApiKey.trim(),
                            "google_custom_search_api_key" to settings.googleCustomSearchApiKey.trim(),
                            "google_cse_cx" to settings.googleCseCx.trim(),
                            "daily_lead_target" to settings.dailyLeadTarget,
                            "daily_revenue_target" to settings.dailyRevenueTarget,
                            "elevenlabs_api_key" to settings.elevenLabsApiKey.trim(),
                            "elevenLabs_api_key" to settings.elevenLabsApiKey.trim(),
                            "brevo_api_key" to settings.brevoApiKey.trim(),
                            "payment_link_url" to settings.paymentLinkUrl.trim(),
                            "booking_link_url" to settings.bookingLinkUrl.trim(),
                            "agency_name" to settings.agencyName.trim(),
                            "agency_logo_url" to settings.agencyLogoUrl.trim(),
                            "custom_email_footer" to settings.customEmailFooter.trim(),
                            "twilio_account_sid" to settings.twilioAccountSid.trim(),
                            "twilio_auth_token" to settings.twilioAuthToken.trim(),
                            "twilio_phone_number" to settings.twilioPhoneNumber.trim()
                        )
                    ).await()
            } catch (e: Exception) {
                Log.w("SettingsRepository", "Cloud sync failed (keys saved locally): ${e.message}")
            }
        } else {
            Log.d("SettingsRepository", "Saved settings locally to device (Guest Mode)")
        }
    }
}
