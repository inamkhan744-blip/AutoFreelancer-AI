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
    val providerUsed: String,
    val reasoningAnalysis: String = "",
    val reasoningStrategy: String = "",
    val nextBestAction: String = ""
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
     * Uses Gemini AI (with OpenRouter fallback) in "Reasoning Mode" to analyze
     * the incoming client email thread, formulate strategic reasoning, determine
     * the next best action, and craft an elite objection-handling response.
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
        paymentLinkUrl: String = "",
        bookingLinkUrl: String = "",
        agencyName: String = "Apex Growth Agency"
    ): Result<ObjectionHandlingResult> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are 'The Closer', the AI Sales Partner and Principal Negotiator at $agencyName.
            You operate in explicit REASONING MODE before drafting every reply.
            
            You must structure your response with three analytical tags followed by the final draft:
            <Analysis>
            [Deep breakdown of the prospect's underlying hesitation, real objection, and emotional state]
            </Analysis>
            <Reasoning>
            [Psychological reframing strategy, risk reversal tactic, and positioning angle to deploy]
            </Reasoning>
            <Next Best Action>
            [Action recommendation: e.g. Book 10-min strategy call / Offer 50% split deposit / Send Loom demo]
            </Next Best Action>
            
            DRAFT REPLY:
            [The complete, elegant, high-converting email ready to send to the client]
            
            RULES:
            1. Empathize with their specific objection first without being defensive.
            2. Reframe the objection into measurable ROI and missed revenue.
            3. Include a zero-friction next step (using ${if (bookingLinkUrl.isNotBlank()) "the booking link: $bookingLinkUrl" else "a 10-minute discovery call"}).
            4. Sign off respectfully as "$agencyName Team". Never use bracket placeholders.
        """.trimIndent()

        val userPrompt = """
            PROSPECT THREAD TO ANALYZE:
            Client Name: $senderName
            Client Email: $senderEmail
            Subject: $subject
            Incoming Message:
            \"\"\"
            $incomingMessage
            \"\"\"
            
            Analyze the thread and output:
            <Analysis>...</Analysis>
            <Reasoning>...</Reasoning>
            <Next Best Action>...</Next Best Action>
            OBJECTION TYPE: [e.g. Budget Constraint / Timing / Trust & Proof / In-House / Scope]
            DRAFT REPLY:
            [Full email response]
        """.trimIndent()

        val fallbackDraft = """
            Hi $senderName,
            
            Thank you for getting back to me regarding "$subject".
            
            I completely understand your perspective and respect your position. Most of our partners initially had the exact same hesitation before we uncovered their high-impact organic keyword opportunities.
            
            Even with conservative projections, fixing the technical indexation and speed bottlenecks will recover far more revenue each month than the one-time investment.
            
            ${if (bookingLinkUrl.isNotBlank()) "Let's hop on a brief 10-minute call to review the quick wins: $bookingLinkUrl" else "Let's start with a streamlined audit phase so you see tangible ranking improvements before committing to the full package."}
            
            Best regards,
            $agencyName Team
        """.trimIndent()

        var drafted = fallbackDraft
        var category = "Budget Objection"
        var provider = "Autonomous Template Closer"
        var analysis = "Client expressed standard commercial hesitation regarding timeline or upfront cost."
        var reasoning = "Acknowledge constraint, reframe into monthly organic traffic loss, offer low-commitment entry point."
        var nextAction = if (bookingLinkUrl.isNotBlank()) "Direct prospect to 10-min calendar intro ($bookingLinkUrl)" else "Offer 3-point micro-audit summary"

        if (geminiApiKey.isNotBlank() || openRouterApiKey.isNotBlank()) {
            val aiRes = aiService.generateContentWithFallback(
                geminiApiKey = geminiApiKey,
                openRouterApiKey = openRouterApiKey,
                geminiModel = geminiModel,
                openRouterModel = openRouterModel,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = 1200,
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

                // Extract Reasoning Mode tags
                val analysisRegex = Regex("<Analysis>([\\s\\S]*?)</Analysis>", RegexOption.IGNORE_CASE)
                val reasoningRegex = Regex("<Reasoning>([\\s\\S]*?)</Reasoning>", RegexOption.IGNORE_CASE)
                val nextActionRegex = Regex("<Next Best Action>([\\s\\S]*?)</Next Best Action>", RegexOption.IGNORE_CASE)

                analysisRegex.find(content)?.groupValues?.getOrNull(1)?.trim()?.let {
                    if (it.isNotBlank()) analysis = it
                }
                reasoningRegex.find(content)?.groupValues?.getOrNull(1)?.trim()?.let {
                    if (it.isNotBlank()) reasoning = it
                }
                nextActionRegex.find(content)?.groupValues?.getOrNull(1)?.trim()?.let {
                    if (it.isNotBlank()) nextAction = it
                }

                // Parse category and draft
                if (content.contains("DRAFT REPLY:", ignoreCase = true)) {
                    val parts = content.split(Regex("DRAFT REPLY:", RegexOption.IGNORE_CASE))
                    val headerPart = parts[0]
                    val generatedDraft = parts.getOrNull(1)?.trim().orEmpty()
                    if (generatedDraft.isNotBlank()) {
                        drafted = generatedDraft
                    }

                    if (headerPart.contains("OBJECTION TYPE:", ignoreCase = true)) {
                        category = headerPart.substringAfter("OBJECTION TYPE:").trim().lines().firstOrNull() ?: "General Objection"
                    }
                } else {
                    // Clean out reasoning tags if the AI didn't use DRAFT REPLY delimiter
                    drafted = content
                        .replace(analysisRegex, "")
                        .replace(reasoningRegex, "")
                        .replace(nextActionRegex, "")
                        .replace(Regex("OBJECTION TYPE:.*"), "")
                        .trim()
                    category = detectObjectionCategory(incomingMessage)
                }
            } else {
                val error = aiRes.exceptionOrNull()
                Log.w(tag, "AI Closer generation failed: ${error?.message}")
                return@withContext Result.failure(
                    IllegalStateException(
                        "AI reply generation failed: ${error?.message ?: "Unknown AI error"}",
                        error
                    )
                )
            }
        } else {
            category = detectObjectionCategory(incomingMessage)
        }

        if (paymentLinkUrl.isNotBlank() && !drafted.contains(paymentLinkUrl)) {
            drafted += "\n\nTo lock in your priority audit slot today, you can complete onboarding here: ${paymentLinkUrl.trim()}"
        }

        Result.success(
            ObjectionHandlingResult(
                objectionCategory = category,
                draftedReply = drafted,
                providerUsed = provider,
                reasoningAnalysis = analysis,
                reasoningStrategy = reasoning,
                nextBestAction = nextAction
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

    data class LeadPriorityScore(
        val score: Int,
        val summary: String,
        val highIntentSignals: List<String> = emptyList(),
        val providerUsed: String = "Heuristic ML"
    )

    /**
     * Machine learning-based priority scoring system that analyzes incoming lead
     * emails and assigns a 1-10 'Conversion Probability' score.
     */
    suspend fun scoreLeadConversionProbability(
        emailBody: String,
        subject: String,
        geminiApiKey: String = "",
        geminiModel: String = AiOrchestrationService.PRIMARY_GEMINI_MODEL
    ): LeadPriorityScore = withContext(Dispatchers.IO) {
        val lowerText = "$subject $emailBody".lowercase()
        val detectedSignals = mutableListOf<String>()
        var baseScore = 5

        // Commercial / Budget Intent
        if (lowerText.contains("budget") || lowerText.contains("quote") || lowerText.contains("pricing") ||
            lowerText.contains("cost") || lowerText.contains("how much") || lowerText.contains("rates") ||
            lowerText.contains("$") || lowerText.contains("proposal") || lowerText.contains("invoice") ||
            lowerText.contains("package") || lowerText.contains("tier")) {
            baseScore += 2
            detectedSignals.add("Commercial/Budget Inquiry")
        }

        // Urgency / Timeline
        if (lowerText.contains("asap") || lowerText.contains("urgent") || lowerText.contains("immediately") ||
            lowerText.contains("this week") || lowerText.contains("start soon") || lowerText.contains("deadline") ||
            lowerText.contains("ready now")) {
            baseScore += 2
            detectedSignals.add("High Timeline Urgency")
        }

        // Direct Call / Meeting
        if (lowerText.contains("call") || lowerText.contains("schedule") || lowerText.contains("calendly") ||
            lowerText.contains("zoom") || lowerText.contains("meet") || lowerText.contains("available at") ||
            lowerText.contains("phone") || lowerText.contains("discuss")) {
            baseScore += 2
            detectedSignals.add("Meeting/Call Requested")
        }

        // Ready to close / Hiring intent
        if (lowerText.contains("hire") || lowerText.contains("work together") || lowerText.contains("contract") ||
            lowerText.contains("let's do it") || lowerText.contains("ready to start") || lowerText.contains("move forward") ||
            lowerText.contains("sign up") || lowerText.contains("agreement")) {
            baseScore += 2
            detectedSignals.add("High Closing Intent")
        }

        // Negative / Rejection triggers
        if (lowerText.contains("unsubscribe") || lowerText.contains("stop emailing") || lowerText.contains("not interested") ||
            lowerText.contains("remove me") || lowerText.contains("spam") || lowerText.contains("wrong person") ||
            lowerText.contains("do not contact")) {
            baseScore = 1
            detectedSignals.add("Opt-Out / Disinterest")
        } else if (lowerText.contains("too expensive") || lowerText.contains("no budget") || lowerText.contains("can't afford")) {
            baseScore -= 2
            detectedSignals.add("Budget Friction")
        }

        val clampedScore = baseScore.coerceIn(1, 10)
        val defaultSummary = when (clampedScore) {
            in 8..10 -> "High intent: Prospect ready to discuss scope, pricing, or call."
            in 6..7 -> "Moderate intent: Actively inquiring into services and timeline."
            in 4..5 -> "Neutral / Inquiry: General discovery or preliminary objection."
            else -> "Low probability: Budget constraint or opt-out signal."
        }

        if (geminiApiKey.isNotBlank()) {
            try {
                val prompt = """
                    Analyze this prospective client email for sales closing probability:
                    Subject: $subject
                    Body: $emailBody

                    Respond ONLY in JSON format:
                    {"score": 9, "summary": "Brief 1-sentence explanation of buying intent and probability."}
                """.trimIndent()

                val geminiRes = aiService.generateContentWithFallback(
                    geminiApiKey = geminiApiKey,
                    openRouterApiKey = "",
                    geminiModel = geminiModel,
                    systemPrompt = "You are a sales machine learning conversion scoring model. Output valid JSON with integer score (1-10) and concise summary.",
                    userPrompt = prompt,
                    maxTokens = 150,
                    temperature = 0.2
                )
                if (geminiRes.isSuccess) {
                    val content = geminiRes.getOrThrow().content
                    val jsonStart = content.indexOf('{')
                    val jsonEnd = content.lastIndexOf('}')
                    if (jsonStart != -1 && jsonEnd != -1) {
                        val parsed = JSONObject(content.substring(jsonStart, jsonEnd + 1))
                        val aiScore = parsed.optInt("score", clampedScore).coerceIn(1, 10)
                        val aiSummary = parsed.optString("summary", defaultSummary)
                        return@withContext LeadPriorityScore(
                            score = aiScore,
                            summary = aiSummary,
                            highIntentSignals = detectedSignals,
                            providerUsed = "Gemini ML ($geminiModel)"
                        )
                    }
                }
            } catch (ignored: Exception) {}
        }

        LeadPriorityScore(
            score = clampedScore,
            summary = defaultSummary,
            highIntentSignals = detectedSignals,
            providerUsed = "Heuristic ML"
        )
    }

    /**
     * Parses incoming emails specifically for job-related keywords using the Gemini API,
     * ensuring only relevant professional client requests are classified as Hot Leads
     * rather than generic automated audit spam or bot notifications.
     */
    suspend fun classifyJobIntentWithGemini(
        subject: String,
        incomingMessage: String,
        geminiApiKey: String,
        geminiModel: String = AiOrchestrationService.PRIMARY_GEMINI_MODEL
    ): JobIntentClassification = withContext(Dispatchers.IO) {
        val lowerText = "$subject\n$incomingMessage".lowercase()

        // 1. Detect Generic Automated Audit Spam & Bot Notifications
        val spamIndicators = listOf(
            "automated audit", "seo score 4", "seo score 3", "seo score 2",
            "noreply", "no-reply", "mailer-daemon", "postmaster",
            "click here to claim your listing", "newsletter digest",
            "unsubscribe from our marketing", "system notification",
            "uptime alert", "ssl certificate notice", "automated report generated",
            "do not reply to this email", "link exchange request", "guest post offer"
        )
        val isSpam = spamIndicators.any { lowerText.contains(it) }

        // 2. Extract Job-Related Keywords (Heuristic check)
        val candidateJobKeywords = listOf(
            "hiring", "hire", "quote", "budget", "pricing", "cost", "rates",
            "proposal", "scope of work", "need help", "freelancer", "consultant",
            "contract", "redesign", "shopify", "store traffic", "organic traffic dropped",
            "improve ranking", "seo audit", "technical audit", "retainer", "timeline",
            "available to start", "work together", "discuss project", "zoom call",
            "discovery call", "website overhaul"
        )
        val matchedKeywords = candidateJobKeywords.filter { keyword ->
            lowerText.contains(keyword)
        }.distinct()

        val hasStrongJobSignals = !isSpam && (matchedKeywords.size >= 2 ||
            (matchedKeywords.size >= 1 && (lowerText.contains("hiring") || lowerText.contains("quote") || lowerText.contains("budget") || lowerText.contains("proposal"))))

        val defaultCategory = when {
            isSpam -> "Automated Audit Spam / Bot"
            lowerText.contains("hiring") || lowerText.contains("hire") -> "Hiring Freelance Specialist"
            lowerText.contains("quote") || lowerText.contains("budget") || lowerText.contains("pricing") -> "Project Quote & Scope Request"
            lowerText.contains("shopify") || lowerText.contains("store") -> "E-Commerce / Shopify Optimization"
            lowerText.contains("traffic dropped") || lowerText.contains("need help") -> "Urgent Site Optimization Inbound"
            hasStrongJobSignals -> "Professional Client Inbound"
            else -> "General Inquiry"
        }

        val defaultSummary = if (hasStrongJobSignals) {
            "Professional client inquiry with job keywords: ${matchedKeywords.take(4).joinToString(", ")}"
        } else if (isSpam) {
            "Filtered out as automated audit spam / generic marketing bot."
        } else {
            "Standard incoming correspondence without active hiring signals."
        }

        // 3. Gemini Multimodal / Structured LLM Analysis when API key is available
        if (geminiApiKey.isNotBlank()) {
            try {
                val prompt = """
                    Classify this incoming email for a freelance web optimization agency:
                    Subject: $subject
                    Body:
                    $incomingMessage

                    Determine if this is a GENUINE PROFESSIONAL CLIENT REQUEST (e.g., business owner looking for help, asking for quotes, hiring a freelancer, discussing a project) vs GENERIC AUTOMATED AUDIT SPAM / BOTS.
                    Extract specific job keywords (e.g., ["hiring", "quote", "Shopify", "budget ${'$'}1,500"]).

                    Respond ONLY with valid JSON:
                    {
                      "is_hot_lead": true,
                      "job_keywords": ["hiring", "quote", "shopify"],
                      "client_intent_category": "Hiring Freelancer / Project Inbound",
                      "summary": "Legitimate business owner inquiring about rates and timeline for store optimization.",
                      "is_spam_or_automated": false
                    }
                """.trimIndent()

                val geminiRes = aiService.generateContentWithFallback(
                    geminiApiKey = geminiApiKey,
                    openRouterApiKey = "",
                    geminiModel = geminiModel,
                    systemPrompt = "You are an elite lead qualification engine. Output strictly valid JSON.",
                    userPrompt = prompt,
                    maxTokens = 200,
                    temperature = 0.1
                )

                if (geminiRes.isSuccess) {
                    val content = geminiRes.getOrThrow().content
                    val jsonStart = content.indexOf('{')
                    val jsonEnd = content.lastIndexOf('}')
                    if (jsonStart != -1 && jsonEnd != -1) {
                        val parsed = JSONObject(content.substring(jsonStart, jsonEnd + 1))
                        val isHot = parsed.optBoolean("is_hot_lead", hasStrongJobSignals)
                        val isSpamResult = parsed.optBoolean("is_spam_or_automated", isSpam)
                        val intentCat = parsed.optString("client_intent_category", defaultCategory)
                        val aiSummary = parsed.optString("summary", defaultSummary)

                        val keywordsArray = parsed.optJSONArray("job_keywords")
                        val aiKeywords = if (keywordsArray != null) {
                            (0 until keywordsArray.length()).map { keywordsArray.getString(it) }
                        } else matchedKeywords

                        return@withContext JobIntentClassification(
                            isHotLead = isHot && !isSpamResult,
                            jobKeywords = aiKeywords.ifEmpty { matchedKeywords },
                            clientIntentCategory = intentCat,
                            summary = aiSummary,
                            isSpamOrAutomatedAudit = isSpamResult,
                            providerUsed = "Gemini AI ($geminiModel)"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Gemini hot lead classification fallback: ${e.message}")
            }
        }

        JobIntentClassification(
            isHotLead = hasStrongJobSignals,
            jobKeywords = matchedKeywords,
            clientIntentCategory = defaultCategory,
            summary = defaultSummary,
            isSpamOrAutomatedAudit = isSpam,
            providerUsed = "Heuristic Parser"
        )
    }
}

data class JobIntentClassification(
    val isHotLead: Boolean,
    val jobKeywords: List<String>,
    val clientIntentCategory: String,
    val summary: String,
    val isSpamOrAutomatedAudit: Boolean,
    val providerUsed: String = "Gemini AI"
)
