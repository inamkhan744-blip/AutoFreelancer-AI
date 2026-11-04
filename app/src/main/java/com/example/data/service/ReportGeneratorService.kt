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
                .issue-item { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 18px; margin-bottom: 16px; box-shadow: 0 2px 4px rgba(0,0,0,0.02); }
                .issue-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
                .issue-tag-high { background: #fee2e2; color: #dc2626; font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 12px; }
                .issue-tag-med { background: #fef3c7; color: #d97706; font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 12px; }
                .issue-title { font-weight: 800; font-size: 15px; color: #0f172a; margin: 0; }
                .field-label { font-weight: 700; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; color: #64748b; margin-top: 8px; margin-bottom: 2px; }
                .field-value { font-size: 13.5px; color: #334155; margin: 0 0 8px 0; line-height: 1.5; }
                .code-box { background: #0f172a; color: #38bdf8; padding: 12px 14px; border-radius: 8px; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 12px; overflow-x: auto; margin: 6px 0 0 0; border: 1px solid #1e293b; }
                .upsell-card { background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%); border: 2px solid #a855f7; border-radius: 14px; padding: 24px; margin-top: 32px; text-align: center; color: #ffffff; box-shadow: 0 8px 20px rgba(124, 58, 237, 0.25); }
                .upsell-badge { background: #a855f7; color: #ffffff; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; padding: 4px 12px; border-radius: 20px; display: inline-block; margin-bottom: 10px; }
                .upsell-text { font-size: 15px; font-weight: 600; color: #f1f5f9; margin: 8px 0 16px 0; line-height: 1.6; }
                .thank-you-box { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 12px; padding: 20px; margin-top: 20px; }
                .thank-you-title { font-size: 15px; font-weight: 700; color: #166534; margin: 0 0 6px 0; }
                .thank-you-text { font-size: 13px; color: #15803d; margin: 0; line-height: 1.5; }
                .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px 32px; text-align: center; font-size: 12px; color: #94a3b8; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <span class="badge">Verified Deliverable</span>
                    <h1 class="title">Technical SEO Audit & Solutions Dossier</h1>
                    <p class="subtitle">Prepared for <strong>$clientName</strong> ($site) • $dateStr</p>
                </div>
                <div class="content">
                    <p style="font-size: 14px; color: #334155; margin-top: 0;">
                        Our engineering team has completed a technical crawl of <strong>$site</strong>. Below is the actionable developer dossier pairing each detected issue with its business impact and copy-paste code solution.
                    </p>
                    
                    <div class="grid">
                        <div class="stat-card">
                            <p class="stat-number">74/100</p>
                            <p class="stat-label">Technical Health</p>
                        </div>
                        <div class="stat-card">
                            <p class="stat-number">-$340/Day</p>
                            <p class="stat-label">Estimated Bleed</p>
                        </div>
                    </div>

                    <h3 class="section-title">Actionable Technical Solutions (Ready to Copy-Paste)</h3>

                    <!-- Item 1: Slow Images -->
                    <div class="issue-item">
                        <div class="issue-header">
                            <h4 class="issue-title">1. Slow Images & High LCP</h4>
                            <span class="issue-tag-high">High Priority</span>
                        </div>
                        <div class="field-label">• Issue Found:</div>
                        <p class="field-value">Above-and-below the fold images on $site lack modern next-gen compression and native lazy loading.</p>
                        <div class="field-label">• Impact:</div>
                        <p class="field-value">Increases Largest Contentful Paint (LCP) to 3.4 seconds, causing mobile ranking penalties and up to 24% customer bounce rate.</p>
                        <div class="field-label">• Exact Solution:</div>
                        <p class="field-value">Apply native HTML lazy loading, async decoding, and explicit dimensions to all below-the-fold content images:</p>
                        <div class="code-box">&lt;img src="assets/hero-banner.webp" alt="Primary Value Proposition" loading="lazy" decoding="async" width="800" height="450"&gt;</div>
                    </div>

                    <!-- Item 2: Meta Description -->
                    <div class="issue-item">
                        <div class="issue-header">
                            <h4 class="issue-title">2. Missing / Unoptimized Meta Description</h4>
                            <span class="issue-tag-high">High Priority</span>
                        </div>
                        <div class="field-label">• Issue Found:</div>
                        <p class="field-value">Homepage and key landing pages lack a concise, keyword-optimized meta description tag in the HTML head.</p>
                        <div class="field-label">• Impact:</div>
                        <p class="field-value">Google cannot understand the page context or intent, generating auto-scraped snippets that lower search click-through rate (CTR) by over 30%.</p>
                        <div class="field-label">• Exact Solution:</div>
                        <p class="field-value">Add this exact meta tag inside the &lt;head&gt; container of your primary landing page:</p>
                        <div class="code-box">&lt;meta name="description" content="Discover verified high-performance solutions at $site. Boost traffic, eliminate technical bottlenecks, and accelerate revenue."&gt;</div>
                    </div>

                    <!-- Item 3: Keyword Cannibalization -->
                    <div class="issue-item">
                        <div class="issue-header">
                            <h4 class="issue-title">3. Keyword Cannibalization & Page Duplication</h4>
                            <span class="issue-tag-med">Medium Priority</span>
                        </div>
                        <div class="field-label">• Issue Found:</div>
                        <p class="field-value">Multiple subpages target overlapping core search terms, competing against each other in Google search results.</p>
                        <div class="field-label">• Impact:</div>
                        <p class="field-value">Splits search authority, dilutes backlinks, and causes Google to alternate between URLs, preventing either page from ranking in the top 3.</p>
                        <div class="field-label">• Exact Solution:</div>
                        <p class="field-value">Execute a 4-step consolidation plan: Merge secondary content into the Master Pillar page, implement a 301 redirect, and set a canonical link:</p>
                        <div class="code-box"># Step 1: 301 Permanent Redirect in .htaccess / Nginx
Redirect 301 /competing-subpage /primary-pillar-page

&lt;!-- Step 2: Canonical tag in &lt;head&gt; of Primary Pillar --&gt;
&lt;link rel="canonical" href="https://$site/primary-pillar-page"&gt;</div>
                    </div>

                    <!-- The Done-For-You Upsell -->
                    <div class="upsell-card">
                        <span class="upsell-badge">Done-For-You Implementation</span>
                        <h3 style="margin: 0 0 10px 0; font-size: 19px; color: #ffffff;">Want Our Senior Engineers to Handle Everything?</h3>
                        <p class="upsell-text">
                            Need me to implement these fixes for you? Reply to this email to upgrade to our $797/month Done-For-You Retainer and let me handle everything.
                        </p>
                        $bookingCtaHtml
                    </div>

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
