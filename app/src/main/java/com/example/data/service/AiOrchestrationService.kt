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

data class AiGenerationResult(
    val content: String,
    val providerUsed: String
)

class AiOrchestrationService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
) {
    private val tag = "AiOrchestrationService"

    companion object {
        const val PRIMARY_GEMINI_MODEL = "gemini-1.5-flash"
        const val FALLBACK_OPENROUTER_MODEL = "deepseek/deepseek-r1:free"
    }

    /**
     * Executes AI text generation using Google Gemini as the primary engine.
     * If Gemini fails (rate limit, error, timeout, or empty response), it
     * automatically and silently falls back to OpenRouter with deepseek/deepseek-r1:free.
     */
    suspend fun generateContentWithFallback(
        geminiApiKey: String,
        openRouterApiKey: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 1500,
        temperature: Double = 0.7
    ): Result<AiGenerationResult> = withContext(Dispatchers.IO) {
        val geminiKeyClean = geminiApiKey.trim()
        val openRouterKeyClean = openRouterApiKey.trim()

        // 1. Try Primary: Google Gemini (gemini-1.5-flash)
        if (geminiKeyClean.isNotBlank()) {
            val geminiResult = callGeminiApi(
                apiKey = geminiKeyClean,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = maxTokens,
                temperature = temperature
            )

            if (geminiResult.isSuccess) {
                val text = geminiResult.getOrThrow()
                if (text.isNotBlank()) {
                    return@withContext Result.success(
                        AiGenerationResult(
                            content = text,
                            providerUsed = "Google Gemini ($PRIMARY_GEMINI_MODEL)"
                        )
                    )
                }
            } else {
                Log.w(
                    tag,
                    "Primary Gemini call failed: ${geminiResult.exceptionOrNull()?.message}. Initiating silent fallback to OpenRouter."
                )
            }
        }

        // 2. Fallback: OpenRouter (deepseek/deepseek-r1:free)
        if (openRouterKeyClean.isNotBlank()) {
            val openRouterResult = callOpenRouterApi(
                apiKey = openRouterKeyClean,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = maxTokens,
                temperature = temperature
            )

            if (openRouterResult.isSuccess) {
                val text = openRouterResult.getOrThrow()
                if (text.isNotBlank()) {
                    return@withContext Result.success(
                        AiGenerationResult(
                            content = text,
                            providerUsed = "OpenRouter ($FALLBACK_OPENROUTER_MODEL) [Fallback]"
                        )
                    )
                }
            } else {
                Log.e(
                    tag,
                    "OpenRouter fallback also failed: ${openRouterResult.exceptionOrNull()?.message}"
                )
            }
        }

        // Neither succeeded or neither configured
        val failureReason = when {
            geminiKeyClean.isBlank() && openRouterKeyClean.isBlank() ->
                "No AI API keys configured. Please add a Gemini API Key (or OpenRouter Key) in Settings."
            geminiKeyClean.isNotBlank() && openRouterKeyClean.isBlank() ->
                "Gemini API request failed and no OpenRouter fallback key was configured in Settings."
            else ->
                "Both Primary Gemini and OpenRouter fallback requests failed. Please check your API keys and network connection."
        }

        Result.failure(Exception(failureReason))
    }

    private fun callGeminiApi(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        temperature: Double
    ): Result<String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$PRIMARY_GEMINI_MODEL:generateContent?key=$apiKey"
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()

            val combinedPrompt = if (systemPrompt.isNotBlank()) {
                "SYSTEM INSTRUCTIONS:\n$systemPrompt\n\nUSER REQUEST:\n$userPrompt"
            } else {
                userPrompt
            }

            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        put("role", "user")
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", combinedPrompt) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val genConfig = JSONObject().apply {
                    put("temperature", temperature)
                    put("maxOutputTokens", maxTokens)
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
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
                    return Result.failure(Exception("Gemini HTTP ${response.code}: $errorMsg"))
                }

                val jsonResponse = JSONObject(body)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text").orEmpty()
                        if (text.isNotBlank()) {
                            return Result.success(text)
                        }
                    }
                }
                Result.failure(Exception("Gemini returned empty candidate content."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callOpenRouterApi(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        temperature: Double
    ): Result<String> {
        return try {
            val url = "https://openrouter.ai/api/v1/chat/completions"
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

            val requestBodyJson = JSONObject().apply {
                put("model", FALLBACK_OPENROUTER_MODEL)
                put("messages", messagesArray)
                put("max_tokens", maxTokens)
                put("temperature", temperature)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://autofreelancer.ai")
                .addHeader("X-Title", "Auto-Freelancer AI")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
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
                    return Result.failure(Exception("OpenRouter HTTP ${response.code}: $errorMsg"))
                }

                val jsonResponse = JSONObject(body)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.optJSONObject("message")
                    var text = message?.optString("content").orEmpty()
                    // Strip DeepSeek R1 reasoning tags if present
                    text = text.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
                    if (text.isNotBlank()) {
                        return Result.success(text)
                    }
                }
                Result.failure(Exception("OpenRouter returned empty choices."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testGeminiConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a Gemini API Key first."))
        }
        val res = callGeminiApi(
            apiKey = apiKey.trim(),
            systemPrompt = "",
            userPrompt = "Respond with exactly: 'OK: Gemini connection successful!'",
            maxTokens = 25,
            temperature = 0.1
        )
        if (res.isSuccess) {
            Result.success("Gemini Connected! ($PRIMARY_GEMINI_MODEL)")
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Gemini test failed"))
        }
    }

    suspend fun testOpenRouterConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter an OpenRouter API Key first."))
        }
        val res = callOpenRouterApi(
            apiKey = apiKey.trim(),
            systemPrompt = "",
            userPrompt = "Respond with exactly: 'OK: OpenRouter connection successful!'",
            maxTokens = 25,
            temperature = 0.1
        )
        if (res.isSuccess) {
            Result.success("OpenRouter Connected! ($FALLBACK_OPENROUTER_MODEL)")
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("OpenRouter test failed"))
        }
    }

    suspend fun generateTaskDeliverable(
        geminiApiKey: String,
        openRouterApiKey: String,
        taskTitle: String,
        category: String,
        client: String,
        workerName: String,
        workerRole: String,
        leads: List<ProspectResult>
    ): Result<AiGenerationResult> {
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

        return generateContentWithFallback(
            geminiApiKey = geminiApiKey,
            openRouterApiKey = openRouterApiKey,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            maxTokens = 1500
        )
    }
}
