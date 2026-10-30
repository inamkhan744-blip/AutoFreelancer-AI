package com.example.data.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.BodyPart
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.search.FlagTerm

data class ImapMessage(
    val messageId: String,
    val senderName: String,
    val senderEmail: String,
    val subject: String,
    val body: String,
    val receivedAt: Long
)

class ImapEmailService {
    private val tag = "ImapEmailService"

    /**
     * Tests connection and credentials against the specified IMAP server (e.g. imap.gmail.com).
     */
    suspend fun testConnection(
        email: String,
        appPassword: String,
        host: String = "imap.gmail.com",
        port: Int = 993
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPassword = appPassword.trim().replace(" ", "")

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("IMAP Email and App Password cannot be empty."))
        }

        try {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", host)
                put("mail.imaps.port", port.toString())
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.timeout", "10000")
                put("mail.imaps.connectiontimeout", "10000")
            }

            val session = Session.getInstance(props)
            val store = session.getStore("imaps")
            store.connect(host, cleanEmail, cleanPassword)
            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            val totalMessages = inbox.messageCount
            inbox.close(false)
            store.close()

            Result.success("IMAP Connected: Verified inbox access ($totalMessages total messages found).")
        } catch (e: Exception) {
            Log.e(tag, "IMAP connection test error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Connects to IMAP inbox, finds unread messages, filters out spam/alerts, and extracts real client replies.
     */
    suspend fun fetchUnseenReplies(
        email: String,
        appPassword: String,
        host: String = "imap.gmail.com",
        port: Int = 993,
        markAsSeen: Boolean = true
    ): Result<List<ImapMessage>> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPassword = appPassword.trim().replace(" ", "")

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        try {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", host)
                put("mail.imaps.port", port.toString())
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.timeout", "15000")
                put("mail.imaps.connectiontimeout", "15000")
            }

            val session = Session.getInstance(props)
            val store = session.getStore("imaps")
            store.connect(host, cleanEmail, cleanPassword)
            val inbox = store.getFolder("INBOX")
            inbox.open(if (markAsSeen) Folder.READ_WRITE else Folder.READ_ONLY)

            val unseenTerm = FlagTerm(Flags(Flags.Flag.SEEN), false)
            val messages = inbox.search(unseenTerm)

            val extractedList = mutableListOf<ImapMessage>()
            // Process the latest 15 unread messages
            val recentMessages = messages.reversed().take(15)

            for (msg in recentMessages) {
                val fromHeader = msg.from?.firstOrNull() as? InternetAddress
                val senderEmail = fromHeader?.address?.trim().orEmpty()
                val senderName = fromHeader?.personal?.trim().orEmpty().ifBlank {
                    senderEmail.substringBefore("@")
                }
                val subject = msg.subject?.trim().orEmpty().ifBlank { "(No Subject)" }
                val receivedDate = msg.receivedDate?.time ?: msg.sentDate?.time ?: System.currentTimeMillis()
                val body = extractBody(msg)

                // ==========================================
                // SMART FILTER: Bot/Alerts/Self ko ignore karein
                // ==========================================
                val lowerSender = senderEmail.lowercase()
                val lowerSubject = subject.lowercase()
                val lowerMyEmail = cleanEmail.lowercase()

                val isSelfSent = lowerSender == lowerMyEmail
                val isAutomated = lowerSender.contains("alert") || 
                                  lowerSender.contains("noreply") || 
                                  lowerSender.contains("no-reply") ||
                                  lowerSender.contains("info") || 
                                  lowerSender.contains("uptimerobot") ||
                                  lowerSender.contains("notification") ||
                                  lowerSender.contains("newsletter") ||
                                  lowerSender.contains("mailer-daemon") ||
                                  lowerSender.contains("support") ||
                                  lowerSubject.contains("monitor is up") || 
                                  lowerSubject.contains("monitor is down") ||
                                  lowerSubject.contains("uptime")

                if (isSelfSent || isAutomated) {
                    Log.d(tag, "Skipped automated/self email: $lowerSender - $lowerSubject")
                    if (markAsSeen) {
                        msg.setFlag(Flags.Flag.SEEN, true)
                    }
                    continue // Agli email par jayen
                }
                // ==========================================

                val messageId = msg.getHeader("Message-ID")?.firstOrNull()
                    ?: "msg_${System.currentTimeMillis()}_${senderEmail.hashCode()}"

                if (senderEmail.isNotBlank()) {
                    extractedList.add(
                        ImapMessage(
                            messageId = messageId,
                            senderName = senderName,
                            senderEmail = senderEmail,
                            subject = subject,
                            body = body,
                            receivedAt = receivedDate
                        )
                    )
                    if (markAsSeen) {
                        msg.setFlag(Flags.Flag.SEEN, true)
                    }
                }
            }

            inbox.close(false)
            store.close()
            Result.success(extractedList)
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch IMAP replies: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun extractBody(part: Any): String {
        return try {
            when (part) {
                is String -> part
                is Multipart -> {
                    val count = part.count
                    val sb = StringBuilder()
                    for (i in 0 until count) {
                        val bodyPart = part.getBodyPart(i)
                        val content = extractBody(bodyPart)
                        if (content.isNotBlank()) {
                            sb.append(content).append("\n")
                        }
                    }
                    sb.toString().trim()
                }
                is Message -> {
                    if (part.isMimeType("text/plain")) {
                        part.content.toString()
                    } else if (part.isMimeType("text/html")) {
                        cleanHtmlText(part.content.toString())
                    } else if (part.isMimeType("multipart/*")) {
                        extractBody(part.content)
                    } else {
                        part.content?.toString().orEmpty()
                    }
                }
                is BodyPart -> {
                    if (part.isMimeType("text/plain")) {
                        part.content.toString()
                    } else if (part.isMimeType("text/html")) {
                        cleanHtmlText(part.content.toString())
                    } else if (part.isMimeType("multipart/*")) {
                        extractBody(part.content)
                    } else {
                        part.content?.toString().orEmpty()
                    }
                }
                else -> part.toString()
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun cleanHtmlText(html: String): String {
        return html
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
