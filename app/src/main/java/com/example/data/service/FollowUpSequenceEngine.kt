package com.example.data.service

import com.example.data.model.CrmClient
import java.util.concurrent.TimeUnit

data class GeneratedTemplate(
    val subject: String,
    val body: String
)

data class SequenceTemplate(
    val stageKey: String,
    val daysAfterInitial: Int,
    val subjectVariantA: String,
    val subjectVariantB: String,
    val bodyGenerator: (client: CrmClient, agencyName: String, bookingLink: String, customFooter: String) -> String
)

class FollowUpSequenceEngine {

    enum class FollowUpStage(val delayDays: Int, val purpose: String) {
        DAY_1(1, "Initial Outreach Pitch"),
        DAY_3(3, "Quick Bump / Follow-up"),
        DAY_7(7, "Value Add (Free Tip)"),
        DAY_14(14, "Case Study & Social Proof"),
        DAY_21(21, "Breakup / Soft Close");

        companion object {
            fun fromString(value: String): FollowUpStage {
                return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: DAY_1
            }
        }
    }

    companion object {
        const val STAGE_DAY_1 = "DAY_1"
        const val STAGE_DAY_3 = "DAY_3"
        const val STAGE_DAY_7 = "DAY_7"
        const val STAGE_DAY_14 = "DAY_14"
        const val STAGE_DAY_21 = "DAY_21"
        const val STAGE_COMPLETED = "COMPLETED"

        val SEQUENCES = listOf(
            SequenceTemplate(
                stageKey = STAGE_DAY_3,
                daysAfterInitial = 3,
                subjectVariantA = "Quick follow-up on my note",
                subjectVariantB = "Did you get a chance to review the audit?",
                bodyGenerator = { client, agency, bookingLink, footer ->
                    buildString {
                        append("Hi ${client.name.ifBlank { "there" }},\n\n")
                        append("I know your inbox is probably overflowing, so I wanted to quickly bump this to the top.\n\n")
                        append("Did you have a chance to take a look at the quick SEO suggestions I shared for ${client.websiteUrl.ifBlank { "your website" }} a few days ago?\n\n")
                        append("If it’s easier to chat live, feel free to pick a quick 10-minute window on my calendar here:\n")
                        if (bookingLink.isNotBlank()) {
                            append("$bookingLink\n\n")
                        } else {
                            append("Just reply 'yes' and I will send over a 4-minute Loom video walking through the findings.\n\n")
                        }
                        append("Best regards,\n$agency\n")
                        if (footer.isNotBlank()) append("\n$footer")
                    }
                }
            ),
            SequenceTemplate(
                stageKey = STAGE_DAY_7,
                daysAfterInitial = 7,
                subjectVariantA = "Free SEO value-add for your team",
                subjectVariantB = "One immediate technical fix for your site",
                bodyGenerator = { client, agency, bookingLink, footer ->
                    buildString {
                        append("Hi ${client.name.ifBlank { "there" }},\n\n")
                        append("No pitch today—just wanted to share a high-leverage technical tip our team spotted on ${client.websiteUrl.ifBlank { "your domain" }}.\n\n")
                        append("We noticed your core landing pages could cut bounce rates by ~23% by preloading above-the-fold hero assets and fixing minor mobile viewport shifts.\n\n")
                        append("We prepared a full roadmap to fix this and rank higher on Google for commercial intent keywords.\n\n")
                        if (bookingLink.isNotBlank()) {
                            append("Grab a time here if you want us to handle the heavy lifting: $bookingLink\n\n")
                        }
                        append("Warmly,\n$agency\n")
                        if (footer.isNotBlank()) append("\n$footer")
                    }
                }
            ),
            SequenceTemplate(
                stageKey = STAGE_DAY_14,
                daysAfterInitial = 14,
                subjectVariantA = "Case study: 184% search traffic increase",
                subjectVariantB = "How a similar brand doubled their organic leads",
                bodyGenerator = { client, agency, bookingLink, footer ->
                    buildString {
                        append("Hi ${client.name.ifBlank { "there" }},\n\n")
                        append("I thought you might find this relevant. Last quarter, we helped an online brand in your space scale organic search revenue by +184% in 90 days by fixing programmatic crawl bloat and cannibalized keywords.\n\n")
                        append("I see the exact same opportunity on ${client.websiteUrl.ifBlank { "your site" }}.\n\n")
                        append("Would you be open to a 10-minute review call this week to see if we can replicate these numbers for your team?\n\n")
                        if (bookingLink.isNotBlank()) {
                            append("Direct booking link: $bookingLink\n\n")
                        }
                        append("Cheers,\n$agency Growth Team\n")
                        if (footer.isNotBlank()) append("\n$footer")
                    }
                }
            ),
            SequenceTemplate(
                stageKey = STAGE_DAY_21,
                daysAfterInitial = 21,
                subjectVariantA = "Closing out your audit file",
                subjectVariantB = "Permission to archive your growth proposal?",
                bodyGenerator = { client, agency, bookingLink, footer ->
                    buildString {
                        append("Hi ${client.name.ifBlank { "there" }},\n\n")
                        append("I assume your team is currently focused on other priorities, which is completely understandable.\n\n")
                        append("I will close out your SEO audit file for now so I don't clutter your inbox. If organic search and customer acquisition become a focus down the road, you can always reach out or book directly here:\n")
                        if (bookingLink.isNotBlank()) {
                            append("$bookingLink\n\n")
                        } else {
                            append("Simply reply to this email anytime.\n\n")
                        }
                        append("Wishing you and your business all the best!\n\n$agency\n")
                        if (footer.isNotBlank()) append("\n$footer")
                    }
                }
            )
        )
    }

