package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        settingsRepository = SettingsRepository(context)
    }

    @Test
    fun saveAndRetrieveApiKeys_persistsCorrectly() = runBlocking {
        val testSettings = AppSettings(
            geminiApiKey = "AIzaSy_test_gemini_key_12345",
            openRouterApiKey = "sk-or-v1-test_openrouter_key_abcde",
            geminiModel = "gemini-3.6-flash",
            openRouterModel = "google/gemma-4-26b-a4b-it:free",
            serperApiKey = "serper_test_key_67890",
            tavilyApiKey = "tvly_test_backup_key_abcde",
            resendApiKey = "re_test_email_key_123",
            paymentLinkUrl = "https://buy.stripe.com/test_payment_link",
            autoPilotEnabled = true,
            searchQuery = "looking for SEO audit site:linkedin.com"
        )

        settingsRepository.saveSettings(testSettings)

        val loaded = settingsRepository.settingsFlow.first()
        assertEquals("AIzaSy_test_gemini_key_12345", loaded.geminiApiKey)
        assertEquals("sk-or-v1-test_openrouter_key_abcde", loaded.openRouterApiKey)
        assertEquals("gemini-3.6-flash", loaded.geminiModel)
        assertEquals("google/gemma-4-26b-a4b-it:free", loaded.openRouterModel)
        assertEquals("serper_test_key_67890", loaded.serperApiKey)
        assertEquals("tvly_test_backup_key_abcde", loaded.tavilyApiKey)
        assertEquals("re_test_email_key_123", loaded.resendApiKey)
        assertEquals("https://buy.stripe.com/test_payment_link", loaded.paymentLinkUrl)
        assertTrue(loaded.autoPilotEnabled)
        assertTrue(loaded.isGeminiConfigured)
        assertTrue(loaded.isOpenRouterConfigured)
        assertTrue(loaded.isAiConfigured)
        assertTrue(loaded.isSearchConfigured)
        assertTrue(loaded.isResendConfigured)
        assertTrue(loaded.isPaymentLinkConfigured)
    }

    @Test
    fun outdatedModels_autoMigrateToActiveModels() = runBlocking {
        val prefs = context.getSharedPreferences("autofreelancer_secure_keys_fallback", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("gemini_model", "gemini-1.5-flash")
            .putString("openrouter_model", "deepseek/deepseek-r1:free")
            .commit()

        val repo = SettingsRepository(context)
        val loaded = repo.settingsFlow.first()
        assertEquals("gemini-3.8-flash", loaded.geminiModel)
        assertEquals("meta-llama/llama-3.3-70b-instruct:free", loaded.openRouterModel)
    }
}
