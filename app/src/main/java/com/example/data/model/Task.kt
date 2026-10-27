package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

@IgnoreExtraProperties
data class Task(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "",
    val workerName: String = "",
    val workerRole: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, RUNNING, COMPLETED, REJECTED
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val client: String = "",
    val clientEmail: String = "",
    val estimatedValue: Double = 0.0,
    val details: String = "",
    val rejectionReason: String? = null,
    val logs: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "category" to category,
            "workerName" to workerName,
            "workerRole" to workerRole,
            "status" to status,
            "priority" to priority,
            "client" to client,
            "clientEmail" to clientEmail,
            "estimatedValue" to estimatedValue,
            "details" to details,
            "rejectionReason" to rejectionReason,
            "logs" to logs,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        ).filterValues { it != null }
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("title", title)
        json.put("category", category)
        json.put("workerName", workerName)
        json.put("workerRole", workerRole)
        json.put("status", status)
        json.put("priority", priority)
        json.put("client", client)
        json.put("clientEmail", clientEmail)
        json.put("estimatedValue", estimatedValue)
        json.put("details", details)
        if (rejectionReason != null) json.put("rejectionReason", rejectionReason)
        val logsArr = JSONArray()
        logs.forEach { logsArr.put(it) }
        json.put("logs", logsArr)
        val createdSec = createdAt?.seconds ?: (System.currentTimeMillis() / 1000)
        val updatedSec = updatedAt?.seconds ?: (System.currentTimeMillis() / 1000)
        json.put("createdAtSeconds", createdSec)
        json.put("updatedAtSeconds", updatedSec)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): Task {
            val logs = mutableListOf<String>()
            val logsArr = json.optJSONArray("logs")
            if (logsArr != null) {
                for (i in 0 until logsArr.length()) {
                    logs.add(logsArr.getString(i))
                }
            }
            val createdSec = json.optLong("createdAtSeconds", System.currentTimeMillis() / 1000)
            val updatedSec = json.optLong("updatedAtSeconds", System.currentTimeMillis() / 1000)

            return Task(
                id = json.optString("id", ""),
                title = json.optString("title", ""),
                category = json.optString("category", ""),
                workerName = json.optString("workerName", ""),
                workerRole = json.optString("workerRole", ""),
                status = json.optString("status", "PENDING"),
                priority = json.optString("priority", "MEDIUM"),
                client = json.optString("client", ""),
                clientEmail = json.optString("clientEmail", ""),
                estimatedValue = json.optDouble("estimatedValue", 0.0),
                details = json.optString("details", ""),
                rejectionReason = if (json.has("rejectionReason")) json.optString("rejectionReason") else null,
                logs = logs,
                createdAt = Timestamp(Date(createdSec * 1000)),
                updatedAt = Timestamp(Date(updatedSec * 1000))
            )
        }
    }
}
