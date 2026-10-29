package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class LeadEnrichmentData(
    val url: String = "",
    val pageTitle: String = "",
    val metaDescription: String = "",
    val detectedTechStack: List<String> = emptyList(),
    val detectedKeywords: List<String> = emptyList(),
    val productOrServiceSummary: String = "",
    val rawH1: String = "",
    val isScrapedSuccessfully: Boolean = false
) {
    val siteTitle: String get() = pageTitle.ifBlank { "Website" }
    val techStack: List<String> get() = detectedTechStack
    val nicheSummary: String get() = productOrServiceSummary.ifBlank { detectedKeywords.take(2).joinToString(", ") }.ifBlank { "Online Business" }
    val customFirstLineHook: String get() = if (url.isNotBlank()) {
        "I was looking at $url and noticed your focus on ${nicheSummary.take(40)}..."
    } else {
        "I was reviewing your online presence and noticed your focus on ${nicheSummary.take(40)}..."
    }

    fun toSummaryPrompt(): String {
        val techStr = if (detectedTechStack.isNotEmpty()) "Tech Stack: ${detectedTechStack.joinToString(", ")}" else ""
        val kwStr = if (detectedKeywords.isNotEmpty()) "Keywords: ${detectedKeywords.joinToString(", ")}" else ""
        val h1Str = if (rawH1.isNotBlank()) "Headline (H1): $rawH1" else ""
        val titleStr = if (pageTitle.isNotBlank()) "Page Title: $pageTitle" else ""
        val descStr = if (metaDescription.isNotBlank()) "Description: $metaDescription" else ""
        val productStr = if (productOrServiceSummary.isNotBlank()) "Business Niche: $productOrServiceSummary" else ""

        return listOf(titleStr, descStr, h1Str, techStr, kwStr, productStr)
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("url", url)
            put("pageTitle", pageTitle)
            put("metaDescription", metaDescription)
            put("detectedTechStack", JSONArray(detectedTechStack))
            put("detectedKeywords", JSONArray(detectedKeywords))
            put("productOrServiceSummary", productOrServiceSummary)
            put("rawH1", rawH1)
            put("isScrapedSuccessfully", isScrapedSuccessfully)
        }
    }

    companion object {
        fun fromJson(json: JSONObject?): LeadEnrichmentData {
            if (json == null) return LeadEnrichmentData()
            val tech = mutableListOf<String>()
            val techArr = json.optJSONArray("detectedTechStack")
            if (techArr != null) {
                for (i in 0 until techArr.length()) {
                    val item = techArr.optString(i, "")
                    if (item.isNotBlank()) tech.add(item)
                }
            }

            val kw = mutableListOf<String>()
            val kwArr = json.optJSONArray("detectedKeywords")
            if (kwArr != null) {
                for (i in 0 until kwArr.length()) {
                    val item = kwArr.optString(i, "")
                    if (item.isNotBlank()) kw.add(item)
                }
            }

            return LeadEnrichmentData(
                url = json.optString("url", ""),
                pageTitle = json.optString("pageTitle", ""),
                metaDescription = json.optString("metaDescription", ""),
                detectedTechStack = tech,
                detectedKeywords = kw,
                productOrServiceSummary = json.optString("productOrServiceSummary", ""),
                rawH1 = json.optString("rawH1", ""),
                isScrapedSuccessfully = json.optBoolean("isScrapedSuccessfully", false)
            )
        }

        fun fromMap(map: Map<String, Any?>?): LeadEnrichmentData {
            if (map == null) return LeadEnrichmentData()
            @Suppress("UNCHECKED_CAST")
            val tech = (map["detectedTechStack"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val kw = (map["detectedKeywords"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

            return LeadEnrichmentData(
                url = map["url"]?.toString().orEmpty(),
                pageTitle = map["pageTitle"]?.toString().orEmpty(),
                metaDescription = map["metaDescription"]?.toString().orEmpty(),
                detectedTechStack = tech,
                detectedKeywords = kw,
                productOrServiceSummary = map["productOrServiceSummary"]?.toString().orEmpty(),
                rawH1 = map["rawH1"]?.toString().orEmpty(),
                isScrapedSuccessfully = (map["isScrapedSuccessfully"] as? Boolean) ?: false
            )
        }
    }
}
