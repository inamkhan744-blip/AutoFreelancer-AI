package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing a freelancing task stored locally.
 * Statuses: "pending", "in-progress", "completed"
 */
@Entity(tableName = "freelance_tasks")
data class FreelanceTaskEntity(
    @PrimaryKey val id: String = "task_" + UUID.randomUUID().toString().take(8),
    val title: String,
    val description: String,
    val status: String = "pending", // pending, in-progress, completed
    val client: String = "",
    val clientEmail: String = "",
    val estimatedValue: Double = 797.0,
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val category: String = "SEO & Growth",
    val workerRole: String = "Senior SEO Engineer",
    val dueDate: Long? = null, // Epoch timestamp in ms when task is due
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
