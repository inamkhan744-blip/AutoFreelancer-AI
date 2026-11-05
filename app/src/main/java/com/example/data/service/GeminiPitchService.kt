package com.example.data.service

import android.util.Log
import com.example.data.model.OutreachTone
import com.example.data.model.ProspectResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GeneratedPitch(
    val prospectNameOrCompany: String,
    val subjectLine: String,
    val personalizedPitchMessage: String,
    val auditHooks: List<String>,
    val suggestedRateOrValue: Double,
    val recommendedCta: String,
    val outreachTone: OutreachTone = OutreachTone.PROFESSIONAL
)

class GeminiPitchService {
    private val tag = "GeminiPitchService"

    private val pitchSchema = Schema.obj(
        properties = mapOf(
            "prospectNameOrCompany" to Schema.string(description = "Identified prospect person, role, or company name"),
            "subjectLine" to Schema.string(description = "Punchy, personalized email or DM subject line"),
            "personalizedPitchMessage" to Schema.string(description = "Full high-converting, personalized, non-spammy pitch message"),
            "auditHooks" to Schema.array(
                items = Schema.string(),
                description = "2 to 3 specific technical SEO observation points or hypotheses"
            ),
            "suggestedRateOrValue" to Schema.double(description = "Recommended freelance contract rate in USD (e.g. 1500.0)"),
            "recommendedCta" to Schema.string(description = "Low-friction next step (e.g. offer a 4-minute Loom video audit)")
        )
    )

    private val config = generationConfig {
        responseMimeType = "application/json"
        responseSchema = pitchSchema
        temperature = 0.7f
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    suspend fun generatePersonalizedPitch(
        prospect: ProspectResult,
        freelancerSpecialty: String = "Technical SEO & Conversion Rate Optimization",
        customInstructions: String = "",
        outreachTone: OutreachTone = OutreachTone.PROFESSIONAL,
        dynamicPrice: Int = DynamicPricingEngine.TIER_RETAINER_PRICE
    ): Result<GeneratedPitch> = withContext(Dispatchers.IO) {
        val prompt = """
            You are 'Apollo-Scout', an expert autonomous AI freelance business development agent for an elite SEO & Web Growth Agency.
            
            A potential client or prospect was found online actively asking for an SEO audit:
            - Source Platform: ${prospect.sourcePlatform}
            - Post Title: ${prospect.title}
            - Post Snippet / Context: ${prospect.snippet}
            - Link: ${prospect.link}
            - Agency Specialty: $freelancerSpecialty
            ${if (customInstructions.isNotBlank()) "- Director Custom Directives: $customInstructions" else ""}

            ${CompetitorSpyPrompt.instructionsFor(prospect)}
            Write in a ${outreachTone.promptLabel.lowercase()} tone, chosen from historical email reply-rate data.
            
            CRITICAL HIGH-TICKET SALES PSYCHOLOGY FRAMEWORK ($797/mo Retainer):
            To eliminate price shock and justify the $797/mo fee, you MUST structure the pitch body using this 4-step framework:
            
            1. The Loss Anchor (Cost of Inaction):
               Start the pitch by calculating how much money the client is losing daily because of their bad SEO (e.g., 'You are losing approximately $300/day ($9,000/mo) in organic revenue directly to competitors ranking above you').
            
            2. The Value Proposition:
               Position the $797 not as an 'expense', but as a 'Revenue Recovery System' (e.g., 'For $797/month, our Revenue Recovery System stops that $9,000/month bleed and turns lost searchers into paying clients').
            
            3. The Risk Reversal (Guarantee):
               Include this exact guarantee statement to build massive trust:
               'If we don't recover your lost traffic in the first 30 days, you don't pay for the second month.'
            
            4. The Low-Friction CTA (Call to Action):
               Do NOT ask them to pay $797 directly in the first email. Instead, the primary low-friction CTA must be:
               'Reply to this email for a free 10-minute strategy breakdown, or pay $50 for a Micro-Audit to see the exact plan.'
            
            Guidelines:
            1. Tone: Consultative, empathetic, highly technical, and conversational. Zero pushy corporate fluff.
            2. Reference their exact situation (source platform, website issues, crawl drops).
            3. Seamlessly weave all 4 psychological anchors into the message.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonString = response.text?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: throw IllegalStateException("Gemini returned an empty response.")

            val json = JSONObject(jsonString)
            val nameOrCompany = json.optString("prospectNameOrCompany", "Prospective Client")
            val subject = json.optString("subjectLine", "Quick question regarding your SEO audit")
            val pitch = json.optString("personalizedPitchMessage", "").trim()
                .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: throw IllegalStateException("Gemini returned an empty pitch.")
            val suggestedValue = dynamicPrice.toDouble()
            val cta = json.optString("recommendedCta", "Send a complimentary 4-minute audit video")

            val hooksArray = json.optJSONArray("auditHooks")
            val hooks = mutableListOf<String>()
            if (hooksArray != null) {
                for (i in 0 until hooksArray.length()) {
                    hooks.add(hooksArray.getString(i))
                }
            }

            Result.success(
                GeneratedPitch(
                    prospectNameOrCompany = nameOrCompany,
                    subjectLine = subject,
                    personalizedPitchMessage = pitch,
                    auditHooks = hooks,
                    suggestedRateOrValue = suggestedValue,
                    recommendedCta = cta,
                    outreachTone = outreachTone
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Gemini pitch generation failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
