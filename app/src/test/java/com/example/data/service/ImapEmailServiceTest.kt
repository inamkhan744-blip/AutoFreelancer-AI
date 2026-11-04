package com.example.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImapEmailServiceTest {

    private val service = ImapEmailService()

    @Test
    fun stripQuotedText_removesHistoricalEmailChain() {
        val incomingEmail = """
            Hey, thanks for reaching out. Yes, we've noticed our organic traffic dropping and would like to review the audit.
            Are you available Thursday at 2 PM EST?

            On Fri, 30 Oct 2026 at 14:22, AutoFreelancer AI <alex@agency.com> wrote:
            > Hi Marcus, noticed your Shopify store isn't ranking for target keywords...
            > Best regards,
            > Alex
        """.trimIndent()

        val stripped = service.stripQuotedText(incomingEmail)
        assertTrue(stripped.contains("Are you available Thursday at 2 PM EST?"))
        assertFalse(stripped.contains("On Fri, 30 Oct 2026 at 14:22"))
        assertFalse(stripped.contains("Hi Marcus, noticed your Shopify store"))
    }

    @Test
    fun stripQuotedText_removesBlockquotesAndDeviceSignatures() {
        val emailWithQuotes = """
            Sounds great, let's schedule a call.

            Sent from my iPhone

            > What is your pricing tier?
            > Thanks
        """.trimIndent()

        val stripped = service.stripQuotedText(emailWithQuotes)
        assertEquals("Sounds great, let's schedule a call.", stripped)
    }

    @Test
    fun isAgency_identifiesKnownAgencyDomainsAndPhrases() {
        assertTrue(SearchProspectService.isAgency("SmartSites - Digital Marketing", "Top SEO agency in NJ", "https://smartsites.com"))
        assertTrue(SearchProspectService.isAgency("WebFX Digital", "Full-service digital agency", "https://webfx.com"))
        assertTrue(SearchProspectService.isAgency("Top SEO Agency", "We offer SEO services and PPC", "https://customagency.com"))

        // Genuine prospective client seeking help
        assertFalse(SearchProspectService.isAgency("Marcus Vance - Founder", "Looking for technical SEO audit for our ecommerce store", "https://nexagrowth-digital.com"))
        assertFalse(SearchProspectService.isAgency("Handmade Soaps Online", "Organic skincare shop looking for help with rankings", "https://auraskinhealth.com"))
    }
}