    fun determineNextFollowUp(currentStage: FollowUpStage, lastContactedAt: Long, now: Long): FollowUpStage? {
        val elapsedMs = now - lastContactedAt
        return when (currentStage) {
            FollowUpStage.DAY_1 -> if (elapsedMs >= TimeUnit.DAYS.toMillis(2)) FollowUpStage.DAY_3 else null
            FollowUpStage.DAY_3 -> if (elapsedMs >= TimeUnit.DAYS.toMillis(4)) FollowUpStage.DAY_7 else null
            FollowUpStage.DAY_7 -> if (elapsedMs >= TimeUnit.DAYS.toMillis(7)) FollowUpStage.DAY_14 else null
            FollowUpStage.DAY_14 -> if (elapsedMs >= TimeUnit.DAYS.toMillis(7)) FollowUpStage.DAY_21 else null
            FollowUpStage.DAY_21 -> null
        }
    }

    fun generateFollowUpTemplate(
        stage: FollowUpStage,
        clientName: String,
        websiteUrl: String,
        bookingLink: String,
        agencyName: String,
        customFooter: String = ""
    ): GeneratedTemplate {
        val greeting = if (clientName.isNotBlank()) "Hi $clientName," else "Hi there,"
        val agency = agencyName.ifBlank { "Apex Growth Agency" }
        val siteRef = websiteUrl.ifBlank { "your website" }

        val footerText = if (customFooter.isNotBlank()) "\n\n$customFooter" else ""

        return when (stage) {
            FollowUpStage.DAY_1 -> {
                val subject = "Quick question regarding $siteRef"
                val body = """
                    $greeting

                    I noticed a few immediate organic search optimization areas on $siteRef that could increase qualified inbound traffic.

                    I prepared a brief technical audit outline for your team. Would you be open to reviewing it?

                    Best regards,
                    $agency$footerText
                """.trimIndent()
                GeneratedTemplate(subject, body)
            }
            FollowUpStage.DAY_3 -> {
                val subject = "Quick follow-up on $siteRef audit"
                val body = """
                    $greeting

                    I know your schedule is busy, so I wanted to quickly bump this to the top of your inbox.

                    Did you have a chance to review the quick search optimization notes I sent over for $siteRef?

                    If you'd like to chat live, feel free to grab 10 minutes on my calendar here:
                    ${if (bookingLink.isNotBlank()) bookingLink else "Just reply to this note and I'll send over a quick video walkthrough."}

                    Best regards,
                    $agency$footerText
                """.trimIndent()
                GeneratedTemplate(subject, body)
            }
            FollowUpStage.DAY_7 -> {
                val subject = "Free technical growth tip for $siteRef"
                val body = """
                    $greeting

                    No sales pitch today—just wanted to share a actionable technical tip we spotted on $siteRef.

                    We noticed that preloading primary hero assets and optimizing Core Web Vitals on mobile could decrease user bounce rates by up to ~25%.

                    We've mapped out the full roadmap to fix this. If you want us to handle implementation, grab a slot here:
                    ${if (bookingLink.isNotBlank()) bookingLink else "Reply 'interested' and we'll share the step-by-step fix."}

                    Warmly,
                    $agency$footerText
                """.trimIndent()
                GeneratedTemplate(subject, body)
            }
            FollowUpStage.DAY_14 -> {
                val subject = "Case study: How similar brands scaled organic pipeline"
                val body = """
                    $greeting

                    Thought you might find this relevant. Last quarter, we helped an online brand in your niche scale organic search traffic by +184% in 90 days by fixing index bloat and keyword intent mismatches.

                    I see a very similar revenue opportunity on $siteRef.

                    Would you be open to a 10-minute call this week to see if we can replicate these numbers for your team?
                    ${if (bookingLink.isNotBlank()) "Direct booking link: $bookingLink" else "Reply here anytime."}

                    Cheers,
                    $agency Growth Team$footerText
                """.trimIndent()
                GeneratedTemplate(subject, body)
            }
            FollowUpStage.DAY_21 -> {
                val subject = "Closing out your $siteRef audit file"
                val body = """
                    $greeting

                    I assume your team is currently focused on other priorities, which is completely understandable.

                    I will close out your growth audit file for now so I don't clutter your inbox. If organic search and customer acquisition become a focus down the road, you can always reach out or book directly here:
                    ${if (bookingLink.isNotBlank()) bookingLink else "Just reply to this email anytime."}

                    Wishing you and your business all the best!

                    $agency$footerText
                """.trimIndent()
                GeneratedTemplate(subject, body)
            }
        }
    }

