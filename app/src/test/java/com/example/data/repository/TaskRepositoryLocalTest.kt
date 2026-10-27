package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TaskRepositoryLocalTest {

    private lateinit var context: Context
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Clear local storage file before test
        val file = File(context.filesDir, "autofreelancer_tasks.json")
        if (file.exists()) {
            file.delete()
        }
        repository = TaskRepository(context)
    }

    @Test
    fun initialLaunch_startsEmptyWithoutDummyData() = runBlocking {
        val tasks = repository.observeTasks().first()
        assertTrue("Initial tasks should be empty without dummy data", tasks.isEmpty())
    }

    @Test
    fun approveTask_updatesLocalTaskStatus() = runBlocking {
        val testTask = Task(
            id = "test_lead_1",
            title = "SEO Audit for Growth Co",
            category = "Lead",
            workerName = "ScoutBot",
            workerRole = "Lead Analyst",
            status = "PENDING",
            priority = "HIGH",
            client = "Growth Co",
            estimatedValue = 1500.0,
            details = "Audit technical performance and backlink profile"
        )
        repository.createTask(testTask)

        val result = repository.approveTask(testTask.id)
        assertTrue(result.isSuccess)

        val updated = repository.getTaskById(testTask.id).getOrThrow()
        assertEquals("APPROVED", updated.status)
        assertTrue(updated.logs.any { it.contains("Worker dispatched") })
    }

    @Test
    fun rejectTask_updatesStatusAndStoresReason() = runBlocking {
        val testTask = Task(
            id = "test_lead_2",
            title = "SEO Audit for Low Budget Lead",
            category = "Lead",
            workerName = "ScoutBot",
            workerRole = "Lead Analyst",
            status = "PENDING",
            priority = "LOW",
            client = "SmallBiz",
            estimatedValue = 300.0,
            details = "Audit site"
        )
        repository.createTask(testTask)

        val reason = "Estimated budget too low"
        val result = repository.rejectTask(testTask.id, reason)
        assertTrue(result.isSuccess)

        val updated = repository.getTaskById(testTask.id).getOrThrow()
        assertEquals("REJECTED", updated.status)
        assertEquals(reason, updated.rejectionReason)
    }

    @Test
    fun createTask_persistsAndRetrievesNewTask() = runBlocking {
        val customTask = Task(
            id = "custom_" + UUID.randomUUID().toString().take(6),
            title = "Keyword research for LegalTech",
            category = "Keyword Strategy",
            workerName = "LegalKeywordBot",
            workerRole = "Niche Researcher",
            status = "PENDING",
            priority = "MEDIUM",
            client = "LawFirm Solutions",
            estimatedValue = 1600.0,
            details = "Identify top 50 low-competition legal terms"
        )

        val createResult = repository.createTask(customTask)
        assertTrue(createResult.isSuccess)

        val retrieved = repository.getTaskById(customTask.id).getOrThrow()
        assertEquals(customTask.title, retrieved.title)
        assertEquals(customTask.client, retrieved.client)
    }
}
