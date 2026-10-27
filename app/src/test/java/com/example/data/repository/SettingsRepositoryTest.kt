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
            groqApiKey = "gsk_test_groq_key_12345",
            serperApiKey = "serper_test_key_67890",
            tavilyApiKey = "tvly_test_backup_key_abcde",
            groqModel = "openai/gpt-oss-120b",
            searchQuery = "looking for SEO audit site:linkedin.com"
        )

        settingsRepository.saveSettings(testSettings)

        val loaded = settingsRepository.settingsFlow.first()
        assertEquals("gsk_test_groq_key_12345", loaded.groqApiKey)
        assertEquals("serper_test_key_67890", loaded.serperApiKey)
        assertEquals("tvly_test_backup_key_abcde", loaded.tavilyApiKey)
        assertEquals("openai/gpt-oss-120b", loaded.groqModel)
        assertTrue(loaded.isGroqConfigured)
        assertTrue(loaded.isSearchConfigured)
    }

    @Test
    fun retiredLlamaModel_autoMigratesToGptOss120b() = runBlocking {
        val prefs = context.getSharedPreferences("autofreelancer_secure_keys_fallback", Context.MODE_PRIVATE)
        prefs.edit().putString("groq_model", "llama-3.3-70b-versatile").commit()

        val repo = SettingsRepository(context)
        val loaded = repo.settingsFlow.first()
        assertEquals("openai/gpt-oss-120b", loaded.groqModel)
    }
}
