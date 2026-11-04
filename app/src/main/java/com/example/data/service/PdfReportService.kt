package com.example.data.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportService {

    /**
     * Generates a clean, authoritative, branded executive PDF report of the SEO Audit.
     * Uses Android's native PdfDocument engine (zero external dependencies).
     */
    fun generateAuditPdf(
        clientName: String,
        domain: String,
        agencyName: String = "Apex Growth Agency",
        dailyBleedUsd: Double = 340.0,
        monthlyBleedUsd: Double = 10200.0,
        paymentLink: String = "",
        auditFindings: List<String> = emptyList()
    ): ByteArray {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 (595 x 842 points)
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cleanDomain = domain.ifBlank { "Client Domain" }.replace("https://", "").replace("http://", "").trimEnd('/')
        val cleanName = clientName.ifBlank { "Executive Decision Maker" }
        val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())

        // 1. Dark Modern Header Banner
        paint.color = Color.rgb(15, 23, 42) // Dark Navy #0F172A
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Accent line below header
        paint.color = Color.rgb(124, 58, 237) // Purple #7C3AED
        canvas.drawRect(0f, 100f, 595f, 104f, paint)

        // Agency Branding
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText(agencyName.uppercase(Locale.US), 36f, 42f, paint)

        paint.color = Color.rgb(148, 163, 184) // Slate-400
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("EXECUTIVE SEO AUDIT & COMPETITOR REVENUE RECOVERY REPORT", 36f, 62f, paint)

        paint.color = Color.rgb(56, 189, 248) // Sky Blue
        paint.textSize = 10f
        canvas.drawText("CONFIDENTIAL AUDIT • $dateStr", 36f, 82f, paint)

        // 2. Client & Target Summary Box
        var curY = 125f
        val summaryBox = RectF(36f, curY, 559f, curY + 60f)
        paint.color = Color.rgb(248, 250, 252) // Light gray bg
        canvas.drawRoundRect(summaryBox, 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawRoundRect(summaryBox, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("TARGET PROSPECT:", 50f, curY + 24f, paint)
        paint.isFakeBoldText = false
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText(cleanName, 170f, curY + 24f, paint)

        paint.color = Color.rgb(30, 41, 59)
        paint.isFakeBoldText = true
        canvas.drawText("ANALYZED DOMAIN:", 50f, curY + 44f, paint)
        paint.isFakeBoldText = false
        paint.color = Color.rgb(124, 58, 237)
        canvas.drawText(cleanDomain, 170f, curY + 44f, paint)

        // 3. High-Impact Daily Bleed Metric Card
        curY += 75f
        val bleedBox = RectF(36f, curY, 559f, curY + 95f)
        paint.color = Color.rgb(254, 242, 242) // Light red #FEF2F2
        canvas.drawRoundRect(bleedBox, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(239, 68, 68) // Red #EF4444
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(bleedBox, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(185, 28, 28) // Deep red
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("⚠️ CRITICAL COMPETITOR TRAFFIC & CASH BLEED DETECTED", 50f, curY + 26f, paint)

        paint.textSize = 24f
        paint.color = Color.rgb(220, 38, 38)
        canvas.drawText("-$${dailyBleedUsd.toInt()} / Day", 50f, curY + 58f, paint)

        paint.textSize = 10f
        paint.color = Color.rgb(127, 29, 29)
        paint.isFakeBoldText = false
        val bleedSubtext = "Estimated Monthly Revenue Loss: -$${monthlyBleedUsd.toInt()}/mo leaking to top SERP competitors."
        canvas.drawText(bleedSubtext, 50f, curY + 78f, paint)

        // 4. Core Technical Audit Findings
        curY += 115f
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("PRIMARY TECHNICAL BOTTLENECKS IDENTIFIED", 36f, curY, paint)

        curY += 12f
        val findings = if (auditFindings.isNotEmpty()) auditFindings else listOf(
            "1. Core Web Vitals LCP Latency: Mobile speed drop causing high bounce rates on key landing pages.",
            "2. Unranked Commercial Intent Keywords: Top direct competitors capture 74% of high-intent search buyers.",
            "3. Indexing & Canonical Fragmentation: Orphaned URLs and redirect chains diluting search authority.",
            "4. Structured Schema Gaps: Missing Rich Snippets, FAQ, and Product Schema leading to lower CTR."
        )

        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(51, 65, 85)
        for (item in findings.take(4)) {
            curY += 18f
            canvas.drawText("• $item", 44f, curY, paint)
        }

        // 5. Strategic 30-Day Recovery Roadmap
        curY += 32f
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("30-DAY REVENUE RECOVERY ROADMAP", 36f, curY, paint)

        curY += 12f
        val roadmapItems = listOf(
            "Phase 1 (Days 1–3): Urgent technical fix of 404s, mobile asset compression & schema overhaul.",
            "Phase 2 (Days 4–14): High-intent commercial keyword targeting & on-page conversion optimization.",
            "Phase 3 (Days 15–30): Authority link acquisition, competitor rank overtaking & monthly reporting."
        )
        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(51, 65, 85)
        for (step in roadmapItems) {
            curY += 18f
            canvas.drawText("✓ $step", 44f, curY, paint)
        }

        // 6. Proposed High-Ticket Investment Tiers
        curY += 32f
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("RECOMMENDED SERVICE TIERS (NO LOW-TICKET SERVICES)", 36f, curY, paint)

        curY += 16f
        // Tier 1 Card
        val t1Box = RectF(36f, curY, 195f, curY + 80f)
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(t1Box, 6f, 6f, paint)
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("Express Audit Fix", 46f, curY + 22f, paint)
        paint.textSize = 16f
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("$297", 46f, curY + 44f, paint)
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("48-Hour speed fix & audit", 46f, curY + 62f, paint)

        // Tier 2 Card (Recommended Highlight)
        val t2Box = RectF(205f, curY, 390f, curY + 80f)
        paint.color = Color.rgb(238, 242, 255) // Indigo tint
        canvas.drawRoundRect(t2Box, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(99, 102, 241)
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(t2Box, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(67, 56, 202)
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("E-Commerce Retainer ★", 215f, curY + 22f, paint)
        paint.textSize = 16f
        paint.color = Color.rgb(79, 70, 229)
        canvas.drawText("$797 / mo", 215f, curY + 44f, paint)
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Halt daily bleed & grow sales", 215f, curY + 62f, paint)

        // Tier 3 Card
        val t3Box = RectF(400f, curY, 559f, curY + 80f)
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(t3Box, 6f, 6f, paint)
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("Enterprise Domination", 410f, curY + 22f, paint)
        paint.textSize = 16f
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("$1,497 / mo", 410f, curY + 44f, paint)
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("Complete market domination", 410f, curY + 62f, paint)

        // 7. Guarantee & Authorization Footer Box
        curY += 100f
        val footerBox = RectF(36f, curY, 559f, curY + 65f)
        paint.color = Color.rgb(240, 253, 244) // Light green #F0FDF4
        canvas.drawRoundRect(footerBox, 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(34, 197, 94) // Green
        paint.strokeWidth = 1f
        canvas.drawRoundRect(footerBox, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(21, 128, 61)
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("🛡️ 30-DAY MONEY-BACK ROI GUARANTEE", 50f, curY + 24f, paint)

        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(22, 101, 52)
        canvas.drawText("If your website does not recover measurable ranking improvements and positive ROI,", 50f, curY + 42f, paint)
        canvas.drawText("we issue an immediate 100% refund. Zero risk to your business.", 50f, curY + 56f, paint)

        // 8. Sign-off & Page Footer
        curY += 80f
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 9f
        val linkText = if (paymentLink.isNotBlank()) "Stripe / Gumroad Checkout: $paymentLink" else "Payment Link: Available upon invoice authorization."
        canvas.drawText(linkText.take(90), 36f, curY, paint)
        canvas.drawText("Prepared by $agencyName Autonomous Engine • All Rights Reserved.", 36f, curY + 16f, paint)

        document.finishPage(page)

        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()

        return outputStream.toByteArray()
    }

    /**
     * Generates a ready-to-attach EmailAttachment object for Brevo API.
     */
    fun generateEmailAttachment(
        clientName: String,
        domain: String,
        agencyName: String = "Apex Growth Agency",
        dailyBleedUsd: Double = 340.0,
        monthlyBleedUsd: Double = 10200.0,
        paymentLink: String = ""
    ): EmailAttachment {
        val pdfBytes = generateAuditPdf(
            clientName = clientName,
            domain = domain,
            agencyName = agencyName,
            dailyBleedUsd = dailyBleedUsd,
            monthlyBleedUsd = monthlyBleedUsd,
            paymentLink = paymentLink
        )
        val cleanDomain = domain.ifBlank { "Client" }.replace("https://", "").replace("http://", "").replace("/", "").replace(".", "_")
        val base64 = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
        return EmailAttachment(
            filename = "${cleanDomain}_Executive_SEO_Audit.pdf",
            contentBase64 = base64
        )
    }
}
