package com.example.data.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BrevoEmailServiceTest {

    private val service = BrevoEmailService()

    @Test
    fun sendEmail_blankApiKey_returnsFailure() = runBlocking {
        val result = service.sendEmail(
            apiKey = "",
            toEmail = "client@example.com",
            subject = "SEO Proposal",
            bodyText = "Here is the proposal..."
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Brevo API key") == true)
    }

    @Test
    fun sendEmail_blankRecipient_returnsFailure() = runBlocking {
        val result = service.sendEmail(
            apiKey = "xkeysib-test-key",
            toEmail = "",
            subject = "SEO Proposal",
            bodyText = "Here is the proposal..."
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("recipient email") == true)
    }

    @Test
    fun sendEmail_invalidVerifiedSender_returnsFailure() = runBlocking {
        val result = service.sendEmail(
            apiKey = "xkeysib-test-key",
            toEmail = "client@company.com",
            subject = "SEO Proposal",
            bodyText = "Here is the proposal...",
            fromEmail = "not-a-verified-address"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("verified Brevo sender") == true)
    }

    @Test
    fun testConnection_blankApiKey_returnsFailure() = runBlocking {
        val result = service.testConnection(apiKey = "")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Brevo API key") == true)
    }
}