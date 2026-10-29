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
    val siteTitle: String get() = pageTitle
    val techStack: List<String> get() = detectedTechStack
    val nicheSummary: String get() = productOrServiceSummary.ifBlank { detectedKeywords.take(2).joinToString(", ") }
    val customFirstLineHook: String get() = "I was looking at $url and noticed your focus on ${nicheSummary.ifBlank { pageTitle.take(30) }}..."

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
        fun fromJson(json: JSONObject): LeadEnrichmentData {
            val tech = mutableListOf<String>()
            val techArr = json.optJSONArray("detectedTechStack")
            if (techArr != null) {
                for (i in 0 until techArr.length()) {
                    tech.add(techArr.optString(i))
                }
            }

            val kw = mutableListOf<String>()
            val kwArr = json.optJSONArray("detectedKeywords")
            if (kwArr != null) {
                for (i in 0 until kwArr.length()) {
                    kw.add(kwArr.optString(i))
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

        fun fromMap(map: Map<String, Any>): LeadEnrichmentData {
            @Suppress("UNCHECKED_CAST")
            val tech = (map["detectedTechStack"] as? List<String>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val kw = (map["detectedKeywords"] as? List<String>) ?: emptyList()

            return LeadEnrichmentData(
                url = map["url"] as? String ?: "",
                pageTitle = map["pageTitle"] as? String ?: "",
                metaDescription = map["metaDescription"] as? String ?: "",
                detectedTechStack = tech,
                detectedKeywords = kw,
                productOrServiceSummary = map["productOrServiceSummary"] as? String ?: "",
                rawH1 = map["rawH1"] as? String ?: "",
                isScrapedSuccessfully = map["isScrapedSuccessfully"] as? Boolean ?: false
            )
        }
    }
}
