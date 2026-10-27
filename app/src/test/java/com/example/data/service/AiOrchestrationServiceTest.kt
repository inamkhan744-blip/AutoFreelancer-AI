package com.example.data.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiOrchestrationServiceTest {

    private val service = AiOrchestrationService()

    @Test
    fun testGeminiConnection_blankKey_returnsFailure() = runBlocking {
        val result = service.testGeminiConnection("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Gemini API Key") == true)
    }

    @Test
    fun testOpenRouterConnection_blankKey_returnsFailure() = runBlocking {
        val result = service.testOpenRouterConnection("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("OpenRouter API Key") == true)
    }

    @Test
    fun generateContentWithFallback_bothKeysBlank_returnsFailure() = runBlocking {
        val result = service.generateContentWithFallback(
            geminiApiKey = "",
            openRouterApiKey = "",
            systemPrompt = "System",
            userPrompt = "User"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No AI API keys configured") == true)
    }

    @Test
    fun verifyModelConstants_matchUserRequirements() {
        assertEquals("gemini-1.5-flash", AiOrchestrationService.PRIMARY_GEMINI_MODEL)
        assertEquals("deepseek/deepseek-r1:free", AiOrchestrationService.FALLBACK_OPENROUTER_MODEL)
    }
}
