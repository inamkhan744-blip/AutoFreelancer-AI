package com.example.data.model

import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

object DealPipelineStage {
    const val NEW = "NEW"
    const val PITCHED = "PITCHED"
    const val FOLLOW_UP = "FOLLOW_UP"
    const val INTERESTED = "INTERESTED"
    const val CALL_BOOKED = "CALL_BOOKED"
    const val PROPOSAL_SENT = "PROPOSAL_SENT"
    const val WON = "WON"
    const val LOST = "LOST"

    val ALL = listOf(NEW, PITCHED, FOLLOW_UP, INTERESTED, CALL_BOOKED, PROPOSAL_SENT, WON, LOST)

    fun displayName(stage: String): String {
        return when (stage.uppercase()) {
            NEW -> "New Lead"
            PITCHED -> "Pitched"
            FOLLOW_UP -> "Follow-Up"
            INTERESTED -> "Interested"
            CALL_BOOKED -> "Call Booked"
            PROPOSAL_SENT -> "Proposal Sent"
            WON -> "Closed Won"
            LOST -> "Closed Lost"
            else -> stage
        }
    }

    fun normalize(raw: String): String {
        val upper = raw.trim().uppercase()
        return when (upper) {
            "NEW" -> NEW
            "PITCHED" -> PITCHED
            "FOLLOWUP", "FOLLOW_UP", "FOLLOW-UP" -> FOLLOW_UP
            "INTERESTED" -> INTERESTED
            "CALLBOOKED", "CALL_BOOKED" -> CALL_BOOKED
            "PROPOSALSENT", "PROPOSAL_SENT" -> PROPOSAL_SENT
            "WON", "PAID", "COMPLETED" -> WON
            "LOST", "REJECTED" -> LOST
            else -> PITCHED
        }
    }
}

