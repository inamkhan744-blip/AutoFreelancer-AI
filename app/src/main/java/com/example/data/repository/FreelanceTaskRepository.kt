package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.dao.FreelanceTaskDao
import com.example.data.local.entity.FreelanceTaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository to manage freelancing tasks persisted locally in Room Database.
 * Supports task statuses: "pending", "in-progress", "completed" and descriptions.
 */
class FreelanceTaskRepository(
    private val taskDao: FreelanceTaskDao
) {
    constructor(context: Context) : this(
        AppDatabase.getDatabase(context).freelanceTaskDao()
    )

    /**
     * Observes all freelancing tasks ordered by creation date.
     */
    fun observeAllTasks(): Flow<List<FreelanceTaskEntity>> {
        return taskDao.getAllTasks()
    }

    /**
     * Observes freelancing tasks filtered by status ("pending", "in-progress", "completed").
     */
    fun observeTasksByStatus(status: String): Flow<List<FreelanceTaskEntity>> {
        return taskDao.getTasksByStatus(status.lowercase().trim())
    }

    /**
     * Retrieves a single task by ID.
     */
    suspend fun getTaskById(id: String): FreelanceTaskEntity? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(id)
    }

    /**
     * Inserts or creates a new freelancing task in Room Database.
     */
    suspend fun createTask(
        title: String,
        description: String,
        status: String = "pending",
        client: String = "",
        clientEmail: String = "",
        estimatedValue: Double = 797.0,
        priority: String = "MEDIUM"
    ): String = withContext(Dispatchers.IO) {
        val normalizedStatus = when (status.lowercase().trim()) {
            "in-progress", "inprogress", "running", "in_progress" -> "in-progress"
            "completed", "done", "approved" -> "completed"
            else -> "pending"
        }
        val id = "task_" + UUID.randomUUID().toString().take(8)
        val task = FreelanceTaskEntity(
            id = id,
            title = title,
            description = description,
            status = normalizedStatus,
            client = client,
            clientEmail = clientEmail,
            estimatedValue = estimatedValue,
            priority = priority,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        taskDao.insertTask(task)
        id
    }

    /**
     * Inserts an existing task entity.
     */
    suspend fun insertTask(task: FreelanceTaskEntity): String = withContext(Dispatchers.IO) {
        val id = if (task.id.isNotBlank()) task.id else "task_" + UUID.randomUUID().toString().take(8)
        taskDao.insertTask(task.copy(id = id))
        id
    }

    /**
     * Updates an existing task entity.
     */
    suspend fun updateTask(task: FreelanceTaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Updates task status ("pending", "in-progress", "completed").
     */
    suspend fun updateTaskStatus(id: String, newStatus: String) = withContext(Dispatchers.IO) {
        val normalizedStatus = when (newStatus.lowercase().trim()) {
            "in-progress", "inprogress", "running", "in_progress" -> "in-progress"
            "completed", "done", "approved" -> "completed"
            else -> "pending"
        }
        taskDao.updateTaskStatus(id, normalizedStatus)
    }

    /**
     * Deletes a task by ID.
     */
    suspend fun deleteTask(id: String) = withContext(Dispatchers.IO) {
        taskDao.deleteTaskById(id)
    }
}
