package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.model.DealPipelineStage
import com.example.data.repository.ClientRepository
import com.example.data.repository.SettingsRepository
import com.example.data.service.BrevoEmailService
import com.example.data.service.PdfReportService
import com.example.util.LiveWorkerLogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class FollowUpWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val tag = "FollowUpWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(tag, "Starting automated follow-up sequence check...")
        val clientRepo = ClientRepository(appContext)
        val settingsRepo = SettingsRepository(appContext)
        val settings = settingsRepo.settingsFlow.value
        val brevoService = BrevoEmailService()
        val pdfService = PdfReportService()

        val clients = runCatching { clientRepo.observeClients().first() }.getOrDefault(emptyList())
        if (clients.isEmpty()) {
            Log.i(tag, "No clients in CRM to check for follow-up.")
            return@withContext Result.success()
        }

        val now = System.currentTimeMillis()
        var followUpsSent = 0

        for (client in clients) {
            val email = client.email.trim()
            if (email.isBlank()) continue

            val stage = DealPipelineStage.normalize(client.pipelineStage)
            // Only process leads in PITCHED or ongoing FOLLOW_UP sequence (skip WON and LOST)
            if (stage == DealPipelineStage.WON || stage == DealPipelineStage.LOST) continue
            if (stage != DealPipelineStage.PITCHED && stage != DealPipelineStage.FOLLOW_UP) continue

            val elapsedMillis = now - client.lastContactedAt
            val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMillis)

            // 7-day follow-up
            if (elapsedDays >= 7 && client.sequenceStage != "DAY_7_SENT") {
                if (settings.brevoApiKey.isNotBlank()) {
                    val pdfAttachment = pdfService.generateEmailAttachment(
                        clientName = client.name,
                        domain = client.websiteUrl.ifBlank { client.company },
                        agencyName = settings.agencyName,
                        dailyBleedUsd = 340.0,
                        monthlyBleedUsd = 10200.0,
                        paymentLink = settings.paymentLinkUrl.ifBlank { client.paymentLink }
                    )

                    val subject = "Final follow-up: ${client.company.ifBlank { client.websiteUrl.ifBlank { "your website" } }} SEO recovery plan ($797/mo)"
                    val bodyText = """
                        Hi ${client.name},

                        I wanted to circle back one final time regarding ${client.websiteUrl.ifBlank { client.company }}. 
                        
                        Your top SERP competitors are actively capturing high-intent organic buyers that should be landing on your store (costing an estimated -$120 to -$480/day in lost sales).

                        I've attached our updated Executive Technical SEO Audit PDF for your review with prioritized action items.

                        Our $797/month E-Commerce Retainer comes with a 100% 30-Day Money-Back ROI Guarantee. If you'd like to authorize work this week:
                        ${settings.paymentLinkUrl.ifBlank { client.paymentLink }}

                        Best regards,
                        ${settings.agencyName} Team
                    """.trimIndent()

                    val sendResult = brevoService.sendEmail(
                        apiKey = settings.brevoApiKey,
                        toEmail = email,
                        subject = subject,
                        bodyText = bodyText,
                        attachments = listOf(pdfAttachment)
                    )

                    if (sendResult.isSuccess) {
                        followUpsSent++
                        val updated = client.copy(
                            pipelineStage = DealPipelineStage.FOLLOW_UP,
                            status = DealPipelineStage.FOLLOW_UP,
                            sequenceStage = "DAY_7_SENT",
                            lastContactedAt = now,
                            updatedAt = now
                        )
                        clientRepo.upsertClient(updated)
                        LiveWorkerLogStore.record(
                            "Follow-Up Agent",
                            "> Sent Day-7 Follow-up with Branded PDF to $email • CRM Status: FOLLOW_UP"
                        )
                    }
                }
            }
            // 3-day follow-up
            else if (elapsedDays >= 3 && client.sequenceStage != "DAY_3_SENT" && client.sequenceStage != "DAY_7_SENT") {
                if (settings.brevoApiKey.isNotBlank()) {
                    val pdfAttachment = pdfService.generateEmailAttachment(
                        clientName = client.name,
                        domain = client.websiteUrl.ifBlank { client.company },
                        agencyName = settings.agencyName,
                        dailyBleedUsd = 340.0,
                        monthlyBleedUsd = 10200.0,
                        paymentLink = settings.paymentLinkUrl.ifBlank { client.paymentLink }
                    )

                    val subject = "${client.name}, did you see this organic traffic bleed on ${client.company.ifBlank { "your site" }}?"
                    val bodyText = """
                        Hi ${client.name},

                        Following up on my note earlier this week regarding ${client.websiteUrl.ifBlank { client.company }}.

                        We ran a deeper crawl and confirmed that mobile Core Web Vitals latency and unranked commercial queries are causing an estimated daily revenue bleed of -$120 to -$480/day leaking directly to your competitors.

                        I've generated and attached your full Executive SEO Audit Report (PDF) with the exact technical bottlenecks outlined.

                        Our $797/mo High-Ticket Growth Retainer is structured to halt this bleed and recover lost positions with zero risk (backed by our 30-day money-back guarantee).

                        You can review the audit PDF attached and authorize onboarding here:
                        ${settings.paymentLinkUrl.ifBlank { client.paymentLink }}

                        Best regards,
                        ${settings.agencyName} Team
                    """.trimIndent()

                    val sendResult = brevoService.sendEmail(
                        apiKey = settings.brevoApiKey,
                        toEmail = email,
                        subject = subject,
                        bodyText = bodyText,
                        attachments = listOf(pdfAttachment)
                    )

                    if (sendResult.isSuccess) {
                        followUpsSent++
                        val updated = client.copy(
                            pipelineStage = DealPipelineStage.FOLLOW_UP,
                            status = DealPipelineStage.FOLLOW_UP,
                            sequenceStage = "DAY_3_SENT",
                            lastContactedAt = now,
                            updatedAt = now
                        )
                        clientRepo.upsertClient(updated)
                        LiveWorkerLogStore.record(
                            "Follow-Up Agent",
                            "> Sent Day-3 Follow-up with Branded PDF to $email • CRM Status: FOLLOW_UP"
                        )
                    }
                }
            }
        }

        Log.i(tag, "Follow-up sequence completed. Dispatched $followUpsSent follow-up emails.")
        Result.success()
    }
}