data class CrmClient(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val company: String = "",
    val websiteUrl: String = "",
    val email: String = "",
    val phone: String = "",
    val status: String = DealPipelineStage.PITCHED,
    val leadSource: String = "Lead Scout",
    val pitchMessage: String = "",
    val seoReport: String = "",
    val estimatedValue: Double = 0.0,
    val leadScore: Int = 0,
    val pipelineStage: String = DealPipelineStage.PITCHED,
    val sequenceStage: String = "DAY_1",
    val abVariant: String = "VARIANT_A",
    val calendarBookingUrl: String = "",
    val proposalHtml: String = "",
    val proposalPrice: Double = 0.0,
    val paymentStatus: String = "UNPAID",
    val paymentLink: String = "",
    val portalToken: String = UUID.randomUUID().toString().take(12),
    val enrichmentNotes: String = "",
    val lastContactedAt: Long = System.currentTimeMillis(),
    val nextFollowUpAt: Long = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "company" to company,
            "websiteUrl" to websiteUrl,
            "email" to email,
            "phone" to phone,
            "status" to status,
            "leadSource" to leadSource,
            "pitchMessage" to pitchMessage,
            "seoReport" to seoReport,
            "estimatedValue" to estimatedValue,
            "leadScore" to leadScore,
            "pipelineStage" to DealPipelineStage.normalize(pipelineStage),
            "sequenceStage" to sequenceStage,
            "abVariant" to abVariant,
            "calendarBookingUrl" to calendarBookingUrl,
            "proposalHtml" to proposalHtml,
            "proposalPrice" to proposalPrice,
            "paymentStatus" to paymentStatus,
            "paymentLink" to paymentLink,
            "portalToken" to portalToken,
            "enrichmentNotes" to enrichmentNotes,
            "lastContactedAt" to lastContactedAt,
            "nextFollowUpAt" to nextFollowUpAt,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("company", company)
            put("websiteUrl", websiteUrl)
            put("email", email)
            put("phone", phone)
            put("status", status)
            put("leadSource", leadSource)
            put("pitchMessage", pitchMessage)
            put("seoReport", seoReport)
            put("estimatedValue", estimatedValue)
            put("leadScore", leadScore)
            put("pipelineStage", DealPipelineStage.normalize(pipelineStage))
            put("sequenceStage", sequenceStage)
            put("abVariant", abVariant)
            put("calendarBookingUrl", calendarBookingUrl)
            put("proposalHtml", proposalHtml)
            put("proposalPrice", proposalPrice)
            put("paymentStatus", paymentStatus)
            put("paymentLink", paymentLink)
            put("portalToken", portalToken)
            put("enrichmentNotes", enrichmentNotes)
            put("lastContactedAt", lastContactedAt)
            put("nextFollowUpAt", nextFollowUpAt)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): CrmClient {
            val stage = DealPipelineStage.normalize(json.optString("pipelineStage", json.optString("status", DealPipelineStage.PITCHED)))
            return CrmClient(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Prospect Client"),
                company = json.optString("company", ""),
                websiteUrl = json.optString("websiteUrl", ""),
                email = json.optString("email", ""),
                phone = json.optString("phone", ""),
                status = json.optString("status", stage),
                leadSource = json.optString("leadSource", "Lead Scout"),
                pitchMessage = json.optString("pitchMessage", ""),
                seoReport = json.optString("seoReport", ""),
                estimatedValue = json.optDouble("estimatedValue", 0.0),
                leadScore = json.optInt("leadScore", 0),
                pipelineStage = stage,
                sequenceStage = json.optString("sequenceStage", "DAY_1"),
                abVariant = json.optString("abVariant", "VARIANT_A"),
                calendarBookingUrl = json.optString("calendarBookingUrl", ""),
                proposalHtml = json.optString("proposalHtml", ""),
                proposalPrice = json.optDouble("proposalPrice", json.optDouble("estimatedValue", 0.0)),
                paymentStatus = json.optString("paymentStatus", "UNPAID"),
                paymentLink = json.optString("paymentLink", ""),
                portalToken = json.optString("portalToken", UUID.randomUUID().toString().take(12)),
                enrichmentNotes = json.optString("enrichmentNotes", ""),
                lastContactedAt = json.optLong("lastContactedAt", System.currentTimeMillis()),
                nextFollowUpAt = json.optLong("nextFollowUpAt", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        fun fromMap(id: String, map: Map<String, Any>): CrmClient {
            val rawStage = map["pipelineStage"] as? String ?: (map["status"] as? String ?: DealPipelineStage.PITCHED)
            val stage = DealPipelineStage.normalize(rawStage)
            val estVal = (map["estimatedValue"] as? Number)?.toDouble() ?: 0.0

            return CrmClient(
                id = id,
                name = map["name"] as? String ?: "Prospect Client",
                company = map["company"] as? String ?: "",
                websiteUrl = map["websiteUrl"] as? String ?: "",
                email = map["email"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                status = map["status"] as? String ?: stage,
                leadSource = map["leadSource"] as? String ?: "Lead Scout",
                pitchMessage = map["pitchMessage"] as? String ?: "",
                seoReport = map["seoReport"] as? String ?: "",
                estimatedValue = estVal,
                leadScore = (map["leadScore"] as? Number)?.toInt() ?: 0,
                pipelineStage = stage,
                sequenceStage = map["sequenceStage"] as? String ?: "DAY_1",
                abVariant = map["abVariant"] as? String ?: "VARIANT_A",
                calendarBookingUrl = map["calendarBookingUrl"] as? String ?: "",
                proposalHtml = map["proposalHtml"] as? String ?: "",
                proposalPrice = (map["proposalPrice"] as? Number)?.toDouble() ?: estVal,
                paymentStatus = map["paymentStatus"] as? String ?: "UNPAID",
                paymentLink = map["paymentLink"] as? String ?: "",
                portalToken = map["portalToken"] as? String ?: UUID.randomUUID().toString().take(12),
                enrichmentNotes = map["enrichmentNotes"] as? String ?: "",
                lastContactedAt = (map["lastContactedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                nextFollowUpAt = (map["nextFollowUpAt"] as? Number)?.toLong() ?: (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