    fun getNextSequence(currentStage: String): SequenceTemplate? {
        return when (currentStage.uppercase()) {
            STAGE_DAY_1 -> SEQUENCES.find { it.stageKey == STAGE_DAY_3 }
            STAGE_DAY_3 -> SEQUENCES.find { it.stageKey == STAGE_DAY_7 }
            STAGE_DAY_7 -> SEQUENCES.find { it.stageKey == STAGE_DAY_14 }
            STAGE_DAY_14 -> SEQUENCES.find { it.stageKey == STAGE_DAY_21 }
            else -> null
        }
    }

    fun isEligibleForNextStage(client: CrmClient): Boolean {
        // Exclude closed or paying clients from cold follow-ups
        if (client.pipelineStage in listOf("WON", "PAID", "CALL_BOOKED", "LOST")) {
            return false
        }
        if (client.sequenceStage == STAGE_COMPLETED) {
            return false
        }

        val elapsedMs = System.currentTimeMillis() - client.lastContactedAt
        return when (client.sequenceStage.uppercase()) {
            STAGE_DAY_1 -> elapsedMs >= TimeUnit.DAYS.toMillis(3)
            STAGE_DAY_3 -> elapsedMs >= TimeUnit.DAYS.toMillis(4) // Day 7 total
            STAGE_DAY_7 -> elapsedMs >= TimeUnit.DAYS.toMillis(7) // Day 14 total
            STAGE_DAY_14 -> elapsedMs >= TimeUnit.DAYS.toMillis(7) // Day 21 total
            else -> false
        }
    }
}
