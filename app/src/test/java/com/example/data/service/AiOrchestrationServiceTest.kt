package com.example.data.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
    fun fetchAvailableGeminiModels_blankKey_returnsFailure() = runBlocking {
        val result = service.fetchAvailableGeminiModels("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Gemini API Key") == true)
    }

    @Test
    fun fetchAvailableOpenRouterModels_blankKey_returnsFailure() = runBlocking {
        val result = service.fetchAvailableOpenRouterModels("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("OpenRouter API Key") == true)
    }

    @Test
    fun generateContentWithFallback_bothKeysBlank_returnsFailureWithClearError() = runBlocking {
        val result = service.generateContentWithFallback(
            geminiApiKey = "",
            openRouterApiKey = "",
            systemPrompt = "System",
            userPrompt = "User"
        )
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(msg.contains("Primary Gemini: Not configured"))
        assertTrue(msg.contains("Fallback OpenRouter: Not configured"))
    }

    @Test
    fun verifyModelConstants_matchUserRequirements() {
        assertEquals("gemini-3.8-flash", AiOrchestrationService.PRIMARY_GEMINI_MODEL)
        assertEquals("meta-llama/llama-3.3-70b-instruct:free", AiOrchestrationService.FALLBACK_OPENROUTER_MODEL)
    }

    @Test
    fun generateTaskDeliverable_bothKeysBlank_failsGracefully() = runBlocking {
        val result = service.generateTaskDeliverable(
            geminiApiKey = "",
            openRouterApiKey = "",
            taskTitle = "SEO Technical Audit",
            category = "Deliverable",
            client = "TestClient Corp",
            workerName = "Apollo-Scout",
            workerRole = "Lead Generation Specialist",
            leads = emptyList(),
            paymentLinkUrl = "https://buy.stripe.com/test12345"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("AI Generation Failed") == true)
    }
}
