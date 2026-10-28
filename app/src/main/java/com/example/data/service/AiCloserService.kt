package com.example.data.service

import android.util.Log
import com.example.data.model.InboxEmail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ObjectionHandlingResult(
    val objectionCategory: String,
    val draftedReply: String,
    val providerUsed: String
)

class AiCloserService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build(),
    private val aiService: AiOrchestrationService = AiOrchestrationService()
) {
    private val tag = "AiCloserService"

    /**
     * Uses Gemini AI (with OpenRouter fallback) to analyze the incoming client email,
     * classify the objection, and craft an elite, high-converting objection-handling response.
     */
    suspend fun analyzeAndDraftReply(
        senderName: String,
        senderEmail: String,
        subject: String,
        incomingMessage: String,
        geminiApiKey: String,
        openRouterApiKey: String,
        geminiModel: String = AiOrchestrationService.PRIMARY_GEMINI_MODEL,
        openRouterModel: String = AiOrchestrationService.FALLBACK_OPENROUTER_MODEL,
        paymentLinkUrl: String = ""
    ): Result<ObjectionHandlingResult> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are 'The Closer', a world-class autonomous freelance sales executive and agency objection handler.
            Your role is to read inbound client emails, identify their primary objection (e.g., budget is too tight, no time right now, skeptics on ROI, comparing to competitors),
            and draft a persuasive, polite, empathetic, yet assertive response that turns hesitation into a signed deal.
            
            RULES:
            1. Empathize with their specific objection first.
            2. Reframe the objection into a massive opportunity or ROI justification.
            3. Provide clear next steps (call, review proposal, or complete payment).
            4. Keep the tone warm, confident, and professional.
            5. Do NOT include placeholder tokens like [Your Name] or [Agency Name] — sign off as "AutoFreelancer Agency Director".
        """.trimIndent()

        val userPrompt = """
            Analyze this incoming client email and draft the ideal response to close the deal:
            
            CLIENT NAME: $senderName
            CLIENT EMAIL: $senderEmail
            EMAIL SUBJECT: $subject
            INCOMING MESSAGE:
            \"\"\"
            $incomingMessage
            \"\"\"
            
            Provide your response in this clean format:
            OBJECTION TYPE: [e.g. Budget Constraint / Timing & Prioritization / Proof & Trust / Scope & Technical]
            
            DRAFT REPLY:
            [Your full, ready-to-send email body]
        """.trimIndent()

        val fallbackDraft = """
            Hi $senderName,
            
            Thank you for getting back to me regarding "$subject".
            
            I completely understand your perspective and respect your position. Most of our top clients initially had the exact same hesitation before we uncovered their high-impact organic keyword opportunities.
            
            Even with conservative projections, fixing the technical indexation and speed bottlenecks will recover far more revenue each month than the one-time investment.
            
            Let's start with a streamlined audit phase so you see tangible ranking improvements before committing to the full package.
            
            Best regards,
            AutoFreelancer Agency Director
        """.trimIndent()

        var drafted = fallbackDraft
        var category = "Budget Objection"
        var provider = "Autonomous Template Closer"

        if (geminiApiKey.isNotBlank() || openRouterApiKey.isNotBlank()) {
            val aiRes = aiService.generateContentWithFallback(
                geminiApiKey = geminiApiKey,
                openRouterApiKey = openRouterApiKey,
                geminiModel = geminiModel,
                openRouterModel = openRouterModel,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = 850,
                temperature = 0.5
            )

            if (aiRes.isSuccess) {
                val gen = aiRes.getOrThrow()
                provider = gen.providerUsed
                val content = gen.content.trim()
                if (content.isBlank() || content.equals("null", ignoreCase = true)) {
                    return@withContext Result.failure(
                        IllegalStateException("AI returned an empty reply. Your existing draft was kept unchanged.")
                    )
                }

                // Parse category and draft
                if (content.contains("DRAFT REPLY:", ignoreCase = true)) {
                    val parts = content.split(Regex("DRAFT REPLY:", RegexOption.IGNORE_CASE))
                    val headerPart = parts[0]
                    val generatedDraft = parts.getOrNull(1)?.trim().orEmpty()
                    if (generatedDraft.isBlank() || generatedDraft.equals("null", ignoreCase = true)) {
                        return@withContext Result.failure(
                            IllegalStateException("AI returned an empty reply. Your existing draft was kept unchanged.")
                        )
                    }
                    drafted = generatedDraft

                    if (headerPart.contains("OBJECTION TYPE:", ignoreCase = true)) {
                        category = headerPart.substringAfter("OBJECTION TYPE:").trim().lines().firstOrNull() ?: "General Objection"
                    }
                } else {
                    drafted = content
                    category = detectObjectionCategory(incomingMessage)
                }
            } else {
                val error = aiRes.exceptionOrNull()
                Log.w(tag, "AI Closer generation failed: ${error?.message}")
                return@withContext Result.failure(
                    IllegalStateException(
                        "AI reply generation failed. Your existing draft was kept unchanged: ${error?.message ?: "Unknown AI error"}",
                        error
                    )
                )
            }
        } else {
            category = detectObjectionCategory(incomingMessage)
        }

        if (paymentLinkUrl.isNotBlank() && !drafted.contains(paymentLinkUrl)) {
            drafted += "\n\nTo lock in your priority audit slot today, you can complete the onboarding here: ${paymentLinkUrl.trim()}"
        }

        Result.success(
            ObjectionHandlingResult(
                objectionCategory = category,
                draftedReply = drafted,
                providerUsed = provider
            )
        )
    }

    /**
     * Polls or fetches incoming emails from an external Webhook or Zapier/Make REST endpoint if provided.
     */
    suspend fun fetchInboundEmailsFromWebhook(webhookUrl: String): Result<List<InboxEmail>> = withContext(Dispatchers.IO) {
        val cleanUrl = webhookUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Webhook URL is empty."))
        }

        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Webhook returned HTTP ${response.code}: $body"))
                }

                val list = mutableListOf<InboxEmail>()
                if (body.trim().startsWith("[")) {
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        list.add(
                            InboxEmail(
                                id = item.optString("id", UUID.randomUUID().toString()),
                                senderName = item.optString("from_name", item.optString("senderName", "Client")),
                                senderEmail = item.optString("from_email", item.optString("senderEmail", "")),
                                subject = item.optString("subject", "Re: Proposal"),
                                incomingMessage = item.optString("text", item.optString("body", item.optString("incomingMessage", ""))),
                                objectionCategory = item.optString("category", "Inbound Client Inquiry")
                            )
                        )
                    }
                } else if (body.trim().startsWith("{")) {
                    val obj = JSONObject(body)
                    if (obj.has("emails")) {
                        val arr = obj.getJSONArray("emails")
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            list.add(
                                InboxEmail(
                                    id = item.optString("id", UUID.randomUUID().toString()),
                                    senderName = item.optString("from_name", "Client"),
                                    senderEmail = item.optString("from_email", ""),
                                    subject = item.optString("subject", "Inquiry"),
                                    incomingMessage = item.optString("text", item.optString("body", ""))
                                )
                            )
                        }
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(tag, "Webhook fetch failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun detectObjectionCategory(message: String): String {
        val lower = message.lowercase()
        return when {
            lower.contains("expensive") || lower.contains("budget") || lower.contains("price") || lower.contains("cost") || lower.contains("cheap") || lower.contains("afford") -> "Budget Objection"
            lower.contains("time") || lower.contains("busy") || lower.contains("next quarter") || lower.contains("later") || lower.contains("month") -> "Timing Objection"
            lower.contains("proof") || lower.contains("guarantee") || lower.contains("results") || lower.contains("case study") || lower.contains("portfolio") -> "Proof & Trust Objection"
            lower.contains("already have") || lower.contains("in-house") || lower.contains("agency") || lower.contains("competitor") -> "Competitor / In-House Objection"
            lower.contains("how does") || lower.contains("details") || lower.contains("what is included") || lower.contains("scope") -> "Scope & Clarification"
            else -> "General Client Objection"
        }
    }
}
