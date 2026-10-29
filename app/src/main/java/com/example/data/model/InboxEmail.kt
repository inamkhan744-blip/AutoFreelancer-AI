package com.example.data.model

import org.json.JSONObject
import java.util.UUID

data class InboxEmail(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val senderEmail: String,
    val subject: String,
    val incomingMessage: String,
    val objectionCategory: String = "Budget Objection",
    val draftedReply: String = "",
    val status: String = "Awaiting Review", // "Awaiting Review", "Approved & Sent", "Dismissed"
    val receivedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sentAt: Long? = null,
    // AI Reasoning Mode fields
    val reasoningAnalysis: String = "",
    val reasoningStrategy: String = "",
    val nextBestAction: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "senderName" to senderName,
            "senderEmail" to senderEmail,
            "subject" to subject,
            "incomingMessage" to incomingMessage,
            "objectionCategory" to objectionCategory,
            "draftedReply" to draftedReply,
            "status" to status,
            "receivedAt" to receivedAt,
            "updatedAt" to updatedAt,
            "sentAt" to sentAt,
            "reasoningAnalysis" to reasoningAnalysis,
            "reasoningStrategy" to reasoningStrategy,
            "nextBestAction" to nextBestAction
        )
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("senderName", senderName)
            put("senderEmail", senderEmail)
            put("subject", subject)
            put("incomingMessage", incomingMessage)
            put("objectionCategory", objectionCategory)
            put("draftedReply", draftedReply)
            put("status", status)
            put("receivedAt", receivedAt)
            put("updatedAt", updatedAt)
            if (sentAt != null) put("sentAt", sentAt)
            put("reasoningAnalysis", reasoningAnalysis)
            put("reasoningStrategy", reasoningStrategy)
            put("nextBestAction", nextBestAction)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): InboxEmail {
            return InboxEmail(
                id = json.optString("id", UUID.randomUUID().toString()),
                senderName = json.optString("senderName", "Client"),
                senderEmail = json.optString("senderEmail", ""),
                subject = json.optString("subject", "Inquiry"),
                incomingMessage = json.optString("incomingMessage", ""),
                objectionCategory = json.optString("objectionCategory", "General Objection"),
                draftedReply = json.optString("draftedReply", ""),
                status = json.optString("status", "Awaiting Review"),
                receivedAt = json.optLong("receivedAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                sentAt = if (json.has("sentAt")) json.optLong("sentAt") else null,
                reasoningAnalysis = json.optString("reasoningAnalysis", ""),
                reasoningStrategy = json.optString("reasoningStrategy", ""),
                nextBestAction = json.optString("nextBestAction", "")
            )
        }

        fun fromMap(id: String, map: Map<String, Any>): InboxEmail {
            return InboxEmail(
                id = id,
                senderName = map["senderName"] as? String ?: "Client",
                senderEmail = map["senderEmail"] as? String ?: "",
                subject = map["subject"] as? String ?: "Inquiry",
                incomingMessage = map["incomingMessage"] as? String ?: "",
                objectionCategory = map["objectionCategory"] as? String ?: "General Objection",
                draftedReply = map["draftedReply"] as? String ?: "",
                status = map["status"] as? String ?: "Awaiting Review",
                receivedAt = (map["receivedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                sentAt = (map["sentAt"] as? Number)?.toLong(),
                reasoningAnalysis = map["reasoningAnalysis"] as? String ?: "",
                reasoningStrategy = map["reasoningStrategy"] as? String ?: "",
                nextBestAction = map["nextBestAction"] as? String ?: ""
            )
        }
    }
}
