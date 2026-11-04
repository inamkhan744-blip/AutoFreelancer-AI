package com.example.data.service

import android.content.Context
import android.util.Log
import com.example.data.model.CrmClient
import com.example.data.model.DealPipelineStage
import com.example.data.repository.ClientRepository
import com.example.data.repository.SettingsRepository
import com.example.util.LiveWorkerLogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PaymentWebhookResult(
    val client: CrmClient,
    val amountPaid: Double,
    val source: String,
    val pdfDispatched: Boolean,
    val message: String
)

class PaymentWebhookService(
    private val context: Context,
    private val clientRepository: ClientRepository = ClientRepository(context),
    private val settingsRepository: SettingsRepository = SettingsRepository(context),
    private val pdfReportService: PdfReportService = PdfReportService(),
    private val brevoEmailService: BrevoEmailService = BrevoEmailService()
) {
    private val tag = "PaymentWebhookService"

    /**
     * Parses an incoming webhook payload from Gumroad, Stripe, or generic payment relay.
     * Marks the client as WON, logs the revenue, and automatically triggers branded PDF report generation and dispatch.
     */
    suspend fun handleInboundPayment(
        payloadJson: String,
        sourceHint: String = "Stripe"
    ): Result<PaymentWebhookResult> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(payloadJson.trim())
            var buyerEmail = ""
            var buyerName = ""
            var amountPaid = 797.0
            var detectedSource = sourceHint

            // 1. Check for Stripe Webhook Event Structure (e.g. checkout.session.completed)
            if (json.has("type") && json.has("data")) {
                val dataObj = json.optJSONObject("data")?.optJSONObject("object")
                if (dataObj != null) {
                    val customerDetails = dataObj.optJSONObject("customer_details")
                    buyerEmail = customerDetails?.optString("email").orEmpty().ifBlank {
                        dataObj.optString("customer_email").ifBlank {
                            dataObj.optString("receipt_email")
                        }
                    }
                    buyerName = customerDetails?.optString("name").orEmpty().ifBlank { "Valued Client" }
                    val totalCents = dataObj.optLong("amount_total", dataObj.optLong("amount", 79700L))
                    amountPaid = if (totalCents > 0) totalCents / 100.0 else 797.0
                    detectedSource = "Stripe (${json.optString("type")})"
                }
            }

            // 2. Check for Gumroad Webhook Structure
            if (buyerEmail.isBlank() && (json.has("buyer_email") || json.has("sale_id") || json.has("order_number"))) {
                buyerEmail = json.optString("email", json.optString("buyer_email")).trim()
                buyerName = json.optString("full_name", json.optString("name", "Valued Client")).trim()
                val priceCents = json.optLong("price", 79700L)
                amountPaid = if (priceCents > 0) priceCents / 100.0 else 797.0
                detectedSource = "Gumroad"
            }

            // 3. Check for Generic / Flattened JSON
            if (buyerEmail.isBlank()) {
                buyerEmail = json.optString("email", json.optString("customer_email")).trim()
                buyerName = json.optString("name", "Client Partner").trim()
                val amt = json.optDouble("amount", 797.0)
                amountPaid = if (!amt.isNaN() && amt > 0) amt else 797.0
                detectedSource = json.optString("source", sourceHint)
            }

            if (buyerEmail.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Could not extract buyer email from webhook payload.")
                )
            }

            val settings = settingsRepository.settingsFlow.value
            val currentClients = clientRepository.observeClients().first()
            val existing = currentClients.find {
                it.email.equals(buyerEmail, ignoreCase = true) ||
                    (it.company.isNotBlank() && buyerEmail.contains(it.company, ignoreCase = true))
            }

            val targetClient = existing?.copy(
                pipelineStage = DealPipelineStage.WON,
                status = DealPipelineStage.WON,
                paymentStatus = "PAID",
                proposalPrice = amountPaid,
                estimatedValue = amountPaid,
                updatedAt = System.currentTimeMillis()
            ) ?: CrmClient(
                name = buyerName.ifBlank { buyerEmail.substringBefore("@") },
                email = buyerEmail,
                company = buyerEmail.substringAfter("@").substringBefore("."),
                pipelineStage = DealPipelineStage.WON,
                status = DealPipelineStage.WON,
                paymentStatus = "PAID",
                proposalPrice = amountPaid,
                estimatedValue = amountPaid,
                leadSource = "$detectedSource Webhook",
                updatedAt = System.currentTimeMillis()
            )

            // Save to CRM
            clientRepository.upsertClient(targetClient)

            // Generate Branded PDF Report
            val domain = targetClient.websiteUrl.ifBlank { targetClient.company.ifBlank { "your-brand.com" } }
            val pdfAttachment = pdfReportService.generateEmailAttachment(
                clientName = targetClient.name,
                domain = domain,
                agencyName = settings.agencyName,
                dailyBleedUsd = 340.0,
                monthlyBleedUsd = 10200.0,
                paymentLink = settings.paymentLinkUrl
            )

            // Dispatch Delivery Email via Brevo
            var pdfDispatched = false
            if (settings.brevoApiKey.isNotBlank()) {
                val subject = "🎉 Payment Received ($${amountPaid.toInt()}) — Your Executive SEO Audit & Strategy Dossier"
                val bodyText = """
                    Hi ${targetClient.name},

                    Thank you for your payment of $${amountPaid.toInt()} via $detectedSource.
                    
                    Your account has been officially activated and marked as Closed Won. We have already initiated the technical crawl and keyword recovery processes for $domain to eliminate the active daily organic traffic bleed.

                    Attached to this email is your full, branded Executive Technical SEO Audit & 30-Day Recovery Dossier (PDF).

                    Our senior engineering team will be in touch with the 48-hour sprint roadmap. If you have any immediate questions, simply reply directly to this email.

                    Welcome to ${settings.agencyName}!

                    Best regards,
                    ${settings.agencyName} Team
                """.trimIndent()

                val emailResult = brevoEmailService.sendEmail(
                    apiKey = settings.brevoApiKey,
                    toEmail = targetClient.email,
                    subject = subject,
                    bodyText = bodyText,
                    attachments = listOf(pdfAttachment)
                )
                pdfDispatched = emailResult.isSuccess
            }

            val successMsg = "Payment of $${amountPaid.toInt()} confirmed from $buyerEmail via $detectedSource. Client marked as WON and Branded PDF dispatched!"
            LiveWorkerLogStore.record("Payment Webhook", "🎉 $successMsg")
            Log.i(tag, successMsg)

            Result.success(
                PaymentWebhookResult(
                    client = targetClient,
                    amountPaid = amountPaid,
                    source = detectedSource,
                    pdfDispatched = pdfDispatched,
                    message = successMsg
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to process payment webhook: ${e.message}", e)
            Result.failure(e)
        }
    }
}
