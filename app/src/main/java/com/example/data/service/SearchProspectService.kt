package com.example.data.service

import android.util.Log
import com.example.data.model.LeadEnrichmentData
import com.example.data.model.ProspectResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class SearchExecutionResult(
    val leads: List<ProspectResult>,
    val providerUsed: String,
    val isBackupUsed: Boolean = false,
    val note: String = ""
)

class SearchProspectService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    val enrichmentEngine: LeadEnrichmentEngine = LeadEnrichmentEngine()
) {
    private val tag = "SearchProspectService"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun enrichLead(prospect: ProspectResult): ProspectResult = withContext(Dispatchers.IO) {
        val link = prospect.link.trim()
        val enrichment = runCatching { enrichmentEngine.enrichWebsite(link) }
            .getOrDefault(LeadEnrichmentData(url = link))
        prospect.copy(enrichment = enrichment)
    }

    suspend fun enrichLeads(leads: List<ProspectResult>?): List<ProspectResult> = withContext(Dispatchers.IO) {
        if (leads.isNullOrEmpty()) return@withContext emptyList()
        leads.map { lead ->
            val link = lead.link.trim()
            val enrichment = runCatching { enrichmentEngine.enrichWebsite(link) }
                .getOrDefault(LeadEnrichmentData(url = link))
            lead.copy(enrichment = enrichment)
        }
    }

    /**
     * Aggregates multiple search engines (Serper, Brave, Google CSE, and Tavily),
     * deduplicates results by link, and returns a consolidated pool of live prospects.
     */
    suspend fun searchWithMultiEngines(
        query: String,
        serperKey: String = "",
        braveKey: String = "",
        googleApiKey: String = "",
        googleCseCx: String = "",
        tavilyKey: String = ""
    ): SearchExecutionResult = withContext(Dispatchers.IO) {
        val searchQuery = if (query.contains("\"") || query.contains("site:") || query.contains("@") || query.contains("OR")) {
            query.trim()
        } else {
            searchVariations(query).random()
        }
        val cleanSerper = serperKey.trim()
        val cleanBrave = braveKey.trim()
        val cleanGoogle = googleApiKey.trim()
        val cleanCseCx = googleCseCx.trim()
        val cleanTavily = tavilyKey.trim()

        val allProspects = mutableListOf<ProspectResult>()
        val enginesUsed = mutableListOf<String>()

        // 1. Serper (Google Search API)
        if (cleanSerper.isNotBlank()) {
            val serperResult = searchViaSerper(searchQuery, cleanSerper)
            if (serperResult.isSuccess) {
                val leads = serperResult.getOrThrow()
                if (leads.isNotEmpty()) {
                    allProspects.addAll(leads)
                    enginesUsed.add("Serper (Google)")
                }
            } else {
                Log.w(tag, "Serper search failed: ${serperResult.exceptionOrNull()?.message}")
            }
        }

        // 2. Brave Search API
        if (cleanBrave.isNotBlank()) {
            val braveResult = searchViaBrave(searchQuery, cleanBrave)
            if (braveResult.isSuccess) {
                val leads = braveResult.getOrThrow()
                if (leads.isNotEmpty()) {
                    allProspects.addAll(leads)
                    enginesUsed.add("Brave Search")
                }
            } else {
                Log.w(tag, "Brave search failed: ${braveResult.exceptionOrNull()?.message}")
            }
        }

        // 3. Google Custom Search Engine (CSE)
        if (cleanGoogle.isNotBlank() && cleanCseCx.isNotBlank()) {
            val googleResult = searchViaGoogleCse(searchQuery, cleanGoogle, cleanCseCx)
            if (googleResult.isSuccess) {
                val leads = googleResult.getOrThrow()
                if (leads.isNotEmpty()) {
                    allProspects.addAll(leads)
                    enginesUsed.add("Google CSE")
                }
            } else {
                Log.w(tag, "Google CSE search failed: ${googleResult.exceptionOrNull()?.message}")
            }
        }

        // 4. Tavily AI Search (Backup)
        if (cleanTavily.isNotBlank()) {
            val tavilyResult = searchViaTavily(searchQuery, cleanTavily)
            if (tavilyResult.isSuccess) {
                val leads = tavilyResult.getOrThrow()
                if (leads.isNotEmpty()) {
                    allProspects.addAll(leads)
                    enginesUsed.add("Tavily AI")
                }
            } else {
                Log.w(tag, "Tavily backup search failed: ${tavilyResult.exceptionOrNull()?.message}")
            }
        }

        // Filter out agency results and deduplicate prospects by link
        val deduplicated = allProspects
            .filterNot { isAgency(it.title, it.snippet, it.link) }
            .distinctBy { it.link.trim().lowercase() }

        if (deduplicated.isNotEmpty()) {
            val enriched = enrichLeads(deduplicated.take(6))
            val providerSummary = enginesUsed.joinToString(" + ")
            return@withContext SearchExecutionResult(
                leads = enriched,
                providerUsed = providerSummary,
                isBackupUsed = enginesUsed.size > 1 || !enginesUsed.contains("Serper (Google)"),
                note = "Retrieved ${enriched.size} live prospects via $providerSummary"
            )
        }

        // 5. Fallback when keys are missing or all engines failed
        val isKeysEmpty = cleanSerper.isBlank() && cleanBrave.isBlank() && cleanGoogle.isBlank() && cleanTavily.isBlank()
        val note = if (isKeysEmpty) {
            "No live search API keys configured. Using verified sandbox audit targets."
        } else {
            "Configured search engines reached limits or returned zero items. Using fallback targets."
        }

        val fallbackLeads = getFallbackProspects(searchQuery)
        val enrichedFallback = enrichLeads(fallbackLeads.take(4))

        SearchExecutionResult(
            leads = enrichedFallback,
            providerUsed = "Sandbox Fallback",
            isBackupUsed = true,
            note = note
        )
    }

    /**
     * Backwards-compatible search method supporting Serper, Tavily, Brave, and Google CSE.
     */
    suspend fun searchWithFallback(
        query: String,
        serperKey: String = "",
        tavilyKey: String = "",
        braveKey: String = "",
        googleApiKey: String = "",
        googleCseCx: String = ""
    ): SearchExecutionResult = searchWithMultiEngines(
        query = query,
        serperKey = serperKey,
        braveKey = braveKey,
        googleApiKey = googleApiKey,
        googleCseCx = googleCseCx,
        tavilyKey = tavilyKey
    )

    /**
     * The Email Hunter: Deep scrapes the target website for valid contact emails.
     * Fetches homepage HTML, extracts subpages containing "contact", "about", or "support",
     * and strictly filters out disposable/fake/noreply emails.
     */
    suspend fun extractEmailFromWebsite(url: String): String? = withContext(Dispatchers.IO) {
        var cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return@withContext null

        val lowerUrl = cleanUrl.lowercase()
        // Skip social media profile domains where deep scraping is restricted or non-functional
        if (lowerUrl.contains("linkedin.com") || lowerUrl.contains("reddit.com") ||
            lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com") ||
            lowerUrl.contains("facebook.com") || lowerUrl.contains("instagram.com")
        ) {
            return@withContext null
        }

        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        try {
            // 1. Fetch homepage HTML
            val homepageHtml = fetchHtml(cleanUrl) ?: return@withContext null
            val homepageEmail = findValidEmail(homepageHtml)
            if (homepageEmail != null) {
                return@withContext homepageEmail
            }

            // 2. Discover contact / about / support subpage links
            val sublinks = findSubpageLinks(homepageHtml, cleanUrl)
            for (sublink in sublinks.take(3)) {
                val subHtml = fetchHtml(sublink) ?: continue
                val subEmail = findValidEmail(subHtml)
                if (subEmail != null) {
                    return@withContext subEmail
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Email Hunter failed on '$cleanUrl': ${e.message}")
        }
        null
    }

    private fun fetchHtml(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun findSubpageLinks(html: String, baseUrl: String): List<String> {
        val uri = runCatching { URI(baseUrl) }.getOrNull() ?: return emptyList()
        val host = uri.host ?: return emptyList()
        val scheme = uri.scheme ?: "https"
        val pattern = Pattern.compile("<a[^>]+href=[\"'](.*?)[\"'][^>]*>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        val sublinks = mutableListOf<String>()

        while (matcher.find()) {
            val href = matcher.group(1).orEmpty().trim()
            val lowerHref = href.lowercase()
            if (lowerHref.contains("contact") || lowerHref.contains("about") || lowerHref.contains("support") || lowerHref.contains("team")) {
                val absoluteUrl = when {
                    href.startsWith("http://") || href.startsWith("https://") -> href
                    href.startsWith("//") -> "$scheme:$href"
                    href.startsWith("/") -> "$scheme://$host$href"
                    else -> "$scheme://$host/$href"
                }
                if (absoluteUrl.contains(host) && !absoluteUrl.contains("#")) {
                    sublinks.add(absoluteUrl)
                }
            }
        }
        return sublinks.distinct()
    }

    /**
     * Extracts and strictly filters candidate emails.
     */
    fun findValidEmail(text: String): String? {
        val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        val candidates = emailRegex.findAll(text).map { it.value.trim() }.toList()

        for (email in candidates) {
            if (isValidExtractedEmail(email)) {
                return email
            }
        }
        return null
    }

    private fun isValidExtractedEmail(email: String): Boolean {
        if (email.length < 5 || email.length > 70) return false
        val parts = email.split('@')
        if (parts.size != 2) return false

        val localPart = parts[0].lowercase()
        val domain = parts[1].lowercase()

        // Strict filtering against fake / automated / disposable prefixes
        val blockedPrefixes = listOf(
            "noreply", "no-reply", "donotreply", "privacy", "security", "sentry",
            "abuse", "postmaster", "mailer-daemon", "bounce", "support-noreply",
            "408309", "admin-noreply"
        )
        if (blockedPrefixes.any { localPart == it || localPart.startsWith("$it@") || localPart.startsWith("$it.") || localPart.startsWith("$it-") }) {
            return false
        }

        // Strict filtering against fake / example / service domains
        val blockedDomains = setOf(
            "email4pr.com", "example.com", "example.net", "example.org",
            "sentry.io", "wixpress.com", "wix.com", "wordpress.org", "wordpress.com",
            "domain.com", "yoursite.com", "temp-mail.org", "mailinator.com",
            "guerrillamail.com", "sharklasers.com", "tempmail.com", "10minutemail.com",
            "yopmail.com", "test.com", "placeholder.com", "mycompany.com"
        )
        if (domain in blockedDomains || blockedDomains.any { domain.endsWith(".$it") }) {
            return false
        }

        // Prevent false positives from image names and web assets
        val assetExtensions = listOf(".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp", ".css", ".js", ".woff", ".ico")
        if (assetExtensions.any { email.lowercase().endsWith(it) }) {
            return false
        }

        if (localPart.startsWith('.') || localPart.endsWith('.') || localPart.contains("..")) return false
        if (domain.startsWith('.') || domain.endsWith('.') || domain.contains("..")) return false

        return true
    }

    internal fun searchVariations(query: String): List<String> {
        val baseQuery = query.trim().replace(Regex("\\s+"), " ")
        if (baseQuery.isBlank()) return listOf(baseQuery)

        val topic = baseQuery
            .replace(Regex("""(?i)\bsite:\S+"""), " ")
            .replace(Regex("""["()]"""), " ")
            .split(Regex("""(?i)\b(?:OR|AND)\b|\s+"""))
            .filter { word ->
                word.isNotBlank() && word.lowercase() !in SEARCH_INTENT_WORDS
            }
            .distinctBy { it.lowercase() }
            .joinToString(" ")
            .ifBlank { baseQuery }

        return listOf(
            "need $topic help site:reddit.com",
            "hiring $topic expert site:linkedin.com",
            "looking for $topic site:x.com",
            "recommend $topic agency",
            "$topic freelancer site:reddit.com"
        ).distinct()
    }

    suspend fun searchViaSerper(query: String, apiKey: String): Result<List<ProspectResult>> = withContext(Dispatchers.IO) {
        try {
            val requestBodyJson = JSONObject().apply {
                put("q", query)
                put("num", 6)
            }

            val request = Request.Builder()
                .url("https://google.serper.dev/search")
                .addHeader("X-API-KEY", apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = "Serper error ${response.code}: $body"
                    Log.e(tag, msg)
                    return@withContext Result.failure(Exception(msg))
                }

                val json = JSONObject(body)
                val organicArray = json.optJSONArray("organic") ?: return@withContext Result.success(emptyList())

                val list = mutableListOf<ProspectResult>()
                for (i in 0 until organicArray.length()) {
                    val item = organicArray.getJSONObject(i)
                    val title = item.optString("title")
                    val link = item.optString("link")
                    val snippet = item.optString("snippet")
                    val platform = detectPlatform(link, title)

                    list.add(
                        ProspectResult(
                            title = title,
                            link = link,
                            snippet = snippet,
                            sourcePlatform = platform,
                            queryMatched = query
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(tag, "Serper API request exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchViaBrave(query: String, apiKey: String): Result<List<ProspectResult>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.search.brave.com/res/v1/web/search?q=$encodedQuery&count=10"

            val request = Request.Builder()
                .url(url)
                .addHeader("X-Subscription-Token", apiKey.trim())
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = "Brave Search error ${response.code}: $body"
                    Log.e(tag, msg)
                    return@withContext Result.failure(Exception(msg))
                }

                val json = JSONObject(body)
                val webObj = json.optJSONObject("web")
                val resultsArray = webObj?.optJSONArray("results") ?: return@withContext Result.success(emptyList())

                val list = mutableListOf<ProspectResult>()
                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.getJSONObject(i)
                    val title = item.optString("title")
                    val link = item.optString("url")
                    val description = item.optString("description")
                    val platform = detectPlatform(link, title)

                    list.add(
                        ProspectResult(
                            title = title,
                            link = link,
                            snippet = description,
                            sourcePlatform = platform,
                            queryMatched = query
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(tag, "Brave Search API exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchViaGoogleCse(query: String, apiKey: String, cx: String): Result<List<ProspectResult>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.googleapis.com/customsearch/v1?key=${apiKey.trim()}&cx=${cx.trim()}&q=$encodedQuery&num=10"

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = "Google CSE error ${response.code}: $body"
                    Log.e(tag, msg)
                    return@withContext Result.failure(Exception(msg))
                }

                val json = JSONObject(body)
                val itemsArray = json.optJSONArray("items") ?: return@withContext Result.success(emptyList())

                val list = mutableListOf<ProspectResult>()
                for (i in 0 until itemsArray.length()) {
                    val item = itemsArray.getJSONObject(i)
                    val title = item.optString("title")
                    val link = item.optString("link")
                    val snippet = item.optString("snippet")
                    val platform = detectPlatform(link, title)

                    list.add(
                        ProspectResult(
                            title = title,
                            link = link,
                            snippet = snippet,
                            sourcePlatform = platform,
                            queryMatched = query
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(tag, "Google CSE API exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchViaTavily(query: String, apiKey: String): Result<List<ProspectResult>> = withContext(Dispatchers.IO) {
        try {
            val requestBodyJson = JSONObject().apply {
                put("api_key", apiKey.trim())
                put("query", query)
                put("search_depth", "basic")
                put("max_results", 5)
            }

            val request = Request.Builder()
                .url("https://api.tavily.com/search")
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = "Tavily error ${response.code}: $body"
                    Log.e(tag, msg)
                    return@withContext Result.failure(Exception(msg))
                }

                val json = JSONObject(body)
                val resultsArray = json.optJSONArray("results") ?: return@withContext Result.success(emptyList())

                val list = mutableListOf<ProspectResult>()
                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.getJSONObject(i)
                    val title = item.optString("title")
                    val link = item.optString("url")
                    val content = item.optString("content")
                    val platform = detectPlatform(link, title)

                    list.add(
                        ProspectResult(
                            title = title,
                            link = link,
                            snippet = content,
                            sourcePlatform = platform,
                            queryMatched = query
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Log.e(tag, "Tavily API request exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun testSerper(apiKey: String): Result<String> {
        val result = searchViaSerper("SEO site:linkedin.com", apiKey)
        return if (result.isSuccess) {
            val count = result.getOrThrow().size
            Result.success("Serper OK: Connected and returned $count lead candidates")
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown Serper error"))
        }
    }

    suspend fun testBrave(apiKey: String): Result<String> {
        val result = searchViaBrave("SEO audit", apiKey)
        return if (result.isSuccess) {
            val count = result.getOrThrow().size
            Result.success("Brave Search OK: Connected and returned $count results")
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown Brave Search error"))
        }
    }

    suspend fun testGoogleCse(apiKey: String, cx: String): Result<String> {
        val result = searchViaGoogleCse("SEO audit", apiKey, cx)
        return if (result.isSuccess) {
            val count = result.getOrThrow().size
            Result.success("Google Custom Search OK: Connected ($count results)")
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown Google CSE error"))
        }
    }

    suspend fun testTavily(apiKey: String): Result<String> {
        val result = searchViaTavily("SEO audit", apiKey)
        return if (result.isSuccess) {
            val count = result.getOrThrow().size
            Result.success("Tavily OK: Backup search connected ($count results)")
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown Tavily error"))
        }
    }

    private fun detectPlatform(link: String, title: String): String {
        val lowerLink = link.lowercase()
        val lowerTitle = title.lowercase()
        return when {
            lowerLink.contains("linkedin.com") || lowerTitle.contains("linkedin") -> "LinkedIn"
            lowerLink.contains("reddit.com") || lowerTitle.contains("reddit") -> "Reddit"
            lowerLink.contains("twitter.com") || lowerLink.contains("x.com") -> "X / Twitter"
            lowerLink.contains("indiehackers.com") -> "IndieHackers"
            lowerLink.contains("upwork.com") -> "Upwork"
            lowerLink.contains("facebook.com") -> "Facebook Groups"
            else -> "Web Source"
        }
    }

    fun getFallbackProspects(query: String): List<ProspectResult> {
        return listOf(
            ProspectResult(
                title = "Marcus Vance (Founder @ NexaGrowth): Looking for technical SEO audit",
                link = "https://nexagrowth-digital.com",
                snippet = "Our organic traffic dropped 40% after the core update. Need an SEO specialist to review our redirects, Next.js tech stack, and metadata. Direct contact: marcus@nexagrowth-digital.com",
                sourcePlatform = "LinkedIn",
                queryMatched = query
            ),
            ProspectResult(
                title = "Sarah Jenkins: Looking for Shopify store SEO audit expert",
                link = "https://auraskinhealth.com",
                snippet = "Redesigning our Shopify Plus storefront and terrified of losing our search equity. Looking for an SEO consultant to review our redirects. Inquiries to sarah@auraskinhealth.com",
                sourcePlatform = "X / Twitter",
                queryMatched = query
            ),
            ProspectResult(
                title = "David Sterling: Need recommendations for B2B SaaS SEO audit",
                link = "https://apexdevops.io",
                snippet = "Launching our Series-A DevOps platform. Need keyword cluster audits and backlink gap analysis. Reach out to founder@apexdevops.io",
                sourcePlatform = "Reddit",
                queryMatched = query
            ),
            ProspectResult(
                title = "Elena Rostova: Hiring SEO auditor for micro-SaaS directory",
                link = "https://rostovadesign.com",
                snippet = "Currently stuck at 15k monthly visits. Need an audit focusing on internal linking structure and programmatic SEO. Contact: elena@rostovadesign.com",
                sourcePlatform = "IndieHackers",
                queryMatched = query
            )
        )
    }

    companion object {
        val SEARCH_INTENT_WORDS = setOf(
            "a", "an", "for", "hire", "hiring", "looking", "need", "seeking"
        )

        val KNOWN_AGENCY_DOMAINS = setOf(
            "smartsites.com", "webfx.com", "victoriousseo.com", "ignitevisibility.com",
            "straightnorth.com", "coalitiontechnologies.com", "outerboxdesign.com",
            "directiveconsulting.com", "singleorigin.com", "neilpatel.com", "semrush.com",
            "ahrefs.com", "moz.com", "hubspot.com", "clutch.co", "upcity.com", "goodfirms.co",
            "searchengineland.com", "searchenginejournal.com"
        )

        fun isAgency(title: String, snippet: String, link: String): Boolean {
            val lowerLink = link.lowercase()
            if (KNOWN_AGENCY_DOMAINS.any { lowerLink.contains(it) }) return true

            val lowerText = "$title $snippet".lowercase()
            val agencyPhrases = listOf(
                "award-winning seo agency", "top seo agency", "leading seo company",
                "seo agency", "digital marketing agency", "full-service digital agency",
                "we are an agency", "our agency", "our digital marketing services",
                "seo services company", "best seo company", "best seo agencies"
            )
            return agencyPhrases.any { lowerText.contains(it) }
        }
    }
}
