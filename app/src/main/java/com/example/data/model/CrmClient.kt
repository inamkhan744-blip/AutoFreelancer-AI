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

    fun displayName(stage: String?): String {
        if (stage.isNullOrBlank()) return "Pitched"
        return when (stage.trim().uppercase()) {
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

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return PITCHED
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
    val name: String = "Prospect Client",
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
    val isUnresponsive48Hours: Boolean
        get() {
            val normalized = DealPipelineStage.normalize(pipelineStage)
            if (normalized == DealPipelineStage.WON || normalized == DealPipelineStage.LOST) return false
            val elapsed = System.currentTimeMillis() - lastContactedAt
            return elapsed >= TimeUnit.HOURS.toMillis(48)
        }

    val hoursInactive: Long
        get() = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastContactedAt).coerceAtLeast(0)

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
        fun fromJson(json: JSONObject?): CrmClient {
            if (json == null) return CrmClient()
            val rawStage = json.optString("pipelineStage", json.optString("status", DealPipelineStage.PITCHED))
            val stage = DealPipelineStage.normalize(rawStage)

            val estVal = json.optDouble("estimatedValue", 0.0)
            val propPrice = json.optDouble("proposalPrice", estVal)

            return CrmClient(
                id = json.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() },
                name = json.optString("name", "Prospect Client").ifBlank { "Prospect Client" },
                company = json.optString("company", ""),
                websiteUrl = json.optString("websiteUrl", ""),
                email = json.optString("email", ""),
                phone = json.optString("phone", ""),
                status = json.optString("status", stage).ifBlank { stage },
                leadSource = json.optString("leadSource", "Lead Scout").ifBlank { "Lead Scout" },
                pitchMessage = json.optString("pitchMessage", ""),
                seoReport = json.optString("seoReport", ""),
                estimatedValue = if (estVal.isNaN()) 0.0 else estVal,
                leadScore = json.optInt("leadScore", 0),
                pipelineStage = stage,
                sequenceStage = json.optString("sequenceStage", "DAY_1").ifBlank { "DAY_1" },
                abVariant = json.optString("abVariant", "VARIANT_A").ifBlank { "VARIANT_A" },
                calendarBookingUrl = json.optString("calendarBookingUrl", ""),
                proposalHtml = json.optString("proposalHtml", ""),
                proposalPrice = if (propPrice.isNaN()) 0.0 else propPrice,
                paymentStatus = json.optString("paymentStatus", "UNPAID").ifBlank { "UNPAID" },
                paymentLink = json.optString("paymentLink", ""),
                portalToken = json.optString("portalToken", UUID.randomUUID().toString().take(12)).ifBlank { UUID.randomUUID().toString().take(12) },
                enrichmentNotes = json.optString("enrichmentNotes", ""),
                lastContactedAt = json.optLong("lastContactedAt", System.currentTimeMillis()),
                nextFollowUpAt = json.optLong("nextFollowUpAt", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        fun fromMap(id: String?, map: Map<String, Any?>?): CrmClient {
            if (map == null) return CrmClient(id = id ?: UUID.randomUUID().toString())

            val rawStage = map["pipelineStage"]?.toString() ?: (map["status"]?.toString() ?: DealPipelineStage.PITCHED)
            val stage = DealPipelineStage.normalize(rawStage)

            val estVal = when (val v = map["estimatedValue"]) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val propPrice = when (val p = map["proposalPrice"]) {
                is Number -> p.toDouble()
                is String -> p.toDoubleOrNull() ?: estVal
                else -> estVal
            }
            val leadScore = when (val s = map["leadScore"]) {
                is Number -> s.toInt()
                is String -> s.toIntOrNull() ?: 0
                else -> 0
            }

            return CrmClient(
                id = id?.ifBlank { null } ?: UUID.randomUUID().toString(),
                name = map["name"]?.toString()?.ifBlank { null } ?: "Prospect Client",
                company = map["company"]?.toString().orEmpty(),
                websiteUrl = map["websiteUrl"]?.toString().orEmpty(),
                email = map["email"]?.toString().orEmpty(),
                phone = map["phone"]?.toString().orEmpty(),
                status = map["status"]?.toString()?.ifBlank { null } ?: stage,
                leadSource = map["leadSource"]?.toString()?.ifBlank { null } ?: "Lead Scout",
                pitchMessage = map["pitchMessage"]?.toString().orEmpty(),
                seoReport = map["seoReport"]?.toString().orEmpty(),
                estimatedValue = if (estVal.isNaN()) 0.0 else estVal,
                leadScore = leadScore,
                pipelineStage = stage,
                sequenceStage = map["sequenceStage"]?.toString()?.ifBlank { null } ?: "DAY_1",
                abVariant = map["abVariant"]?.toString()?.ifBlank { null } ?: "VARIANT_A",
                calendarBookingUrl = map["calendarBookingUrl"]?.toString().orEmpty(),
                proposalHtml = map["proposalHtml"]?.toString().orEmpty(),
                proposalPrice = if (propPrice.isNaN()) 0.0 else propPrice,
                paymentStatus = map["paymentStatus"]?.toString()?.ifBlank { null } ?: "UNPAID",
                paymentLink = map["paymentLink"]?.toString().orEmpty(),
                portalToken = map["portalToken"]?.toString()?.ifBlank { null } ?: UUID.randomUUID().toString().take(12),
                enrichmentNotes = map["enrichmentNotes"]?.toString().orEmpty(),
                lastContactedAt = (map["lastContactedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                nextFollowUpAt = (map["nextFollowUpAt"] as? Number)?.toLong() ?: (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
