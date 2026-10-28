package com.example.data.service

import android.util.Log
import com.example.data.model.OutreachTone
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
        const val PRIMARY_GEMINI_MODEL = "gemini-3.8-flash"
        const val FALLBACK_OPENROUTER_MODEL = "meta-llama/llama-3.3-70b-instruct:free"
    }

    /**
     * Executes AI text generation using Google Gemini as the primary engine.
     * If Gemini fails (rate limit, error, timeout, or empty response), it
     * logs a clear diagnostic message and automatically falls back to OpenRouter.
     * If OpenRouter also fails, a detailed combined error message is returned.
     */
    suspend fun generateContentWithFallback(
        geminiApiKey: String,
        openRouterApiKey: String,
        geminiModel: String = PRIMARY_GEMINI_MODEL,
        openRouterModel: String = FALLBACK_OPENROUTER_MODEL,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 1500,
        temperature: Double = 0.7
    ): Result<AiGenerationResult> = withContext(Dispatchers.IO) {
        val geminiKeyClean = geminiApiKey.trim()
        val openRouterKeyClean = openRouterApiKey.trim()

        val effectiveGeminiModel = cleanGeminiModel(geminiModel)
        val effectiveOpenRouterModel = cleanOpenRouterModel(openRouterModel)

        var geminiFailureReason: String? = null
        var openRouterFailureReason: String? = null

        // 1. Try Primary: Google Gemini
        if (geminiKeyClean.isNotBlank()) {
            val geminiResult = callGeminiApi(
                apiKey = geminiKeyClean,
                model = effectiveGeminiModel,
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
                            providerUsed = "Google Gemini ($effectiveGeminiModel)"
                        )
                    )
                } else {
                    geminiFailureReason = "Gemini returned empty candidate content."
                }
            } else {
                val err = geminiResult.exceptionOrNull()?.message ?: "Unknown Gemini failure"
                geminiFailureReason = err
                Log.w(
                    tag,
                    "Primary Gemini call failed with model '$effectiveGeminiModel': $err. Attempting fallback to OpenRouter..."
                )
            }
        } else {
            geminiFailureReason = "Gemini API key is not configured."
        }

        // 2. Fallback: OpenRouter
        if (openRouterKeyClean.isNotBlank()) {
            Log.i(tag, "Executing fallback via OpenRouter model '$effectiveOpenRouterModel'...")
            val openRouterResult = callOpenRouterApi(
                apiKey = openRouterKeyClean,
                model = effectiveOpenRouterModel,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = maxTokens,
                temperature = temperature
            )

            if (openRouterResult.isSuccess) {
                val text = openRouterResult.getOrThrow()
                if (text.isNotBlank()) {
                    Log.i(tag, "OpenRouter fallback succeeded using model '$effectiveOpenRouterModel'.")
                    return@withContext Result.success(
                        AiGenerationResult(
                            content = text,
                            providerUsed = "OpenRouter ($effectiveOpenRouterModel) [Fallback]"
                        )
                    )
                } else {
                    openRouterFailureReason = "OpenRouter returned an empty completion."
                }
            } else {
                val err = openRouterResult.exceptionOrNull()?.message ?: "Unknown OpenRouter failure"
                openRouterFailureReason = err
                Log.e(
                    tag,
                    "OpenRouter fallback failed with model '$effectiveOpenRouterModel': $err"
                )
            }
        } else {
            openRouterFailureReason = "OpenRouter fallback API key is not configured in Settings."
        }

        // Detailed error reporting so the user and system know exactly what failed on both endpoints
        val failureReason = buildString {
            append("AI Generation Failed. ")
            if (geminiKeyClean.isNotBlank()) {
                append("Primary Gemini ($effectiveGeminiModel): $geminiFailureReason. ")
            } else {
                append("Primary Gemini: Not configured. ")
            }
            if (openRouterKeyClean.isNotBlank()) {
                append("Fallback OpenRouter ($effectiveOpenRouterModel): $openRouterFailureReason.")
            } else {
                append("Fallback OpenRouter: Not configured.")
            }
        }

        Log.e(tag, failureReason)
        Result.failure(Exception(failureReason))
    }

    private fun cleanGeminiModel(model: String): String {
        val clean = model.trim().removePrefix("models/")
        return if (clean.isBlank() || clean == "gemini-1.5-flash") {
            PRIMARY_GEMINI_MODEL
        } else {
            clean
        }
    }

    private fun cleanOpenRouterModel(model: String): String {
        val clean = model.trim()
        return if (clean.isBlank() || clean == "deepseek/deepseek-r1:free") {
            FALLBACK_OPENROUTER_MODEL
        } else {
            clean
        }
    }

    private fun callGeminiApi(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        temperature: Double
    ): Result<String> {
        return try {
            val effectiveModel = cleanGeminiModel(model)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent?key=$apiKey"
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
                    return Result.failure(Exception("Gemini HTTP ${response.code} ($effectiveModel): $errorMsg"))
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
        model: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        temperature: Double
    ): Result<String> {
        return try {
            val effectiveModel = cleanOpenRouterModel(model)
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
                put("model", effectiveModel)
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
                    return Result.failure(Exception("OpenRouter HTTP ${response.code} ($effectiveModel): $errorMsg"))
                }

                val jsonResponse = JSONObject(body)
                val choices = jsonResponse.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.optJSONObject("message")
                    var text = message?.optString("content").orEmpty()
                    // Strip DeepSeek/reasoning tags if present
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

    suspend fun fetchAvailableGeminiModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a Gemini API Key first."))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey.trim()}"
            val request = Request.Builder()
                .url(url)
                .get()
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
                    return@withContext Result.failure(Exception("Gemini models error: $errorMsg"))
                }

                val json = JSONObject(body)
                val modelsArray = json.optJSONArray("models")
                val resultList = mutableListOf<String>()

                if (modelsArray != null) {
                    for (i in 0 until modelsArray.length()) {
                        val obj = modelsArray.getJSONObject(i)
                        val name = obj.optString("name").removePrefix("models/")
                        val methods = obj.optJSONArray("supportedGenerationMethods")
                        val supportsGenerateContent = if (methods != null) {
                            (0 until methods.length()).any { idx ->
                                methods.optString(idx) == "generateContent"
                            }
                        } else true

                        if (supportsGenerateContent && name.isNotBlank()) {
                            resultList.add(name)
                        }
                    }
                }

                if (resultList.isEmpty()) {
                    Result.success(listOf("gemini-3.8-flash", "gemini-3.6-flash"))
                } else {
                    val sorted = resultList.sortedWith { a, b ->
                        when {
                            a == "gemini-3.8-flash" -> -1
                            b == "gemini-3.8-flash" -> 1
                            a == "gemini-3.6-flash" -> -1
                            b == "gemini-3.6-flash" -> 1
                            a.startsWith("gemini-3") && !b.startsWith("gemini-3") -> -1
                            !a.startsWith("gemini-3") && b.startsWith("gemini-3") -> 1
                            else -> a.compareTo(b, ignoreCase = true)
                        }
                    }
                    Result.success(sorted)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch Gemini models: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAvailableOpenRouterModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter an OpenRouter API Key first."))
        }

        try {
            val url = "https://openrouter.ai/api/v1/models"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .get()
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
                    return@withContext Result.failure(Exception("OpenRouter models error: $errorMsg"))
                }

                val json = JSONObject(body)
                val dataArray = json.optJSONArray("data")
                val resultList = mutableListOf<String>()

                if (dataArray != null) {
                    for (i in 0 until dataArray.length()) {
                        val obj = dataArray.getJSONObject(i)
                        val id = obj.optString("id")
                        if (id.isNotBlank()) {
                            resultList.add(id)
                        }
                    }
                }

                if (resultList.isEmpty()) {
                    Result.success(
                        listOf(
                            "meta-llama/llama-3.3-70b-instruct:free",
                            "google/gemma-4-26b-a4b-it:free"
                        )
                    )
                } else {
                    // Prioritize free models at the top
                    val sorted = resultList.sortedWith { a, b ->
                        val aFree = a.endsWith(":free")
                        val bFree = b.endsWith(":free")
                        when {
                            a == "meta-llama/llama-3.3-70b-instruct:free" -> -1
                            b == "meta-llama/llama-3.3-70b-instruct:free" -> 1
                            a == "google/gemma-4-26b-a4b-it:free" -> -1
                            b == "google/gemma-4-26b-a4b-it:free" -> 1
                            aFree && !bFree -> -1
                            !aFree && bFree -> 1
                            else -> a.compareTo(b, ignoreCase = true)
                        }
                    }
                    Result.success(sorted)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch OpenRouter models: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun testGeminiConnection(apiKey: String, model: String = PRIMARY_GEMINI_MODEL): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a Gemini API Key first."))
        }
        val effectiveModel = cleanGeminiModel(model)
        val res = callGeminiApi(
            apiKey = apiKey.trim(),
            model = effectiveModel,
            systemPrompt = "",
            userPrompt = "Respond with exactly: 'OK: Gemini connection successful!'",
            maxTokens = 25,
            temperature = 0.1
        )
        if (res.isSuccess) {
            Result.success("Gemini Connected! ($effectiveModel)")
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Gemini test failed"))
        }
    }

    suspend fun testOpenRouterConnection(apiKey: String, model: String = FALLBACK_OPENROUTER_MODEL): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter an OpenRouter API Key first."))
        }
        val effectiveModel = cleanOpenRouterModel(model)
        val res = callOpenRouterApi(
            apiKey = apiKey.trim(),
            model = effectiveModel,
            systemPrompt = "",
            userPrompt = "Respond with exactly: 'OK: OpenRouter connection successful!'",
            maxTokens = 25,
            temperature = 0.1
        )
        if (res.isSuccess) {
            Result.success("OpenRouter Connected! ($effectiveModel)")
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("OpenRouter test failed"))
        }
    }

    suspend fun generateTaskDeliverable(
        geminiApiKey: String,
        openRouterApiKey: String,
        geminiModel: String = PRIMARY_GEMINI_MODEL,
        openRouterModel: String = FALLBACK_OPENROUTER_MODEL,
        taskTitle: String,
        category: String,
        client: String,
        workerName: String,
        workerRole: String,
        leads: List<ProspectResult>,
        paymentLinkUrl: String = "",
        outreachTone: OutreachTone = OutreachTone.PROFESSIONAL,
        targetWebsite: String = "",
        dynamicPrice: Int? = null
    ): Result<AiGenerationResult> {
        val systemPrompt = """
            You are $workerName, acting as an elite $workerRole in the AutoFreelancer AI agency.
            You produce real, high-impact deliverables, client outreach proposals, and technical SEO action plans.
            Always format your response cleanly with clear section headings and actionable recommendations.
            Use a ${outreachTone.promptLabel.lowercase()} outreach tone based on the strongest observed reply rate.
            ${dynamicPrice?.let { "Use exactly $$it as the audit price; do not generate a different amount." }.orEmpty()}
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
        val website = targetWebsite.ifBlank {
            leads.firstOrNull()?.let(CompetitorSpyPrompt::websiteFor).orEmpty()
        }
        val competitorInstructions = CompetitorSpyPrompt.instructions(
            website.ifBlank { "No client website URL was provided; use the business context as a hypothesis." },
            "Client: $client\nTask: $taskTitle\n${leadsSummary.take(1200)}"
        )

        val userPrompt = """
            TASK: $taskTitle
            CATEGORY: $category
            PROSPECT / CLIENT: $client
            
            DISCOVERED LIVE SEARCH LEADS:
            $leadsSummary

            $competitorInstructions
            
            INSTRUCTIONS:
            1. Craft a personalized, non-spammy outreach message or deliverable tailored directly to the prospect and findings above.
            2. Include 3 specific high-impact audit findings / hypotheses (e.g., Core Web Vitals, indexation bloat, programmatic keyword cannibalization, internal linking).
            3. ${dynamicPrice?.let { "Quote exactly $$it for the audit." } ?: "Provide an estimated project scope value."}
            4. Provide a zero-friction Call to Action (e.g. proposing a 4-minute Loom video audit).
            5. Make it ready to deliver immediately.
        """.trimIndent()

        val genResult = generateContentWithFallback(
            geminiApiKey = geminiApiKey,
            openRouterApiKey = openRouterApiKey,
            geminiModel = geminiModel,
            openRouterModel = openRouterModel,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            maxTokens = 1500
        )

        return if (genResult.isSuccess && paymentLinkUrl.isNotBlank()) {
            val gen = genResult.getOrThrow()
            val cta = if (dynamicPrice != null) {
                "\n\nThe ${outreachTone.promptLabel.lowercase()} audit package is $$dynamicPrice. To get the full SEO audit report, complete payment here: ${paymentLinkUrl.trim()}"
            } else {
                "\n\nTo get the full SEO audit report, please complete the payment here: ${paymentLinkUrl.trim()}"
            }
            Result.success(gen.copy(content = gen.content + cta))
        } else {
            genResult
        }
    }
}
