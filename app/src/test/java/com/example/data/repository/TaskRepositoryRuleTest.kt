package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Task
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TaskRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createTask_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = TaskRepository(firestore)

        val task = Task(
            id = "task_" + UUID.randomUUID().toString().take(8),
            title = "Scouting for SEO clients on LinkedIn",
            category = "Client Acquisition",
            workerName = "Apollo-Scout",
            workerRole = "Lead Generation Specialist",
            status = "PENDING",
            priority = "HIGH",
            client = "Fintech & SaaS",
            estimatedValue = 1500.0,
            details = "Scrape 30 profiles and draft outreach messages"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(task)
        }
        assertTrue("Task creation should succeed", result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun observeTasks_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = TaskRepository(firestore)
        val taskId = "task_" + UUID.randomUUID().toString().take(8)

        val task = Task(
            id = taskId,
            title = "Delivering SEO audit report",
            category = "Deliverable",
            workerName = "ReportBot-Pro",
            workerRole = "Technical SEO Analyst",
            status = "PENDING",
            priority = "HIGH",
            client = "TechVentures",
            estimatedValue = 2200.0,
            details = "Audit Core Web Vitals and keywords"
        )

        repository.createTask(task).getOrThrow()

        val emittedTasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks().first { list -> list.any { it.id == taskId } }
        }
        assertTrue(emittedTasks.any { it.id == taskId })
    }

    @Test
    fun approveTask_authenticatedOwner_updatesStatusToApproved() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = TaskRepository(firestore)
        val taskId = "task_" + UUID.randomUUID().toString().take(8)

        val task = Task(
            id = taskId,
            title = "Generating proposal for Shopify redesign",
            category = "Proposal",
            workerName = "PitchCraft AI",
            workerRole = "Proposal Writer",
            status = "PENDING",
            priority = "MEDIUM",
            client = "Apex Store",
            estimatedValue = 1800.0,
            details = "Design overhaul proposal"
        )
        repository.createTask(task).getOrThrow()

        val approveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.approveTask(taskId)
        }
        assertTrue(approveResult.isSuccess)

        val updatedTask = repository.getTaskById(taskId).getOrThrow()
        assertEquals("APPROVED", updatedTask.status)
    }

    @Test
    fun rejectTask_authenticatedOwner_updatesStatusToRejectedWithReason() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = TaskRepository(firestore)
        val taskId = "task_" + UUID.randomUUID().toString().take(8)

        val task = Task(
            id = taskId,
            title = "Cold email outreach",
            category = "Outreach",
            workerName = "OutreachGenie",
            workerRole = "Outreach Bot",
            status = "PENDING",
            priority = "LOW",
            client = "Unverified Target",
            estimatedValue = 500.0,
            details = "Mass email campaign"
        )
        repository.createTask(task).getOrThrow()

        val rejectResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.rejectTask(taskId, "Target audience out of scope")
        }
        assertTrue(rejectResult.isSuccess)

        val updatedTask = repository.getTaskById(taskId).getOrThrow()
        assertEquals("REJECTED", updatedTask.status)
        assertEquals("Target audience out of scope", updatedTask.rejectionReason)
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = TaskRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks().first()
            }
        } catch (e: Exception) {
            caughtException = e
        }
        assertNotNull("Expected exception when unauthenticated", caughtException)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_test@autofreelancer.ai"
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
