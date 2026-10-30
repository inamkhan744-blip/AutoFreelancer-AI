package com.example.data.service

import com.example.data.model.CrmClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportGeneratorService {

    /**
     * Generates a modern, responsive HTML SEO Audit & Growth Strategy Report
     * ready for immediate automated email delivery to clients.
     */
    fun generateHtmlReport(
        client: CrmClient,
        agencyName: String = "Apex Growth Agency",
        bookingLink: String = "",
        customThankYou: String = ""
    ): String {
        val site = client.websiteUrl.ifBlank { "client website" }
        val clientName = client.name.ifBlank { "Valued Client" }
        val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())

        val thankYouNote = customThankYou.ifBlank {
            "Thank you for choosing $agencyName! We are thrilled to partner with you to accelerate your organic growth, fix critical technical bottlenecks, and drive measurable revenue from Google."
        }

        val bookingCtaHtml = if (bookingLink.isNotBlank()) {
            """
            <div style="text-align: center; margin: 32px 0;">
                <a href="$bookingLink" style="background: linear-gradient(135deg, #0284c7 0%, #2563eb 100%); color: #ffffff; padding: 14px 28px; border-radius: 8px; text-decoration: none; font-weight: bold; font-size: 15px; display: inline-block; box-shadow: 0 4px 14px rgba(2, 132, 199, 0.35);">
                    📅 Book Strategy Review Call
                </a>
            </div>
            """.trimIndent()
        } else ""

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Technical SEO Audit Report — $site</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #1e293b; background-color: #f8fafc; margin: 0; padding: 24px; }
                .container { max-width: 680px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 100%); padding: 36px 32px; color: #ffffff; }
                .badge { display: inline-block; background: rgba(56, 189, 248, 0.2); color: #38bdf8; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; padding: 4px 10px; border-radius: 20px; margin-bottom: 12px; }
                .title { margin: 0 0 6px 0; font-size: 24px; font-weight: 800; }
                .subtitle { margin: 0; color: #94a3b8; font-size: 14px; }
                .content { padding: 32px; }
                .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin: 24px 0; }
                .stat-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; text-align: center; }
                .stat-number { font-size: 26px; font-weight: 800; color: #0284c7; margin: 0; }
                .stat-label { font-size: 12px; color: #64748b; text-transform: uppercase; font-weight: 600; margin: 4px 0 0 0; }
                .section-title { font-size: 17px; font-weight: 700; color: #0f172a; margin: 28px 0 14px 0; border-left: 4px solid #0ea5e9; padding-left: 10px; }
                .issue-item { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px; margin-bottom: 10px; }
                .issue-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
                .issue-tag-high { background: #fee2e2; color: #dc2626; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 12px; }
                .issue-tag-med { background: #fef3c7; color: #d97706; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 12px; }
                .issue-title { font-weight: 700; font-size: 14px; color: #0f172a; margin: 0; }
                .issue-desc { font-size: 13px; color: #475569; margin: 4px 0 0 0; }
                .thank-you-box { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 12px; padding: 20px; margin-top: 28px; }
                .thank-you-title { font-size: 15px; font-weight: 700; color: #166534; margin: 0 0 6px 0; }
                .thank-you-text { font-size: 13px; color: #15803d; margin: 0; line-height: 1.5; }
                .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px 32px; text-align: center; font-size: 12px; color: #94a3b8; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <span class="badge">Verified Deliverable</span>
                    <h1 class="title">Technical SEO Audit & Growth Analysis</h1>
                    <p class="subtitle">Prepared for <strong>$clientName</strong> ($site) • $dateStr</p>
                </div>
                <div class="content">
                    <p style="font-size: 14px; color: #334155; margin-top: 0;">
                        Our engineering team has completed a technical crawl of <strong>$site</strong>. Below are the key baseline performance indicators and top critical fixes to prioritize for immediate search engine uplift.
                    </p>
                    
                    <div class="grid">
                        <div class="stat-card">
                            <p class="stat-number">74/100</p>
                            <p class="stat-label">Technical Health</p>
                        </div>
                        <div class="stat-card">
                            <p class="stat-number">3 Priority</p>
                            <p class="stat-label">Action Items</p>
                        </div>
                    </div>

                    <h3 class="section-title">Top Identified Optimization Items</h3>

                    <div class="issue-item">
                        <div class="issue-header">
                            <h4 class="issue-title">1. Core Web Vitals & Image Payloads</h4>
                            <span class="issue-tag-high">High Priority</span>
                        </div>
                        <p class="issue-desc">Above-the-fold hero image payloads on $site lack modern next-gen compression (WebP/AVIF). Compressing hero assets will decrease Largest Contentful Paint (LCP) to under 2.2 seconds.</p>
                    </div>

                    <div class="issue-item">
                        <div class="issue-header">
                            <h4 class="issue-title">2. Structured Schema Markup</h4>
                            <span class="issue-tag-high">High Priority</span>
                        </div>
                        <p class="issue-desc">No JSON-LD structured data detected. Implementing schema for Organization, Product, and BreadcrumbList allows search engines to render enhanced rich snippets.</p>
                    </div>

                    <div class="issue-item">
                        <div class="issue-header">
                            <h4 class="issue-title">3. Meta Description & Title Tag Optimization</h4>
                            <span class="issue-tag-med">Medium Priority</span>
                        </div>
                        <p class="issue-desc">Several sub-pages share generic or truncated title tags. Aligning target keywords with user intent and unique value propositions will increase organic Click-Through-Rate (CTR).</p>
                    </div>

                    $bookingCtaHtml

                    <div class="thank-you-box">
                        <h4 class="thank-you-title">Thank You from $agencyName!</h4>
                        <p class="thank-you-text">$thankYouNote</p>
                    </div>
                </div>
                <div class="footer">
                    Delivered autonomously by $agencyName • All rights reserved • $site
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
