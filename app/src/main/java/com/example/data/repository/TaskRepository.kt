package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Task
import com.example.util.LiveWorkerLogStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File
import java.util.UUID

class TaskRepository(
    private val context: Context
) {
    private val tag = "TaskRepository"
    private val localTasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private val storageFile: File = File(context.filesDir, "autofreelancer_tasks.json")

    init {
        loadLocalTasks()
    }

    private fun loadLocalTasks() {
        if (!storageFile.exists()) {
            localTasksFlow.value = emptyList()
            return
        }
        try {
            val content = storageFile.readText()
            if (content.isNotBlank()) {
                val jsonArr = JSONArray(content)
                val list = mutableListOf<Task>()
                for (i in 0 until jsonArr.length()) {
                    list.add(Task.fromJson(jsonArr.getJSONObject(i)))
                }
                // Purge any legacy dummy seed tasks if present
                val cleanList = list.filterNot {
                    it.id.startsWith("task_init_") ||
                    it.client.contains("Fintech & SaaS") ||
                    it.client.contains("TechVentures") ||
                    it.client.contains("Apex Apparel") ||
                    it.client.contains("CloudOps") ||
                    it.client.contains("HealthPulse")
                }
                localTasksFlow.value = cleanList
                if (cleanList.size != list.size) {
                    saveLocalTasks(cleanList)
                }
            } else {
                localTasksFlow.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load local tasks: ${e.message}", e)
            localTasksFlow.value = emptyList()
        }
    }

    private fun saveLocalTasks(tasks: List<Task>) {
        localTasksFlow.value = tasks
        try {
            val jsonArr = JSONArray()
            tasks.forEach { jsonArr.put(it.toJson()) }
            storageFile.writeText(jsonArr.toString())
        } catch (e: Exception) {
            Log.e(tag, "Failed to save local tasks: ${e.message}", e)
        }
    }

    fun observeTasks(): Flow<List<Task>> {
        // Sirf local memory se data aayega, no Firebase
        return localTasksFlow.asStateFlow()
    }

    suspend fun getTaskById(taskId: String): Result<Task> {
        val found = localTasksFlow.value.find { it.id == taskId }
        return if (found != null) {
            Result.success(found)
        } else {
            Result.failure(NoSuchElementException("Task $taskId not found"))
        }
    }

    suspend fun createTask(task: Task): Result<String> {
        val newId = if (task.id.isNotBlank()) task.id else "task_" + UUID.randomUUID().toString().take(8)
        val createdTask = task.copy(id = newId)
        val current = localTasksFlow.value.toMutableList()
        current.add(0, createdTask)
        saveLocalTasks(current)
        return Result.success(newId)
    }

    suspend fun approveTask(taskId: String): Result<Unit> {
        val current = localTasksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val old = current[index]
            val updatedLogs = old.logs.toMutableList().apply {
                add("Task approved by director. Worker dispatched.")
            }
            current[index] = old.copy(status = "APPROVED", logs = updatedLogs)
            saveLocalTasks(current)
            return Result.success(Unit)
        }
        return Result.failure(NoSuchElementException("Task $taskId not found"))
    }

    suspend fun rejectTask(taskId: String, reason: String): Result<Unit> {
        val current = localTasksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val old = current[index]
            val updatedLogs = old.logs.toMutableList().apply {
                add("Task rejected by director: $reason")
            }
            current[index] = old.copy(status = "REJECTED", rejectionReason = reason, logs = updatedLogs)
            saveLocalTasks(current)
            return Result.success(Unit)
        }
        return Result.failure(NoSuchElementException("Task $taskId not found"))
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: String, newLog: String? = null): Result<Unit> {
        if (!newLog.isNullOrBlank()) LiveWorkerLogStore.record("Task Worker", newLog)
        
        val current = localTasksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val old = current[index]
            val updatedLogs = old.logs.toMutableList()
            if (!newLog.isNullOrBlank()) {
                updatedLogs.add(newLog)
            }
            current[index] = old.copy(status = newStatus, logs = updatedLogs)
            saveLocalTasks(current)
            return Result.success(Unit)
        }
        return Result.failure(NoSuchElementException("Task $taskId not found"))
    }

    suspend fun updateTaskExecution(
        taskId: String,
        newStatus: String,
        appendDetails: String? = null,
        newLog: String? = null
    ): Result<Unit> {
        if (!newLog.isNullOrBlank()) LiveWorkerLogStore.record("Task Worker", newLog)

        val current = localTasksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val old = current[index]
            val updatedLogs = old.logs.toMutableList()
            if (!newLog.isNullOrBlank()) {
                updatedLogs.add(newLog)
            }
            val finalDetails = if (!appendDetails.isNullOrBlank()) {
                if (old.details.isNotBlank()) "${old.details}\n\n$appendDetails" else appendDetails
            } else {
                old.details
            }
            current[index] = old.copy(status = newStatus, details = finalDetails, logs = updatedLogs)
            saveLocalTasks(current)
            return Result.success(Unit)
        }
        return Result.failure(NoSuchElementException("Task $taskId not found"))
    }

    suspend fun seedInitialTasksIfEmpty(): Result<Unit> {
        // App starts with empty state until real data is fetched with API keys
        return Result.success(Unit)
    }
}
