package com.example.data.model

import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class CrmClient(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val company: String = "",
    val websiteUrl: String = "",
    val email: String = "",
    val status: String = "Pitched", // Allowed: "Pitched", "Interested", "FollowUp", "Paid", "Completed"
    val leadSource: String = "Lead Scout",
    val pitchMessage: String = "",
    val seoReport: String = "",
    val estimatedValue: Double = 0.0,
    val leadScore: Int = 0,
    val pipelineStage: String = "Pitched",
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
            "status" to status,
            "leadSource" to leadSource,
            "pitchMessage" to pitchMessage,
            "seoReport" to seoReport,
            "estimatedValue" to estimatedValue,
            "leadScore" to leadScore,
            "pipelineStage" to pipelineStage,
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
            put("status", status)
            put("leadSource", leadSource)
            put("pitchMessage", pitchMessage)
            put("seoReport", seoReport)
            put("estimatedValue", estimatedValue)
            put("leadScore", leadScore)
            put("pipelineStage", pipelineStage)
            put("lastContactedAt", lastContactedAt)
            put("nextFollowUpAt", nextFollowUpAt)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): CrmClient {
            return CrmClient(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Prospect Client"),
                company = json.optString("company", ""),
                websiteUrl = json.optString("websiteUrl", ""),
                email = json.optString("email", ""),
                status = json.optString("status", "Pitched"),
                leadSource = json.optString("leadSource", "Lead Scout"),
                pitchMessage = json.optString("pitchMessage", ""),
                seoReport = json.optString("seoReport", ""),
                estimatedValue = json.optDouble("estimatedValue", 0.0),
                leadScore = json.optInt("leadScore", 0),
                pipelineStage = json.optString("pipelineStage", "Pitched"),
                lastContactedAt = json.optLong("lastContactedAt", System.currentTimeMillis()),
                nextFollowUpAt = json.optLong("nextFollowUpAt", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        fun fromMap(id: String, map: Map<String, Any>): CrmClient {
            return CrmClient(
                id = id,
                name = map["name"] as? String ?: "Prospect Client",
                company = map["company"] as? String ?: "",
                websiteUrl = map["websiteUrl"] as? String ?: "",
                email = map["email"] as? String ?: "",
                status = map["status"] as? String ?: "Pitched",
                leadSource = map["leadSource"] as? String ?: "Lead Scout",
                pitchMessage = map["pitchMessage"] as? String ?: "",
                seoReport = map["seoReport"] as? String ?: "",
                estimatedValue = (map["estimatedValue"] as? Number)?.toDouble() ?: 0.0,
                leadScore = (map["leadScore"] as? Number)?.toInt() ?: 0,
                pipelineStage = map["pipelineStage"] as? String ?: (map["status"] as? String ?: "Pitched"),
                lastContactedAt = (map["lastContactedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                nextFollowUpAt = (map["nextFollowUpAt"] as? Number)?.toLong() ?: (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
