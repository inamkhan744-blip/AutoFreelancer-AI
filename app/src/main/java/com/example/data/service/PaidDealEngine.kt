package com.example.data.service

data class ServiceTier(
    val id: String,
    val title: String,
    val priceUsd: Double,
    val tag: String,
    val deliveryTimeframe: String,
    val description: String,
    val keyDeliverables: List<String>
)

object PaidDealEngine {
    val TIERS = listOf(
        ServiceTier(
            id = "TIER_AUDIT_FIX",
            title = "⚡ Express SEO Audit & Speed Fix",
            priceUsd = 297.0,
            tag = "Quick Cash Win",
            deliveryTimeframe = "48-Hour Turnaround",
            description = "Fix technical SEO errors, meta tags, 404 broken links, mobile responsiveness, and Core Web Vitals speed.",
            keyDeliverables = listOf(
                "Full 50+ Point Technical SEO & Crawler Audit",
                "Site Speed & Core Web Vitals Optimization",
                "Broken Links & 404 Redirect Repair",
                "Meta Title, Description & Schema Markup Overhaul",
                "Executive Summary Report with Action Items"
            )
        ),
        ServiceTier(
            id = "TIER_MONTHLY_RETAINER",
            title = "🚀 E-Commerce / Shopify Growth Retainer",
            priceUsd = 797.0,
            tag = "Recurring Monthly Retainer",
            deliveryTimeframe = "Monthly Retainer",
            description = "Target 10 high-intent buyer keywords, optimize product pages, resolve Google Search Console drops, and drive organic sales.",
            keyDeliverables = listOf(
                "10 High-Intent Commercial Keyword Targets",
                "Weekly On-Page & Technical Optimizations",
                "Google Search Console Indexing & Drop Recovery",
                "Product & Collection Page Rich Snippet Schema",
                "Bi-Weekly Traffic & Revenue Growth Reports",
                "Direct Slack / WhatsApp Support Access"
            )
        ),
        ServiceTier(
            id = "TIER_ENTERPRISE_TAKEOVER",
            title = "👑 Complete Organic Revenue Domination",
            priceUsd = 1497.0,
            tag = "High-Ticket Client Closer",
            deliveryTimeframe = "Monthly Retainer",
            description = "Comprehensive competitor displacement, 30 buyer keywords, authoritative backlink acquisition, and full organic revenue scaling.",
            keyDeliverables = listOf(
                "30 High-Value Buyer Keyword Rankings",
                "Competitor Backlink & Content Gap Takeover",
                "10 High-Authority Niche Contextual Backlinks / Month",
                "Full Technical, Speed & CRO Conversion Rate Overhaul",
                "Bi-Weekly Strategy & Executive Performance Calls",
                "100% 30-Day Money-Back Satisfaction Guarantee"
            )
        )
    )

    fun buildClosingProposal(
        clientName: String,
        websiteUrl: String,
        agencyName: String,
        tier: ServiceTier,
        paymentLinkUrl: String,
        customNote: String = ""
    ): String {
        val directCheckout = if (paymentLinkUrl.isNotBlank()) {
            val cleanLink = paymentLinkUrl.trim()
            if (cleanLink.contains("paypal.me", ignoreCase = true) && !cleanLink.endsWith("/${tier.priceUsd.toInt()}USD")) {
                "${cleanLink.trimEnd('/')}/${tier.priceUsd.toInt()}USD"
            } else {
                cleanLink
            }
        } else {
            "https://buy.stripe.com/invoice_instant_checkout"
        }

        val noteBlock = if (customNote.isNotBlank()) "\nSPECIAL NOTE:\n$customNote\n" else ""

        return """
Hi ${clientName.ifBlank { "there" }},

Following up on our analysis of ${websiteUrl.ifBlank { "your website" }}: we've prepared our targeted ${tier.title} package designed to immediately fix your ranking drops and drive real organic buyers to your business.

══════════════════════════════════════════════════
📦 PACKAGE: ${tier.title}
💵 INVESTMENT: $${tier.priceUsd.toInt()} USD (${tier.deliveryTimeframe})
══════════════════════════════════════════════════

KEY DELIVERABLES:
${tier.keyDeliverables.joinToString("\n") { "  ✓ $it" }}
$noteBlock
🛡️ RISK-FREE GUARANTEE:
We stand behind our results with a 30-day money-back guarantee. If you don't see tangible ranking improvements and technical fixes within 30 days, we'll refund your fee.

💳 SECURE INSTANT CHECKOUT:
To initiate your campaign today, complete your activation here:
👉 $directCheckout

Once confirmed, our team begins your optimization within 2 hours.

Best regards,
${agencyName.ifBlank { "Apex Growth Agency" }}
══════════════════════════════════════════════════
        """.trimIndent()
    }
}
