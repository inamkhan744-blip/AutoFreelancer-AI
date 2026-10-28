package com.example.data.service

import com.example.data.model.ProspectResult
import org.junit.Assert.assertTrue
import org.junit.Test

class CompetitorSpyPromptTest {
    @Test
    fun instructionsFor_usesClientWebsiteAndAvoidsTreatingSocialPostAsWebsite() {
        val withWebsite = CompetitorSpyPrompt.instructionsFor(
            ProspectResult("SEO Lead", "https://acme.example", "Need SEO help", "Reddit")
        )
        val withoutWebsite = CompetitorSpyPrompt.instructionsFor(
            ProspectResult("Need a Shopify audit", "https://reddit.com/post", "Traffic dropped this month", "Reddit")
        )

        assertTrue(withWebsite.contains("https://acme.example"))
        assertTrue(withWebsite.contains("three plausible top direct competitors"))
        assertTrue(withoutWebsite.contains("No client website URL was provided"))
        assertTrue(withoutWebsite.contains("never claim you browsed"))
    }
}