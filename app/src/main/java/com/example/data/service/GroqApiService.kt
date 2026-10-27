package com.example.data.service

import android.util.Log
import com.example.data.model.ProspectResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GroqApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
) {
    private val tag = "GroqApiService"
    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"

    suspend fun generateChatCompletion(
        apiKey: String,
        model: String = "openai/gpt-oss-120b",
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 1200,
        temperature: Double = 0.7
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Groq API Key is not configured. Please add your Groq key in Settings.")
            )
        }

        try {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()

            val messagesArray = JSONArray().apply {
                if (systemPrompt.isNotBlank()) {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        }
                    )
                }
                put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    }
                )
            }

            val effectiveModel = if (model.isBlank() || model == "llama-3.3-70b-versatile") {
                "openai/gpt-oss-120b"
            } else {
                model
            }

            val requestJson = JSONObject().apply {
                put("model", effectiveModel)
                put("messages", messagesArray)
                put("temperature", temperature)
                put("max_tokens", maxTokens)
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(body).optJSONObject("error")
                        errObj?.optString("message") ?: "HTTP ${response.code}: $body"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $body"
                    }
                    Log.e(tag, "Groq API error: $errorMsg")
                    return@withContext Result.failure(Exception("Groq API error ($errorMsg)"))
                }

                val jsonResponse = JSONObject(body)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.optJSONObject("message")
                    val content = message?.optString("content").orEmpty()
                    if (content.isNotBlank()) {
                        return@withContext Result.success(content)
                    }
                }
                Result.failure(Exception("Groq API returned an empty completion response."))
            }
        } catch (e: Exception) {
            Log.e(tag, "Groq network request failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun testConnection(
        apiKey: String,
        model: String = "openai/gpt-oss-120b"
    ): Result<String> {
        return generateChatCompletion(
            apiKey = apiKey,
            model = model,
            systemPrompt = "You are a test validator.",
            userPrompt = "Respond with exactly: 'OK: Groq connection successful!'",
            maxTokens = 20,
            temperature = 0.1
        )
    }

    suspend fun generateTaskDeliverable(
        apiKey: String,
        model: String = "openai/gpt-oss-120b",
        taskTitle: String,
        category: String,
        client: String,
        workerName: String,
        workerRole: String,
        leads: List<ProspectResult>
    ): Result<String> {
        val systemPrompt = """
            You are $workerName, acting as an elite $workerRole in the AutoFreelancer AI agency.
            You produce real, high-impact deliverables, client outreach proposals, and technical SEO action plans.
            Always format your response cleanly with clear section headings, actionable recommendations, and professional tone.
        """.trimIndent()

        val leadsSummary = if (leads.isNotEmpty()) {
            leads.mapIndexed { idx, lead ->
                """
                Lead #${idx + 1}:
                - Title: ${lead.title}
                - URL: ${lead.link}
                - Source: ${lead.sourcePlatform}
                - Notes/Context: ${lead.snippet}
                """.trimIndent()
            }.joinToString("\n\n")
        } else {
            "Targeting general prospect inquiries and organic SEO search opportunities."
        }

        val userPrompt = """
            TASK: $taskTitle
            CATEGORY: $category
            PROSPECT / CLIENT: $client
            
            DISCOVERED LIVE SEARCH LEADS:
            $leadsSummary
            
            INSTRUCTIONS:
            1. Craft a personalized, non-spammy outreach message or deliverable tailored directly to the prospect and findings above.
            2. Include 3 specific high-impact audit findings / hypotheses (e.g., Core Web Vitals, indexation bloat, programmatic keyword cannibalization, internal linking).
            3. Provide an estimated project scope value and a zero-friction Call to Action (e.g. proposing a 4-minute Loom video audit).
            4. Make it ready to deliver immediately.
        """.trimIndent()

        return generateChatCompletion(
            apiKey = apiKey,
            model = model,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            maxTokens = 1200
        )
    }
}
