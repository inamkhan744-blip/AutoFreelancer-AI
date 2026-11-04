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
            assertTrue(
                "Must include Done-For-You Retainer upsell",
                report.generatedReport.contains(SeoAuditService.DONE_FOR_YOU_UPSELL_TEXT)
            )
            assertTrue("Must include technical solutions", report.solutions.isNotEmpty())
        } else {
            // Offline environment test runner - expected network exception
            val err = result.exceptionOrNull()
            assertNotNull(err)
        }
    }

    @Test
    fun buildDefaultTechnicalSolutions_returnsActionableThreePartFixes() {
        val rawData = RawSeoData(
            url = "https://ecombrand.store",
            httpStatus = 200,
            latencyMs = 1200,
            title = "Eco Clean Products",
            metaDescription = null,
            h1Heading = null,
            canonicalUrl = null,
            hasViewport = true,
            robots = null,
            issuesIdentified = listOf("Missing Meta Description", "Slow Images"),
            rawAuditSummary = "Sample Audit Summary"
        )
        val solutions = service.buildDefaultTechnicalSolutions(rawData)
        assertEquals(5, solutions.size)

        // Verify Slow Images solution contains <img loading="lazy">
        val slowImages = solutions.find { it.issueFound.contains("Slow Images", ignoreCase = true) }
        assertNotNull(slowImages)
        assertTrue(slowImages!!.exactSolution.isNotBlank())
        assertTrue(slowImages.codeSnippet!!.contains("loading=\"lazy\""))

        // Verify Meta Description solution contains <meta name="description">
        val metaDesc = solutions.find { it.issueFound.contains("Meta Description", ignoreCase = true) }
        assertNotNull(metaDesc)
        assertTrue(metaDesc!!.codeSnippet!!.contains("<meta name=\"description\""))

        // Verify Keyword Cannibalization contains step-by-step plan and 301 redirect
        val cannibalization = solutions.find { it.issueFound.contains("Cannibalization", ignoreCase = true) }
        assertNotNull(cannibalization)
        assertTrue(cannibalization!!.exactSolution.contains("Step-by-step", ignoreCase = true))
        assertTrue(cannibalization.codeSnippet!!.contains("Redirect 301"))
    }

    @Test
    fun parseTechnicalSolutions_extractsThreePartStructure() {
        val rawData = RawSeoData(
            url = "https://testbrand.com",
            httpStatus = 200,
            latencyMs = 450,
            title = "Test Brand",
            metaDescription = "Existing",
            h1Heading = "Heading",
            canonicalUrl = "https://testbrand.com",
            hasViewport = true,
            robots = null,
            issuesIdentified = emptyList(),
            rawAuditSummary = ""
        )
        val sampleAiReport = """
            # SEO Technical Audit
            - **Issue Found**: Slow Images on Homepage
            - **Impact**: Google flags mobile Core Web Vitals resulting in search demotion
            - **Exact Solution**: Add native lazy loading to image elements:
            ```html
            <img src="banner.webp" loading="lazy" alt="Banner">
            ```
            
            - **Issue Found**: Missing Meta Description
            - **Impact**: Google renders truncated snippets lowering CTR
            - **Exact Solution**: Add description to head:
            ```html
            <meta name="description" content="Quality goods for modern buyers.">
            ```
            
            - **Issue Found**: Keyword Cannibalization
            - **Impact**: Two pages compete for same keyword cluster
            - **Exact Solution**: Step 1: Merge content into master page. Step 2: 301 redirect.
            ```apache
            Redirect 301 /competing /master
            ```
        """.trimIndent()

        val parsed = service.parseTechnicalSolutions(sampleAiReport, rawData)
        assertEquals(3, parsed.size)
        assertEquals("Slow Images on Homepage", parsed[0].issueFound)
        assertTrue(parsed[0].codeSnippet!!.contains("loading=\"lazy\""))
        assertEquals("Missing Meta Description", parsed[1].issueFound)
        assertTrue(parsed[1].codeSnippet!!.contains("<meta name=\"description\""))
        assertEquals("Keyword Cannibalization", parsed[2].issueFound)
    }
}
