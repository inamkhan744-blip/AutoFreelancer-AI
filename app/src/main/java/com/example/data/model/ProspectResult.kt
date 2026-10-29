package com.example.data.model

data class ProspectResult(
    val title: String,
    val link: String,
    val snippet: String,
    val sourcePlatform: String,
    val queryMatched: String = "",
    val enrichment: LeadEnrichmentData? = null
)

enum class SearchProvider(val displayName: String) {
    SERPER("Serper API (Google Search)"),
    GOOGLE_CSE("Google Custom Search API")
}
