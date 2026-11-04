package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsRepositoryCloudFallbackTest {
    @Test
    fun loadCloudSettings_withoutAuthOrFirestore_keepsApiKeysEmpty() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SettingsRepository(context, firestore = null, auth = null)

        repository.loadCloudSettings()

        val settings = repository.settingsFlow.value
        assertEquals("", settings.geminiApiKey)
        assertEquals("", settings.openRouterApiKey)
        assertEquals("", settings.serperApiKey)
        assertEquals("", settings.tavilyApiKey)
        assertEquals("", settings.elevenLabsApiKey)
        assertEquals("", settings.brevoApiKey)
        assertEquals("", settings.paymentLinkUrl)
    }
}