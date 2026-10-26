package com.example.data.repository

import android.content.Context
import com.example.R
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
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TaskRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeTasks(): Flow<List<Task>> = callbackFlow {
        val uid = requireUserId()
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

        awaitClose {
            registration.remove()
        }
    }

    suspend fun getTaskById(taskId: String): Result<Task> {
        val uid = requireUserId()
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

    suspend fun createTask(task: Task): Result<String> {
        val uid = requireUserId()
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

    suspend fun approveTask(taskId: String): Result<Unit> {
        val uid = requireUserId()
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

    suspend fun rejectTask(taskId: String, reason: String): Result<Unit> {
        val uid = requireUserId()
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

    suspend fun updateTaskStatus(taskId: String, newStatus: String, newLog: String? = null): Result<Unit> {
        val uid = requireUserId()
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

    suspend fun seedInitialTasksIfEmpty(): Result<Unit> {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("tasks")
        return try {
            val existing = collection.limit(1).get().await()
            if (existing.isEmpty) {
                val sampleTasks = listOf(
                    Task(
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
}
