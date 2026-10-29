package com.example.data.service

import org.junit.Assert.assertTrue
import org.junit.Test

class SearchProspectServiceTest {
    @Test
    fun `search variations use topic and distinct intent sources`() {
        val service = SearchProspectService()

        val variations = service.searchVariations(
            """looking for SEO audit OR "need an SEO audit" site:linkedin.com OR site:reddit.com"""
        )

        assertTrue("need SEO audit help site:reddit.com" in variations)
        assertTrue("hiring SEO audit expert site:linkedin.com" in variations)
        assertTrue("looking for SEO audit site:x.com" in variations)
        assertTrue("recommend SEO audit agency" in variations)
        assertTrue(variations.distinct().size == variations.size)
    }
}
