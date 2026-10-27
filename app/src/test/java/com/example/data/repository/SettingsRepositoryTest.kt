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
            serperApiKey = "serper_test_key_67890",
            tavilyApiKey = "tvly_test_backup_key_abcde",
            searchQuery = "looking for SEO audit site:linkedin.com"
        )

        settingsRepository.saveSettings(testSettings)

        val loaded = settingsRepository.settingsFlow.first()
        assertEquals("AIzaSy_test_gemini_key_12345", loaded.geminiApiKey)
        assertEquals("sk-or-v1-test_openrouter_key_abcde", loaded.openRouterApiKey)
        assertEquals("serper_test_key_67890", loaded.serperApiKey)
        assertEquals("tvly_test_backup_key_abcde", loaded.tavilyApiKey)
        assertTrue(loaded.isGeminiConfigured)
        assertTrue(loaded.isOpenRouterConfigured)
        assertTrue(loaded.isAiConfigured)
        assertTrue(loaded.isSearchConfigured)
    }
}
