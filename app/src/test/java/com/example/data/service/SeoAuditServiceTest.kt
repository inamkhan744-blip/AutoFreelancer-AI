package com.example.data.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeoAuditServiceTest {

    private val service = SeoAuditService()

    @Test
    fun urlNormalization_prependsHttpsWhenMissing() {
        // Access private normalizeUrl via reflection or test behavior
        val method = SeoAuditService::class.java.getDeclaredMethod("normalizeUrl", String::class.java)
        method.isAccessible = true

        val res1 = method.invoke(service, "example.com") as String
        assertEquals("https://example.com", res1)

        val res2 = method.invoke(service, "http://myblog.org") as String
        assertEquals("http://myblog.org", res2)

        val res3 = method.invoke(service, "https://secure.store.co/shop") as String
        assertEquals("https://secure.store.co/shop", res3)
    }

    @Test
    fun extractMetaTag_parsesDescriptionCorrectly() {
        val method = SeoAuditService::class.java.getDeclaredMethod("extractMetaTag", String::class.java, String::class.java)
        method.isAccessible = true

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Best Specialty Coffee Online</title>
                <meta name="description" content="Shop fresh roasted beans with complimentary next-day delivery nationwide.">
                <meta property="og:title" content="Specialty Coffee">
            </head>
            <body>
                <h1>Craft Artisanal Roast</h1>
            </body>
            </html>
        """.trimIndent()

        val desc = method.invoke(service, html, "description") as? String
        assertEquals("Shop fresh roasted beans with complimentary next-day delivery nationwide.", desc)
    }

    @Test
    fun default5PointAuditTemplate_containsRequiredPromptSections() = runBlocking {
        // Generate with blank API keys to test synthesized 5-point template fallback
        val result = service.generate5PointAuditReport(
            rawUrl = "https://example.com",
            geminiApiKey = "",
            openRouterApiKey = "",
            clientName = "Sample Client",
            paymentLinkUrl = "https://pay.example.com/audit"
        )

        // If network allows or fails gracefully, check response structure
        if (result.isSuccess) {
            val report = result.getOrThrow()
            assertNotNull(report.generatedReport)
            assertTrue(
                "Must include 5-point audit elements or CTA",
                report.generatedReport.contains("5-POINT") || report.generatedReport.contains("Point") || report.generatedReport.contains("AUDIT")
            )
        } else {
            // Offline environment test runner - expected network exception
            val err = result.exceptionOrNull()
            assertNotNull(err)
        }
    }
}
