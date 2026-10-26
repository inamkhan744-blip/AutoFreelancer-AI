package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.error.OperationType
import com.example.data.error.handleFirestoreError
import com.example.data.model.Task
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
            val sampleTasks = getInitialSampleTasks()
            saveLocalTasks(sampleTasks)
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
                if (list.isEmpty()) {
                    val sampleTasks = getInitialSampleTasks()
                    saveLocalTasks(sampleTasks)
                } else {
                    localTasksFlow.value = list
                }
            } else {
                val sampleTasks = getInitialSampleTasks()
                saveLocalTasks(sampleTasks)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load local tasks: ${e.message}", e)
            val sampleTasks = getInitialSampleTasks()
            saveLocalTasks(sampleTasks)
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

    suspend fun seedInitialTasksIfEmpty(): Result<Unit> {
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            val collection = db.collection("users").document(uid).collection("tasks")
            return try {
                val existing = collection.limit(1).get().await()
                if (existing.isEmpty) {
                    val sampleTasks = getInitialSampleTasks()
                    for (task in sampleTasks) {
                        createTask(task)
                    }
                }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "users/$uid/tasks")
                Result.failure(e)
            }
        }

        // Local seeding
        if (localTasksFlow.value.isEmpty()) {
            val sampleTasks = getInitialSampleTasks()
            saveLocalTasks(sampleTasks)
        }
        return Result.success(Unit)
    }

    private fun getInitialSampleTasks(): List<Task> {
        return listOf(
            Task(
                id = "task_init_1",
                title = "Scouting for SEO clients on LinkedIn",
                category = "Client Acquisition",
                workerName = "Apollo-Scout",
                workerRole = "Lead Generation Specialist",
                status = "PENDING",
                priority = "HIGH",
                client = "Fintech & SaaS Series A",
                estimatedValue = 1800.0,
                details = "AI worker will scrape 50 marketing director profiles on LinkedIn, analyze their organic search gaps, and prepare custom pitch decks for review.",
                logs = listOf("Target ICP identified", "Initial 50 prospect profiles gathered")
            ),
            Task(
                id = "task_init_2",
                title = "Delivering SEO audit report",
                category = "Deliverable",
                workerName = "ReportBot-Pro",
                workerRole = "Technical SEO Analyst",
                status = "PENDING",
                priority = "HIGH",
                client = "TechVentures Inc.",
                estimatedValue = 2400.0,
                details = "Full technical site audit covering Core Web Vitals, indexing bottlenecks, backlink velocity, and top 20 keyword cluster recommendations ready for client delivery.",
                logs = listOf("Site crawl complete: 1,420 URLs indexed", "Speed analysis generated")
            ),
            Task(
                id = "task_init_3",
                title = "Generating proposal for Shopify redesign",
                category = "Proposal",
                workerName = "PitchCraft AI",
                workerRole = "Proposal & Scope Architect",
                status = "PENDING",
                priority = "MEDIUM",
                client = "Apex Apparel Co.",
                estimatedValue = 3500.0,
                details = "Created 12-page conversion rate optimization proposal detailing checkout flow improvements, headless architecture suggestions, and project milestones.",
                logs = listOf("Client RFP parsed", "Scope breakdown formulated")
            ),
            Task(
                id = "task_init_4",
                title = "Cold email outreach sequence for B2B leads",
                category = "Outreach",
                workerName = "OutreachGenie",
                workerRole = "Cold Outreach Agent",
                status = "RUNNING",
                priority = "MEDIUM",
                client = "CloudOps Providers",
                estimatedValue = 1200.0,
                details = "Sending personalized 3-step value-first email series to verified DevOps managers. Current open rate: 64%.",
                logs = listOf("Worker started execution", "Batch 1 sent (30 emails)", "3 replies received")
            ),
            Task(
                id = "task_init_5",
                title = "Competitor backlink gap analysis",
                category = "Market Research",
                workerName = "LinkIntel AI",
                workerRole = "Off-Page SEO Agent",
                status = "COMPLETED",
                priority = "LOW",
                client = "HealthPulse App",
                estimatedValue = 950.0,
                details = "Analyzed top 3 market competitors, discovered 42 high-DR guest blogging and citation opportunities.",
                logs = listOf("Domain matrices extracted", "Final CSV report compiled and stored")
            )
        )
    }
}
