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
class ResendEmailServiceTest {

    private val service = ResendEmailService()

    @Test
    fun sendEmail_blankApiKey_returnsFailure() = runBlocking {
        val result = service.sendEmail(
            apiKey = "",
            toEmail = "client@example.com",
            subject = "SEO Proposal",
            bodyText = "Here is the proposal..."
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Resend API Key") == true)
    }

    @Test
    fun sendEmail_blankRecipient_returnsFailure() = runBlocking {
        val result = service.sendEmail(
            apiKey = "re_test_key_12345",
            toEmail = "",
            subject = "SEO Proposal",
            bodyText = "Here is the proposal..."
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Recipient email") == true)
    }

    @Test
    fun testConnection_blankApiKey_returnsFailure() = runBlocking {
        val result = service.testConnection(apiKey = "")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Resend API Key") == true)
    }
}
