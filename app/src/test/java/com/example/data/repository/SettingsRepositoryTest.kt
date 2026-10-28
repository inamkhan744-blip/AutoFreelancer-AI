package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest : FirestoreEmulatorTestBase() {
    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository

    @Before
    override fun setUpFirebase() {
        super.setUpFirebase()
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = SettingsRepository(context, firestore, auth)
    }

    @Test
    fun saveAndRetrieveApiKeys_persistsInUserSettingsDocument() = runBlocking {
        val userId = signInTestUser("settings-user@example.com")
        val testSettings = AppSettings(
            geminiApiKey = "AIzaSy_test_gemini_key_12345",
            openRouterApiKey = "sk-or-v1-test_openrouter_key_abcde",
            geminiModel = "gemini-3.6-flash",
            openRouterModel = "google/gemma-4-26b-a4b-it:free",
            serperApiKey = "serper_test_key_67890",
            tavilyApiKey = "tvly_test_backup_key_abcde",
            brevoApiKey = "xkeysib-test-email-key",
            elevenLabsApiKey = "elevenlabs-test-key",
            paymentLinkUrl = "https://buy.stripe.com/test_payment_link",
            autoPilotEnabled = true,
            searchQuery = "looking for SEO audit site:linkedin.com"
        )

        settingsRepository.saveSettings(testSettings)

        val document = firestore.collection("users").document(userId)
            .collection("settings").document("api_keys").get().await()
        assertEquals(testSettings.geminiApiKey, document.getString("gemini_api_key"))
        assertEquals(testSettings.brevoApiKey, document.getString("brevo_api_key"))

        val reloaded = SettingsRepository(context, firestore, auth)
        reloaded.loadCloudSettings()
        val loaded = reloaded.settingsFlow.value
        assertEquals(testSettings.geminiApiKey, loaded.geminiApiKey)
        assertEquals(testSettings.openRouterApiKey, loaded.openRouterApiKey)
        assertEquals(testSettings.serperApiKey, loaded.serperApiKey)
        assertEquals(testSettings.tavilyApiKey, loaded.tavilyApiKey)
        assertEquals(testSettings.brevoApiKey, loaded.brevoApiKey)
        assertEquals(testSettings.elevenLabsApiKey, loaded.elevenLabsApiKey)
        assertEquals(testSettings.paymentLinkUrl, loaded.paymentLinkUrl)
        assertEquals(testSettings.geminiModel, loaded.geminiModel)
        assertTrue(loaded.autoPilotEnabled)
        assertTrue(loaded.isBrevoConfigured)
        assertTrue(loaded.isPaymentLinkConfigured)
    }
}
