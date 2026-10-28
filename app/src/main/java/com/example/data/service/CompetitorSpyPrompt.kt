package com.example.data.service

import com.example.data.model.ProspectResult

object CompetitorSpyPrompt {
    private val websitePattern = Regex("(?:https?://)?(?:www\\.)?[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+", RegexOption.IGNORE_CASE)
    private val excludedHosts = setOf(
        "linkedin.com", "reddit.com", "x.com", "twitter.com", "facebook.com", "instagram.com",
        "youtube.com", "tiktok.com", "pinterest.com", "medium.com"
    )

    fun websiteFor(prospect: ProspectResult): String {
        val source = "${prospect.snippet}\n${prospect.title}"
        return sequenceOf(prospect.link, source)
            .flatMap { websitePattern.findAll(it).map { match -> match.value.trimEnd('.', ',', ';', ')', ']') } }
            .firstOrNull { candidate ->
                val host = candidate.removePrefix("https://").removePrefix("http://").removePrefix("www.")
                    .substringBefore('/').lowercase()
                excludedHosts.none { host == it || host.endsWith(".$it") }
            }
            ?: ""
    }

    fun instructionsFor(prospect: ProspectResult): String {
        val website = websiteFor(prospect).ifBlank {
            "No client website URL was provided; infer a likely business category from the company name and post context."
        }
        return instructions(website, "${prospect.snippet}\n${prospect.title}")
    }

    fun instructions(targetWebsite: String, businessContext: String): String = """
        COMPETITOR SPY ANALYSIS:
        Target website or business: $targetWebsite
        Context: $businessContext
        Identify three plausible top direct competitors for this business, naming them when reliable or stating
        the competitor categories when not. Compare their likely public positioning, search visibility, content
        coverage, and conversion experience against the target. Use actual supplied
        website evidence when present; otherwise label comparisons as hypotheses and never claim you browsed
        or verified live competitor data. Turn the strongest specific gap into a helpful, persuasive pitch.
    """.trimIndent()
}