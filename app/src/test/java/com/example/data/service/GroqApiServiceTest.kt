package com.example.data.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroqApiServiceTest {

    private val service = GroqApiService()

    @Test
    fun fetchAvailableModels_blankKey_returnsFailure() = runBlocking {
        val result = service.fetchAvailableModels("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Groq API Key is not configured") == true)
    }

    @Test
    fun generateChatCompletion_blankKey_returnsFailure() = runBlocking {
        val result = service.generateChatCompletion(
            apiKey = "",
            systemPrompt = "test",
            userPrompt = "test"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Groq API Key is not configured") == true)
    }
}
