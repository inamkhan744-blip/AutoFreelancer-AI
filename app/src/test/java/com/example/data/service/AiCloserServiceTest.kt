package com.example.data.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiCloserServiceTest {

    private val closerService = AiCloserService()

    @Test
    fun analyzeAndDraftReply_handlesBudgetObjection_appendsPaymentLink() = runBlocking {
        val result = closerService.analyzeAndDraftReply(
            senderName = "Alex Rivera",
            senderEmail = "alex@techstart.io",
            subject = "Re: SEO Proposal",
            incomingMessage = "Thanks for the proposal, but the cost is way too high and we do not have this budget right now.",
            geminiApiKey = "",
            openRouterApiKey = "",
            paymentLinkUrl = "https://gumroad.com/l/seo-agency-pack"
        )

        assertTrue(result.isSuccess)
        val reply = result.getOrThrow()
        assertNotNull(reply)
        assertTrue(reply.objectionCategory.contains("Budget", ignoreCase = true))
        assertTrue(reply.draftedReply.contains("https://gumroad.com/l/seo-agency-pack"))
        assertTrue(reply.draftedReply.contains("Alex"))
    }

    @Test
    fun analyzeAndDraftReply_handlesTrustObjection() = runBlocking {
        val result = closerService.analyzeAndDraftReply(
            senderName = "Sarah Chen",
            senderEmail = "sarah@chenlaw.com",
            subject = "Questions about your SEO work",
            incomingMessage = "Can you show me case studies or proof this will work? We have been burned before by agencies.",
            geminiApiKey = "",
            openRouterApiKey = "",
            paymentLinkUrl = ""
        )

        assertTrue(result.isSuccess)
        val reply = result.getOrThrow()
        assertTrue(reply.objectionCategory.contains("Proof", ignoreCase = true) || reply.objectionCategory.contains("Trust", ignoreCase = true))
        assertTrue(reply.draftedReply.isNotBlank())
    }

    @Test
    fun fetchInboundEmailsFromWebhook_emptyUrl_returnsFailure() = runBlocking {
        val result = closerService.fetchInboundEmailsFromWebhook("")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Webhook URL is empty") == true)
    }
}
