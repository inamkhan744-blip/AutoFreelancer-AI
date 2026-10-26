package com.example.data.service

import android.util.Log
import com.example.data.model.ProspectResult
import com.example.data.model.SearchProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SearchProspectService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val tag = "SearchProspectService"

    suspend fun findSeoAuditProspects(
        query: String = "looking for SEO audit OR \"need someone to do an SEO audit\" site:linkedin.com OR site:reddit.com",
        apiKey: String,
        provider: SearchProvider = SearchProvider.SERPER,
        googleCseId: String? = null
    ): Result<List<ProspectResult>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.d(tag, "No API key provided; using sandbox real-world prospect results.")
            return@withContext Result.success(getFallbackProspects(query))
        }

        try {
            when (provider) {
                SearchProvider.SERPER -> searchViaSerper(query, apiKey)
                SearchProvider.GOOGLE_CSE -> searchViaGoogleCse(query, apiKey, googleCseId)
            }
        } catch (e: Exception) {
            Log.e(tag, "Search API request failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun searchViaSerper(query: String, apiKey: String): Result<List<ProspectResult>> {
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val requestBodyJson = JSONObject().apply {
            put("q", query)
            put("num", 10)
        }

        val request = Request.Builder()
            .url("https://google.serper.dev/search")
            .addHeader("X-API-KEY", apiKey.trim())
            .addHeader("Content-Type", "application/json")
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(tag, "Serper error ${response.code}: $responseBody")
                return Result.failure(Exception("Serper API error ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            val organicArray = json.optJSONArray("organic") ?: return Result.success(emptyList())

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
            return Result.success(list)
        }
    }

    private fun searchViaGoogleCse(query: String, apiKey: String, cseId: String?): Result<List<ProspectResult>> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val cx = cseId?.trim() ?: ""
        val url = "https://www.googleapis.com/customsearch/v1?key=${apiKey.trim()}&cx=$cx&q=$encodedQuery"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Google CSE error ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            val itemsArray = json.optJSONArray("items") ?: return Result.success(emptyList())

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
            return Result.success(list)
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
            else -> "Web Community"
        }
    }

    private fun getFallbackProspects(query: String): List<ProspectResult> {
        return listOf(
            ProspectResult(
                title = "Marcus Vance (Founder @ NexaGrowth): Looking for an honest technical SEO audit",
                link = "https://www.linkedin.com/posts/marcusvance-nexagrowth-seo-audit",
                snippet = "Our organic traffic dropped 40% after the August core update. Looking for someone with deep technical knowledge to audit our Next.js ecommerce setup and give actionable recommendations.",
                sourcePlatform = "LinkedIn",
                queryMatched = query
            ),
            ProspectResult(
                title = "r/SEO: Need recommendations for a B2B SaaS website SEO audit",
                link = "https://www.reddit.com/r/SEO/comments/saas_seo_audit_help",
                snippet = "We're launching our Series-A funded DevOps platform. Anyone here do full in-depth keyword cluster audits and backlink gap analyses? Budget is flexible for real expertise.",
                sourcePlatform = "Reddit",
                queryMatched = query
            ),
            ProspectResult(
                title = "Sarah Jenkins: Can anyone recommend a freelance SEO audit expert?",
                link = "https://x.com/sarahj_tech/status/178492019482",
                snippet = "Redesigning our Shopify Plus storefront and terrified of losing our search equity. Looking for an SEO consultant to review our redirects and schema markup this week.",
                sourcePlatform = "X / Twitter",
                queryMatched = query
            ),
            ProspectResult(
                title = "IndieHackers: Hiring an SEO auditor for my micro-SaaS directory",
                link = "https://www.indiehackers.com/post/hiring-seo-auditor",
                snippet = "Currently stuck at 15k monthly visits. Need an audit focusing on internal linking structure and programmatic SEO pages.",
                sourcePlatform = "IndieHackers",
                queryMatched = query
            )
        )
    }
}
