package com.example.data.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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

    @Test
    fun `findValidEmail filters out fake, disposable, and asset emails`() {
        val service = SearchProspectService()

        val htmlWithFake = """
            Contact us at noreply@example.com or 408309@email4pr.com.
            Also logo is icon@2x.png and tracker is beacon@sentry.io.
            For direct inquiries, email partnerships@apexdevops.io!
        """.trimIndent()

        val found = service.findValidEmail(htmlWithFake)
        assertEquals("partnerships@apexdevops.io", found)
    }

    @Test
    fun `findValidEmail returns null when only fake emails are present`() {
        val service = SearchProspectService()

        val htmlOnlyFake = "Send reports to no-reply@example.org or info@email4pr.com or dev@wixpress.com"
        assertNull(service.findValidEmail(htmlOnlyFake))
    }

    @Test
    fun `fallback prospects provide valid contact details and URLs`() {
        val service = SearchProspectService()
        val prospects = service.getFallbackProspects("SEO audit")

        assertTrue(prospects.isNotEmpty())
        for (prospect in prospects) {
            val email = service.findValidEmail(prospect.snippet)
            assertNotNull("Prospect ${prospect.title} should have an extractable email in snippet", email)
            assertTrue(prospect.link.startsWith("https://"))
        }
    }

    @Test
    fun `testBrave and testGoogleCse fail gracefully with invalid keys`() = runBlocking {
        val service = SearchProspectService()
        val braveRes = service.testBrave("invalid_test_key")
        assertTrue(braveRes.isFailure)

        val cseRes = service.testGoogleCse("invalid_key", "invalid_cx")
        assertTrue(cseRes.isFailure)
    }
}
