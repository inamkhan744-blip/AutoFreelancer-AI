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
    val brevoApiKey: String = "",
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

    val isBrevoConfigured: Boolean
        get() = brevoApiKey.isNotBlank()

    val isPaymentLinkConfigured: Boolean
        get() = paymentLinkUrl.isNotBlank()

    val isElevenLabsConfigured: Boolean
        get() = elevenLabsApiKey.isNotBlank()

    val isWebhookConfigured: Boolean
        get() = inboundEmailWebhookUrl.isNotBlank()
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

        return AppSettings(
            geminiModel = activeGemini,
            openRouterModel = activeOpenRouter,
            autoPilotEnabled = preferences.getBoolean("auto_pilot_enabled", false),
            searchQuery = preferences.getString(
                "search_query",
                "looking for SEO audit OR \"need an SEO audit\" site:linkedin.com OR site:reddit.com OR site:x.com"
            ) ?: "looking for SEO audit",
            autoRunOnApproval = preferences.getBoolean("auto_run_approval", true),
            elevenLabsVoiceId = preferences.getString("elevenlabs_voice_id", "21m00Tcm4TlvDq8ikWAM").orEmpty().ifBlank { "21m00Tcm4TlvDq8ikWAM" },
            inboundEmailWebhookUrl = preferences.getString("inbound_webhook_url", "").orEmpty()
        )
    }

    suspend fun loadCloudSettings() {
        val userId = runCatching { auth?.currentUser?.uid }.getOrNull()
        if (userId == null) {
            clearCloudKeys()
            return
        }

        val db = firestore
        if (db == null) {
            clearCloudKeys()
            return
        }

        val document = try {
            db.collection("users").document(userId).collection("settings").document("api_keys")
                .get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("SettingsRepository", "Cloud API keys unavailable; using empty keys: ${e.message}")
            clearCloudKeys()
            return
        }

        _settingsFlow.value = _settingsFlow.value.copy(
            geminiApiKey = document.getString("gemini_api_key").orEmpty(),
            openRouterApiKey = document.getString("openrouter_api_key").orEmpty(),
            serperApiKey = document.getString("serper_api_key").orEmpty(),
            tavilyApiKey = document.getString("tavily_api_key").orEmpty(),
            elevenLabsApiKey = document.getString("elevenlabs_api_key").orEmpty(),
            brevoApiKey = document.getString("brevo_api_key").orEmpty(),
            paymentLinkUrl = document.getString("payment_link_url").orEmpty()
        )
    }

    private fun clearCloudKeys() {
        _settingsFlow.value = _settingsFlow.value.copy(
            geminiApiKey = "",
            openRouterApiKey = "",
            serperApiKey = "",
            tavilyApiKey = "",
            elevenLabsApiKey = "",
            brevoApiKey = "",
            paymentLinkUrl = ""
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        val userId = auth?.currentUser?.uid
            ?: throw IllegalStateException("Sign in before saving API keys to Firebase.")
        val db = requireNotNull(firestore) { "Firestore is unavailable." }

        preferences.edit()
            .putString("gemini_model", settings.geminiModel.trim())
            .putString("openrouter_model", settings.openRouterModel.trim())
            .putBoolean("auto_pilot_enabled", settings.autoPilotEnabled)
            .putString("search_query", settings.searchQuery.trim())
            .putBoolean("auto_run_approval", settings.autoRunOnApproval)
            .putString("elevenlabs_voice_id", settings.elevenLabsVoiceId.trim())
            .putString("inbound_webhook_url", settings.inboundEmailWebhookUrl.trim())
            .apply()

        db.collection("users").document(userId).collection("settings").document("api_keys")
            .set(
                mapOf(
                    "gemini_api_key" to settings.geminiApiKey.trim(),
                    "openrouter_api_key" to settings.openRouterApiKey.trim(),
                    "serper_api_key" to settings.serperApiKey.trim(),
                    "tavily_api_key" to settings.tavilyApiKey.trim(),
                    "elevenlabs_api_key" to settings.elevenLabsApiKey.trim(),
                    "brevo_api_key" to settings.brevoApiKey.trim(),
                    "payment_link_url" to settings.paymentLinkUrl.trim()
                )
            ).await()
        _settingsFlow.value = settings
    }
}
