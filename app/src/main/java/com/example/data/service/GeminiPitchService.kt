package com.example.data.service

import android.util.Log
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
    val recommendedCta: String
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
        customInstructions: String = ""
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
            
            Write a deeply personalized, high-converting pitch message for this prospect.
            Guidelines:
            1. No robotic corporate fluff. Be authentic, technical, helpful, and empathetic.
            2. Reference their exact situation (e.g. core update traffic drops, migrations, Shopify redirects, or indexation).
            3. Provide 2-3 value-first insights without charging anything upfront.
            4. Propose a zero-friction CTA: e.g. "I can record a quick 4-minute video walking through what I found, no strings attached."
            5. Suggest an estimated project scope value between $800 and $4,000 depending on complexity.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonString = response.text
                ?: throw IllegalStateException("Gemini returned an empty response.")

            val json = JSONObject(jsonString)
            val nameOrCompany = json.optString("prospectNameOrCompany", "Prospective Client")
            val subject = json.optString("subjectLine", "Quick question regarding your SEO audit")
            val pitch = json.optString("personalizedPitchMessage", "")
            val suggestedValue = json.optDouble("suggestedRateOrValue", 1800.0)
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
                    recommendedCta = cta
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
