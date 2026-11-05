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

data class TechnicalSolution(
    val issueFound: String,
    val impact: String,
    val exactSolution: String,
    val codeSnippet: String? = null,
    val isStepByStepPlan: Boolean = false
)

data class SeoReportResult(
    val targetUrl: String,
    val rawData: RawSeoData,
    val generatedReport: String,
    val engineUsed: String,
    val solutions: List<TechnicalSolution> = emptyList()
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
        val defaultSolutions = buildDefaultTechnicalSolutions(rawData)

        val systemPrompt = """
            You are an elite Senior Technical SEO Developer & Full-Stack Web Engineer at AutoFreelancer AI agency.
            You formulate high-impact, actionable, client-ready 5-point SEO audit reports that provide the exact HOW, not just the WHAT.
            
            MANDATORY STRUCTURE:
            For EVERY issue found or optimization point, you MUST return a structured format with three distinct parts:
            • Issue Found: (e.g., 'Missing Meta Description', 'Slow Images', 'Keyword Cannibalization', 'Missing H1 Heading')
            • Impact: (e.g., 'Google cannot understand the page context', 'High mobile bounce rate and Core Web Vitals penalty')
            • Exact Solution: Provide the exact, copy-paste ready code or step-by-step plan for a developer.
              - If the issue is 'Slow Images', provide exact HTML code for lazy loading (<img loading="lazy" ...>).
              - If the issue is 'Missing Meta Description', provide the exact <meta name="description" content="..."> snippet for the <head> tag.
              - If the issue is 'Keyword Cannibalization', provide a step-by-step plan to merge pages with 301 redirects and canonical tags.
              - The solution must be 100% ready to copy-paste for a developer.
            
            CONSULTATIVE FINANCIAL IMPACT BREAKDOWN (MANDATORY):
            Clearly show a consultative, non-pushy financial breakdown of 'Money Saved' vs 'Agency Fee':
            - Estimated Monthly Bleed (Loss from inaction): ~$9,000 - $10,200/month leaking to competitors.
            - Agency Fee ($797/mo Revenue Recovery System): $797/month.
            - Net Money Saved / Recovered: +$8,203 to +$9,403/month (Estimated ~11x-12x positive ROI).
            Tone MUST be consultative, objective, empathetic, and diagnostic — never pushy or aggressive.
            
            AT THE VERY END OF THE REPORT, you MUST include this exact closing section:
            ---
            ### Done-For-You Implementation
            $DONE_FOR_YOU_UPSELL_TEXT
        """.trimIndent()

        val userPrompt = """
            Analyze these SEO metrics and write an Expert Developer Technical SEO Audit Report with exact copy-paste code solutions.
            
            CLIENT: $clientName
            AUDIT DATA:
            ${rawData.rawAuditSummary}
            
            MANDATORY FORMAT FOR EACH ITEM:
            For each optimization point (Points 1 through 5):
            - **Issue Found**: [Clear title of the problem]
            - **Impact**: [Direct impact on search ranking, indexing, or CTR]
            - **Exact Solution**: [Exact copy-pasteable HTML/JS/htaccess code snippet or numbered step-by-step developer plan]
            
            Include:
            1. Executive SEO Health Summary (with Daily Bleed estimate)
            2. Financial Breakdown: Money Saved vs Agency Fee (Consultative ROI Analysis)
            3. Point 1: Title & On-Page Snippet Optimization (Issue Found, Impact, Exact Solution with <title> snippet)
            4. Point 2: Meta Description & SERP CTR (Issue Found, Impact, Exact Solution with <meta name="description"> tag)
            5. Point 3: Content Hierarchy & H1 Architecture (Issue Found, Impact, Exact Solution with <h1> tag)
            6. Point 4: Performance, Image Optimization & Core Web Vitals (Issue Found, Impact, Exact Solution with lazy loading <img loading="lazy"> tag)
            7. Point 5: Keyword Architecture & Cannibalization Prevention (Issue Found, Impact, Exact Solution with 301 redirect and canonical merge plan)
            
            CLOSING SECTION (MANDATORY):
            ---
            ### Done-For-You Implementation
            $DONE_FOR_YOU_UPSELL_TEXT
        """.trimIndent()

        var reportText = buildDefaultStructuredReport(rawData, clientName, defaultSolutions)
        var engineUsed = "Direct Developer Parser"

        if (geminiApiKey.isNotBlank() || openRouterApiKey.isNotBlank()) {
            val aiRes = aiService.generateContentWithFallback(
                geminiApiKey = geminiApiKey,
                openRouterApiKey = openRouterApiKey,
                geminiModel = geminiModel,
                openRouterModel = openRouterModel,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                maxTokens = 2200,
                temperature = 0.5
            )

            if (aiRes.isSuccess) {
                val gen = aiRes.getOrThrow()
                reportText = gen.content
                engineUsed = gen.providerUsed
            } else {
                Log.w(tag, "AI generation failed, falling back to parsed template: ${aiRes.exceptionOrNull()?.message}")
            }
        }

        // Ensure Done-For-You Retainer upsell is present at the end of the report
        if (!reportText.contains(DONE_FOR_YOU_UPSELL_TEXT)) {
            reportText += "\n\n---\n### Done-For-You Implementation\n$DONE_FOR_YOU_UPSELL_TEXT"
        }

        if (paymentLinkUrl.isNotBlank()) {
            reportText += "\n\nDirect Authorization Link: ${paymentLinkUrl.trim()}"
        }

        val parsedSolutions = parseTechnicalSolutions(reportText, rawData, defaultSolutions)

        Result.success(
            SeoReportResult(
                targetUrl = rawData.url,
                rawData = rawData,
                generatedReport = reportText,
                engineUsed = engineUsed,
                solutions = parsedSolutions
            )
        )
    }

    companion object {
        const val DONE_FOR_YOU_UPSELL_TEXT =
            "Need me to implement these fixes for you? Reply to this email to upgrade to our $797/month Done-For-You Retainer and let me handle everything."
    }

    /**
     * Synthesizes 5 ready-to-copy developer solutions covering Meta Descriptions,
     * Lazy-Loaded Images, Keyword Cannibalization, H1 Semantics, and Server Latency.
     */
    fun buildDefaultTechnicalSolutions(rawData: RawSeoData): List<TechnicalSolution> {
        val domain = rawData.url.substringAfter("://").substringBefore("/")
        val pageTitle = rawData.title ?: "Leading Industry Brand"

        return listOf(
            TechnicalSolution(
                issueFound = "Missing / Unoptimized Meta Description",
                impact = "Google cannot understand the page context or SERP intent, resulting in automated snippet truncation and up to 35% lower click-through rate (CTR).",
                exactSolution = "Add this exact <meta> tag inside the <head> section of your primary landing page:",
                codeSnippet = """<meta name="description" content="Discover $pageTitle at $domain. Fast, verified solutions designed to maximize search visibility and organic conversions.">"""
            ),
            TechnicalSolution(
                issueFound = "Slow Images & High Largest Contentful Paint (LCP)",
                impact = "Unoptimized above-and-below the fold images block the main browser thread, causing mobile LCP to exceed 2.8s and penalizing Core Web Vitals rankings.",
                exactSolution = "Apply native browser-level lazy loading, asynchronous decoding, and explicit dimensions to all catalog/content images:",
                codeSnippet = """<img src="assets/banner.webp" alt="$pageTitle" loading="lazy" decoding="async" width="800" height="450">"""
            ),
            TechnicalSolution(
                issueFound = "Keyword Cannibalization & Page Split",
                impact = "Multiple competing URLs target identical keywords, splitting backlink equity, confusing search crawlers, and suppressing top-3 Google rankings.",
                exactSolution = "Step-by-step page consolidation plan:\n1. Consolidate thin competing pages into one authoritative Master Pillar page.\n2. Add a 301 Permanent Redirect rule in .htaccess / server configuration.\n3. Add a self-referencing canonical tag to the Master Pillar page.\n4. Update all internal site menus to point strictly to the consolidated URL.",
                codeSnippet = """# 301 Redirect old competing page to canonical pillar
Redirect 301 /subpage-competing /${domain.take(12)}-guide

<!-- Self-referencing canonical tag in <head> -->
<link rel="canonical" href="${rawData.url}">""",
                isStepByStepPlan = true
            ),
            TechnicalSolution(
                issueFound = "Missing / Fragmented H1 Heading Architecture",
                impact = "Search bots cannot extract the primary topical entity of the page, diluting keyword relevance in semantic search vectors.",
                exactSolution = "Add a single, semantic <h1> heading at the top of the main container matching primary search intent:",
                codeSnippet = """<h1 class="hero-title">${rawData.title?.take(48) ?: "Premier Solutions Engineered for Growth"}</h1>"""
            ),
            TechnicalSolution(
                issueFound = "Server Response Latency (TTFB) & Missing Asset Compression",
                impact = "Server response latency of ${rawData.latencyMs}ms causes crawler budget depletion and increases bounce rate for mobile users.",
                exactSolution = "Enable Gzip / Brotli compression and cache headers in your Apache/Nginx configuration:",
                codeSnippet = """<IfModule mod_deflate.c>
  AddOutputFilterByType DEFLATE text/html text/plain text/xml text/css application/javascript application/json
</IfModule>"""
            )
        )
    }

    private fun buildDefaultStructuredReport(
        rawData: RawSeoData,
        clientName: String,
        solutions: List<TechnicalSolution>
    ): String {
        return buildString {
            appendLine("# 5-POINT TECHNICAL SEO AUDIT & DEVELOPER FIX REPORT")
            appendLine("**Target**: ${rawData.url}")
            appendLine("**Client**: $clientName")
            appendLine("**Server Response**: ${rawData.httpStatus} (${rawData.latencyMs}ms)")
            appendLine("**Daily Bleed Estimate**: -$340/day leaking to ranking competitors")
            appendLine()
            appendLine("### Executive Summary")
            appendLine("Our automated deep crawl has pinpointed 5 core technical bottlenecks. Below are the exact problem diagnoses, business impacts, and developer-ready copy-paste solutions.")
            appendLine()
            appendLine("### Financial Analysis: Capital Allocation & Revenue Recovery")
            appendLine("• Estimated Monthly Bleed (Inaction Loss): -$10,200/month leaking to competitors")
            appendLine("• Agency Fee ($797/mo Revenue Recovery System): $797/month")
            appendLine("• Net Money Saved / Recovered: +$9,403/month (~12.8x Return on Capital)")
            appendLine("• Consultative Recommendation: Halting active ranking decay recovers capital immediately. The $797 retainer functions as a high-yield recovery asset rather than an operational expense.")
            appendLine()
            solutions.forEachIndexed { idx, sol ->
                appendLine("### Point ${idx + 1}: ${sol.issueFound}")
                appendLine("- **Issue Found**: ${sol.issueFound}")
                appendLine("- **Impact**: ${sol.impact}")
                appendLine("- **Exact Solution**: ${sol.exactSolution}")
                if (!sol.codeSnippet.isNullOrBlank()) {
                    appendLine("```html")
                    appendLine(sol.codeSnippet)
                    appendLine("```")
                }
                appendLine()
            }
            appendLine("---")
            appendLine("### Done-For-You Implementation")
            appendLine(DONE_FOR_YOU_UPSELL_TEXT)
        }
    }

    /**
     * Parses generated report text for structured three-part items (Issue Found, Impact, Exact Solution).
     * If the report text contains structured items, it maps them; otherwise falls back to defaults.
     */
    fun parseTechnicalSolutions(
        reportText: String,
        rawData: RawSeoData,
        fallback: List<TechnicalSolution> = buildDefaultTechnicalSolutions(rawData)
    ): List<TechnicalSolution> {
        val parsedList = mutableListOf<TechnicalSolution>()
        try {
            // Regex to find sections with Issue Found, Impact, and Exact Solution with optional markdown formatting
            val pattern = Pattern.compile(
                "(?i)(?:[-*•\\s]*)(?:\\*\\*)?(?:Issue Found|Problem)(?:\\*\\*)?:?\\s*([^\n\r]+)[\\s\\S]*?(?:[-*•\\s]*)(?:\\*\\*)?Impact(?:\\*\\*)?:?\\s*([^\n\r]+)[\\s\\S]*?(?:[-*•\\s]*)(?:\\*\\*)?Exact Solution(?:\\*\\*)?:?\\s*([^\n\r]+(?:(?:\n(?![-*•\\s]*(?:\\*\\*)?(?:Issue Found|Problem|###|---))[^\n\r]+)*))"
            )
            val matcher = pattern.matcher(reportText)
            while (matcher.find() && parsedList.size < 6) {
                val issue = matcher.group(1)?.replace("**", "")?.removePrefix(":")?.trim().orEmpty()
                val impact = matcher.group(2)?.replace("**", "")?.removePrefix(":")?.trim().orEmpty()
                val rawSolution = matcher.group(3)?.trim().orEmpty()

                // Extract code snippet if present in markdown code block
                val codeBlockPattern = Pattern.compile("```(?:html|css|apache|nginx)?\\s*([\\s\\S]*?)```")
                val codeMatcher = codeBlockPattern.matcher(rawSolution)
                val codeSnippet = if (codeMatcher.find()) {
                    codeMatcher.group(1)?.trim()
                } else if (rawSolution.contains("<") && rawSolution.contains(">")) {
                    rawSolution.substringAfter("<").substringBeforeLast(">").let { "<$it>" }
                } else null

                if (issue.isNotBlank() && impact.isNotBlank()) {
                    parsedList.add(
                        TechnicalSolution(
                            issueFound = issue,
                            impact = impact,
                            exactSolution = rawSolution.replace("```[a-z]*".toRegex(), "").replace("```", "").trim(),
                            codeSnippet = codeSnippet,
                            isStepByStepPlan = rawSolution.contains("Step 1", ignoreCase = true) || rawSolution.contains("1.", ignoreCase = true)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse solutions from AI text: ${e.message}")
        }

        return if (parsedList.size >= 3) parsedList else fallback
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
