package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

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
            "estimatedValue" to estimatedValue,
            "details" to details,
            "rejectionReason" to rejectionReason,
            "logs" to logs,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        ).filterValues { it != null }
    }
}
