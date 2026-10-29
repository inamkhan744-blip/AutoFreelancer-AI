package com.example.data.service

import android.util.Log
import com.example.data.model.LeadEnrichmentData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class LeadEnrichmentEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    private val tag = "LeadEnrichmentEngine"

    suspend fun enrichWebsite(url: String): LeadEnrichmentData = withContext(Dispatchers.IO) {
        val cleanUrl = normalizeUrl(url)
        if (cleanUrl.isBlank()) {
            return@withContext LeadEnrichmentData(url = url)
        }

        try {
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext fallbackEnrichment(cleanUrl)
                }

                val html = response.body?.string().orEmpty()
                if (html.isBlank()) {
                    return@withContext fallbackEnrichment(cleanUrl)
                }

                val title = extractTitle(html)
                val description = extractMetaDescription(html)
                val h1 = extractH1(html)
                val techStack = detectTechStack(html)
                val keywords = detectKeywords(html)
                val niche = inferNiche(title, description, h1, techStack, keywords)

                LeadEnrichmentData(
                    url = cleanUrl,
                    pageTitle = title,
                    metaDescription = description,
                    detectedTechStack = techStack,
                    detectedKeywords = keywords,
                    productOrServiceSummary = niche,
                    rawH1 = h1,
                    isScrapedSuccessfully = true
                )
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to enrich website '$cleanUrl': ${e.message}")
            fallbackEnrichment(cleanUrl)
        }
    }

    private fun normalizeUrl(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed.contains("linkedin.com") || trimmed.contains("reddit.com") || trimmed.contains("x.com")) {
            return ""
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        return trimmed
    }

    private fun extractTitle(html: String): String {
        val matcher = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
        return if (matcher.find()) {
            cleanHtmlText(matcher.group(1).orEmpty())
        } else {
            ""
        }
    }

    private fun extractMetaDescription(html: String): String {
        val patterns = listOf(
            Pattern.compile("<meta[^>]+name=[\"']description[\"'][^>]+content=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE or Pattern.DOTALL),
            Pattern.compile("<meta[^>]+content=[\"'](.*?)[\"'][^>]+name=[\"']description[\"']", Pattern.CASE_INSENSITIVE or Pattern.DOTALL),
            Pattern.compile("<meta[^>]+property=[\"']og:description[\"'][^>]+content=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        )
        for (p in patterns) {
            val matcher = p.matcher(html)
            if (matcher.find()) {
                val desc = cleanHtmlText(matcher.group(1).orEmpty())
                if (desc.isNotBlank()) return desc
            }
        }
        return ""
    }

    private fun extractH1(html: String): String {
        val matcher = Pattern.compile("<h1[^>]*>(.*?)</h1>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
        return if (matcher.find()) {
            cleanHtmlText(matcher.group(1).orEmpty())
        } else {
            ""
        }
    }

    private fun cleanHtmlText(raw: String): String {
        return raw.replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun detectTechStack(html: String): List<String> {
        val detected = mutableListOf<String>()
        val lower = html.lowercase()

        if (lower.contains("cdn.shopify.com") || lower.contains("shopify.theme") || lower.contains("myshopify")) {
            detected.add("Shopify")
        }
        if (lower.contains("wp-content") || lower.contains("wordpress")) {
            detected.add("WordPress")
        }
        if (lower.contains("woocommerce")) {
            detected.add("WooCommerce")
        }
        if (lower.contains("static.squarespace.com") || lower.contains("squarespace")) {
            detected.add("Squarespace")
        }
        if (lower.contains("wix.com") || lower.contains("wixsite")) {
            detected.add("Wix")
        }
        if (lower.contains("webflow.com") || lower.contains("wf-")) {
            detected.add("Webflow")
        }
        if (lower.contains("js.stripe.com")) {
            detected.add("Stripe Checkout")
        }
        if (lower.contains("hubspot") || lower.contains("hs-scripts")) {
            detected.add("HubSpot")
        }

        return detected.distinct()
    }

    private fun detectKeywords(html: String): List<String> {
        val lower = html.lowercase()
        val keywords = mutableListOf<String>()

        val checkList = listOf(
            "e-commerce", "shopify", "agency", "handmade", "organic", "dental",
            "medical", "legal", "law firm", "consulting", "real estate", "saas",
            "b2b", "subscription", "skincare", "fitness", "plumbing", "contractor"
        )

        for (kw in checkList) {
            if (lower.contains(kw)) {
                keywords.add(kw.replaceFirstChar { it.uppercase() })
            }
        }
        return keywords.distinct().take(6)
    }

    private fun inferNiche(title: String, desc: String, h1: String, tech: List<String>, kw: List<String>): String {
        val combined = "$title $desc $h1 ${kw.joinToString(" ")}".lowercase()
        return when {
            combined.contains("handmade") || combined.contains("soap") || combined.contains("artisan") || combined.contains("craft") -> "Artisanal & Handmade Goods"
            combined.contains("shopify") || tech.contains("Shopify") || tech.contains("WooCommerce") || combined.contains("store") || combined.contains("cart") -> "Direct-To-Consumer (DTC) E-Commerce"
            combined.contains("agency") || combined.contains("marketing") || combined.contains("studio") -> "Creative / Marketing Agency"
            combined.contains("law") || combined.contains("attorney") || combined.contains("legal") -> "Legal Services & Law Practice"
            combined.contains("dental") || combined.contains("dentist") || combined.contains("teeth") -> "Dental Clinic & Practice"
            combined.contains("clinic") || combined.contains("health") || combined.contains("medical") -> "Healthcare & Wellness Clinic"
            combined.contains("saas") || combined.contains("software") || combined.contains("platform") -> "B2B SaaS / Software Platform"
            combined.contains("real estate") || combined.contains("property") || combined.contains("realtor") -> "Real Estate & Property Management"
            combined.contains("plumb") || combined.contains("hvac") || combined.contains("roof") -> "Local Trade & Home Services"
            else -> "Independent Online Business"
        }
    }

    private fun fallbackEnrichment(url: String): LeadEnrichmentData {
        val domain = runCatching {
            url.substringAfter("://").substringBefore("/").replace("www.", "")
        }.getOrDefault("domain")
        val brandGuess = domain.substringBefore(".").ifBlank { "Client" }.replaceFirstChar { it.uppercase() }
        return LeadEnrichmentData(
            url = url,
            pageTitle = "$brandGuess Website",
            metaDescription = "Digital storefront and services for $brandGuess.",
            detectedTechStack = listOf("Modern Web"),
            detectedKeywords = listOf("Business", "Digital Growth"),
            productOrServiceSummary = "Independent Online Brand ($brandGuess)",
            rawH1 = "Welcome to $brandGuess",
            isScrapedSuccessfully = false
        )
    }
}
