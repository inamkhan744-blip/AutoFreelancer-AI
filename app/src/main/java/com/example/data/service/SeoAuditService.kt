package com.example.data.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class RawSeoData(
    val url: String,
    val httpStatus: Int,
    val latencyMs: Long,
    val title: String?,
    val metaDescription: String?,
    val h1Heading: String?,
    val canonicalUrl: String?,
    val hasViewport: Boolean,
    val robots: String?,
    val performanceScore: Int? = null,
    val issuesIdentified: List<String>,
    val rawAuditSummary: String
)

data class SeoReportResult(
    val targetUrl: String,
    val rawData: RawSeoData,
    val generatedReport: String,
    val engineUsed: String
)

class SeoAuditService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val aiService: AiOrchestrationService = AiOrchestrationService()
) {
    private val tag = "SeoAuditService"

    /**
     * Scrapes target website HTML for core technical SEO meta tags & performance metrics.
     */
    suspend fun scrapeWebsite(rawUrl: String): Result<RawSeoData> = withContext(Dispatchers.IO) {
        val targetUrl = normalizeUrl(rawUrl)
        if (targetUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please provide a valid website URL."))
        }

        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (compatible; AutoFreelancerSeoBot/1.0; +https://example.com/bot)")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val statusCode = response.code
                val html = response.body?.string().orEmpty()

                val title = extractRegex(html, "(?i)<title[^>]*>(.*?)</title>")?.trim()
                val metaDesc = extractMetaTag(html, "description") ?: extractMetaTag(html, "og:description")
                val h1 = extractRegex(html, "(?i)<h1[^>]*>(.*?)</h1>")?.replace(Regex("<[^>]*>"), "")?.trim()
                val canonical = extractRegex(html, "(?i)<link[^>]*rel=[\"']canonical[\"'][^>]*href=[\"'](.*?)[\"']")
                val hasViewport = html.contains("name=\"viewport\"", ignoreCase = true) || html.contains("name='viewport'", ignoreCase = true)
                val robots = extractMetaTag(html, "robots")

                // Optional: Attempt Google PageSpeed Insights free query
                val pagespeed = fetchPageSpeedScore(targetUrl)

                val issues = mutableListOf<String>()
                if (title.isNullOrBlank()) {
                    issues.add("Missing <title> tag (Critical ranking factor)")
                } else if (title.length < 30) {
                    issues.add("Title tag too short (${title.length} chars). Best practice is 50-60 chars.")
                } else if (title.length > 65) {
                    issues.add("Title tag too long (${title.length} chars), risk of SERP truncation.")
                }

                if (metaDesc.isNullOrBlank()) {
                    issues.add("Missing meta description (Degrades click-through rate in Google SERPs)")
                } else if (metaDesc.length < 80) {
                    issues.add("Meta description under 80 chars. Best practice is 140-160 chars.")
                }

                if (h1.isNullOrBlank()) {
                    issues.add("Missing primary <h1> heading tag on homepage")
                }

                if (!hasViewport) {
                    issues.add("Missing mobile viewport configuration (Fails Mobile-Friendly test)")
                }

                if (latency > 1500) {
                    issues.add("High server response latency (${latency}ms). TTFB needs optimization.")
                }

                val summary = buildString {
                    appendLine("TARGET WEBSITE: $targetUrl")
                    appendLine("HTTP STATUS: $statusCode (${latency}ms server response)")
                    appendLine("TITLE TAG: ${title ?: "MISSING"}")
                    appendLine("META DESCRIPTION: ${metaDesc ?: "MISSING"}")
                    appendLine("PRIMARY H1: ${h1 ?: "MISSING"}")
                    appendLine("CANONICAL URL: ${canonical ?: "None detected"}")
                    appendLine("MOBILE VIEWPORT: ${if (hasViewport) "Configured" else "MISSING"}")
                    appendLine("ROBOTS META: ${robots ?: "Default (index, follow)"}")
                    if (pagespeed != null) {
                        appendLine("GOOGLE PAGESPEED SCORE: $pagespeed/100")
                    }
                    appendLine("DETECTED ISSUES: ${if (issues.isNotEmpty()) issues.joinToString("; ") else "No severe meta tag omissions detected."}")
                }

                val rawData = RawSeoData(
                    url = targetUrl,
                    httpStatus = statusCode,
                    latencyMs = latency,
                    title = title,
                    metaDescription = metaDesc,
                    h1Heading = h1,
                    canonicalUrl = canonical,
                    hasViewport = hasViewport,
                    robots = robots,
                    performanceScore = pagespeed,
                    issuesIdentified = issues,
                    rawAuditSummary = summary
                )

                Result.success(rawData)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to scrape $targetUrl: ${e.message}", e)
            Result.failure(Exception("Could not connect to $targetUrl: ${e.message}"))
        }
    }

    /**
     * Synthesizes a professional 5-point audit report with solutions using Gemini AI.
     */
    suspend fun generate5PointAuditReport(
        rawUrl: String,
        geminiApiKey: String,
        openRouterApiKey: String,
        geminiModel: String = AiOrchestrationService.PRIMARY_GEMINI_MODEL,
        openRouterModel: String = AiOrchestrationService.FALLBACK_OPENROUTER_MODEL,
        clientName: String = "Client",
        paymentLinkUrl: String = ""
    ): Result<SeoReportResult> = withContext(Dispatchers.IO) {
        val scrapeRes = scrapeWebsite(rawUrl)
        if (scrapeRes.isFailure) {
            return@withContext Result.failure(scrapeRes.exceptionOrNull() ?: Exception("Scraping failed"))
        }

        val rawData = scrapeRes.getOrThrow()
        val systemPrompt = """
            You are an elite Senior Technical SEO Director at AutoFreelancer AI agency.
            You formulate high-impact, actionable, client-ready 5-point SEO audit reports.
            Keep formatting pristine with numbered sections (Points 1 to 5), problem analysis, technical solutions, and business impact.
            Zero fluff, maximum conversion.
        """.trimIndent()

        val userPrompt = """
            Analyze these SEO errors and write a professional 5-point audit report with solutions.
            
            CLIENT: $clientName
            AUDIT DATA:
            ${rawData.rawAuditSummary}
            
            STRUCTURE REQUIRED:
            1. Executive SEO Health Summary
            2. Point 1: Title & On-Page Snippet Optimization (Findings & Solution)
            3. Point 2: Meta Description & SERP Click-Through Rate (Findings & Solution)
            4. Point 3: Content Hierarchy & H1 Semantic Architecture (Findings & Solution)
            5. Point 4: Performance, Mobile Responsiveness & Crawlability (Findings & Solution)
            6. Point 5: High-Impact Priority Roadmap & Next Steps
        """.trimIndent()

        var reportText = """
            # 5-POINT TECHNICAL SEO AUDIT REPORT
            **Target**: ${rawData.url}
            **Client**: $clientName
            **Server Response**: ${rawData.httpStatus} (${rawData.latencyMs}ms)
            
            ### 1. Title Tag Optimization
            Current: ${rawData.title ?: "None"}
            Solution: Formulate keyword-targeted title tag within 50-60 characters.
            
            ### 2. Meta Description Architecture
            Current: ${rawData.metaDescription ?: "None"}
            Solution: Write compelling 155-character meta descriptions with clear value proposition and CTA.
            
            ### 3. Header Tag & Content Hierarchy
            Current H1: ${rawData.h1Heading ?: "None"}
            Solution: Anchor the primary search intent with a single semantic <h1> header.
            
            ### 4. Technical Crawlability & Speed
            Status: ${rawData.httpStatus} • Latency: ${rawData.latencyMs}ms
            Solution: Compress unoptimized assets, enable edge caching, and verify robots.txt.
            
            ### 5. Immediate Action Plan
            Implement priority fixes above to increase organic indexation and CTR.
        """.trimIndent()

        var engineUsed = "Direct Meta Parser"

        if (geminiApiKey.isNotBlank() || openRouterApiKey.isNotBlank()) {
            val aiRes = aiService.generateContentWithFallback(
                geminiApiKey = geminiApiKey,
                openRouterApiKey = openRouterApiKey,
                geminiModel = geminiModel,
                openRouterModel = openRouterModel,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = 1800,
                temperature = 0.6
            )

            if (aiRes.isSuccess) {
                val gen = aiRes.getOrThrow()
                reportText = gen.content
                engineUsed = gen.providerUsed
            } else {
                Log.w(tag, "AI generation failed, falling back to parsed template: ${aiRes.exceptionOrNull()?.message}")
            }
        }

        if (paymentLinkUrl.isNotBlank()) {
            reportText += "\n\n---\nTo get the full SEO audit report, please complete the payment here: ${paymentLinkUrl.trim()}"
        }

        Result.success(
            SeoReportResult(
                targetUrl = rawData.url,
                rawData = rawData,
                generatedReport = reportText,
                engineUsed = engineUsed
            )
        )
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        return if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    private fun extractRegex(content: String, regex: String): String? {
        val pattern = Pattern.compile(regex, Pattern.DOTALL)
        val matcher = pattern.matcher(content)
        return if (matcher.find()) {
            matcher.group(1)?.trim()
        } else null
    }

    private fun extractMetaTag(html: String, metaName: String): String? {
        val r1 = "(?i)<meta[^>]*name=[\"']${Pattern.quote(metaName)}[\"'][^>]*content=[\"'](.*?)[\"']"
        val r2 = "(?i)<meta[^>]*content=[\"'](.*?)[\"'][^>]*name=[\"']${Pattern.quote(metaName)}[\"']"
        val r3 = "(?i)<meta[^>]*property=[\"']${Pattern.quote(metaName)}[\"'][^>]*content=[\"'](.*?)[\"']"
        return extractRegex(html, r1) ?: extractRegex(html, r2) ?: extractRegex(html, r3)
    }

    private fun fetchPageSpeedScore(url: String): Int? {
        return try {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val apiUrl = "https://www.googleapis.com/pagespeedonline/v5/runPagespeed?url=$encoded&strategy=mobile"
            val req = Request.Builder()
                .url(apiUrl)
                .get()
                .build()

            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val lighthouse = json.optJSONObject("lighthouseResult")
                    val categories = lighthouse?.optJSONObject("categories")
                    val perf = categories?.optJSONObject("performance")
                    val score = perf?.optDouble("score", -1.0) ?: -1.0
                    if (score >= 0.0) {
                        (score * 100).toInt()
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
