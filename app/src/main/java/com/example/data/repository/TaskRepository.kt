package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.error.OperationType
import com.example.data.error.handleFirestoreError
import com.example.data.model.Task
import com.example.util.LiveWorkerLogStore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import java.io.File
import java.util.UUID

class TaskRepository(
    private val db: FirebaseFirestore? = null,
    private val context: Context? = null
) {
    private val tag = "TaskRepository"
    private val auth by lazy { runCatching { Firebase.auth }.getOrNull() }
    private val localTasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private val storageFile: File? = context?.filesDir?.let { File(it, "autofreelancer_tasks.json") }

    constructor(context: Context) : this(db = null, context = context.applicationContext)

    constructor(db: FirebaseFirestore) : this(db = db, context = null)

    init {
        loadLocalTasks()
    }

    private fun loadLocalTasks() {
        val file = storageFile ?: return
        if (!file.exists()) {
            localTasksFlow.value = emptyList()
            return
        }
        try {
            val content = file.readText()
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
        val file = storageFile ?: return
        try {
            val jsonArr = JSONArray()
            tasks.forEach { jsonArr.put(it.toJson()) }
            file.writeText(jsonArr.toString())
        } catch (e: Exception) {
            Log.e(tag, "Failed to save local tasks: ${e.message}", e)
        }
    }

    fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeTasks(): Flow<List<Task>> {
        // If testing against Firestore directly
        if (db != null) {
            val uid = auth?.currentUser?.uid
            if (uid != null) {
                return callbackFlow {
                    val path = "users/$uid/tasks"
                    val query = db.collection("users").document(uid).collection("tasks")
                    val registration = query.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            handleFirestoreError(error, OperationType.LIST, path)
                            close(error)
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val tasks = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(Task::class.java)
                            }
                            trySend(tasks)
                        }
                    }
                    awaitClose { registration.remove() }
                }
            } else {
                // If unauthenticated in Firestore test mode, trigger query so rules test asserts permission denied
                return callbackFlow {
                    val path = "users/unauth/tasks"
                    val query = db.collection("users").document("unauth").collection("tasks")
                    val registration = query.addSnapshotListener { _, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                    }
                    awaitClose { registration.remove() }
                }
            }
        }

        // Standard device mode: backed by local persistent flow
        return localTasksFlow.asStateFlow()
    }

    suspend fun getTaskById(taskId: String): Result<Task> {
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            val path = "users/$uid/tasks/$taskId"
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            return try {
                val snapshot = docRef.get().await()
                val task = snapshot.toObject(Task::class.java)
                if (task != null) {
                    Result.success(task)
                } else {
                    Result.failure(NoSuchElementException("Task $taskId not found"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, path)
                Result.failure(e)
            }
        }

        val found = localTasksFlow.value.find { it.id == taskId }
        return if (found != null) {
            Result.success(found)
        } else {
            Result.failure(NoSuchElementException("Task $taskId not found"))
        }
    }

    suspend fun createTask(task: Task): Result<String> {
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            val collectionRef = db.collection("users").document(uid).collection("tasks")
            val docRef = if (task.id.isNotBlank()) collectionRef.document(task.id) else collectionRef.document()
            val path = docRef.path

            return try {
                val payload = mutableMapOf<String, Any>(
                    "title" to task.title,
                    "category" to task.category,
                    "workerName" to task.workerName,
                    "workerRole" to task.workerRole,
                    "status" to task.status,
                    "priority" to task.priority,
                    "client" to task.client,
                    "estimatedValue" to task.estimatedValue,
                    "details" to task.details,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (task.rejectionReason != null) {
                    payload["rejectionReason"] = task.rejectionReason
                }
                if (task.logs.isNotEmpty()) {
                    payload["logs"] = task.logs
                }

                docRef.set(payload).await()
                Result.success(docRef.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, path)
                Result.failure(e)
            }
        }

        // Local persistent creation
        val newId = if (task.id.isNotBlank()) task.id else "task_" + UUID.randomUUID().toString().take(8)
        val createdTask = task.copy(id = newId)
        val current = localTasksFlow.value.toMutableList()
        current.add(0, createdTask)
        saveLocalTasks(current)
        return Result.success(newId)
    }

    suspend fun approveTask(taskId: String): Result<Unit> {
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            val path = docRef.path

            return try {
                docRef.update(
                    mapOf(
                        "status" to "APPROVED",
                        "logs" to FieldValue.arrayUnion("Task approved by director. Worker dispatched."),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, path)
                Result.failure(e)
            }
        }

        // Local update
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
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            val path = docRef.path

            return try {
                docRef.update(
                    mapOf(
                        "status" to "REJECTED",
                        "rejectionReason" to reason,
                        "logs" to FieldValue.arrayUnion("Task rejected by director: $reason"),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, path)
                Result.failure(e)
            }
        }

        // Local update
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
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            val path = docRef.path

            return try {
                val updates = mutableMapOf<String, Any>(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!newLog.isNullOrBlank()) {
                    updates["logs"] = FieldValue.arrayUnion(newLog)
                }
                docRef.update(updates).await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, path)
                Result.failure(e)
            }
        }

        // Local update
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
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
            val path = docRef.path

            return try {
                val updates = mutableMapOf<String, Any>(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!appendDetails.isNullOrBlank()) {
                    val currentDoc = docRef.get().await()
                    val existingDetails = currentDoc.getString("details").orEmpty()
                    val combined = if (existingDetails.isNotBlank()) "$existingDetails\n\n$appendDetails" else appendDetails
                    updates["details"] = combined
                }
                if (!newLog.isNullOrBlank()) {
                    updates["logs"] = FieldValue.arrayUnion(newLog)
                }
                docRef.update(updates).await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, path)
                Result.failure(e)
            }
        }

        // Local update
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
        // Dummy data seeding removed: app starts with empty state until real data is fetched with API keys
        return Result.success(Unit)
    }
}
