package com.example.data.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ElevenLabsVoiceServiceTest {

    private val service = ElevenLabsVoiceService()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun textToSpeech_blankApiKey_returnsFailure() = runBlocking {
        val result = service.textToSpeech(
            context = context,
            apiKey = "",
            text = "Hello from Auto-Freelancer AI Agency"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("ElevenLabs API Key") == true)
    }

    @Test
    fun textToSpeech_blankText_returnsFailure() = runBlocking {
        val result = service.textToSpeech(
            context = context,
            apiKey = "xi_test_key_12345",
            text = "   "
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("empty text") == true)
    }

    @Test
    fun testConnection_blankApiKey_returnsFailure() = runBlocking {
        val result = service.testConnection(apiKey = "")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("ElevenLabs API Key") == true)
    }
}
