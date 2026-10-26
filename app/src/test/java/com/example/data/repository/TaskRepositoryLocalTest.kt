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
    fun initialLaunch_seedsSampleTasksAutomatically() = runBlocking {
        val tasks = repository.observeTasks().first()
        assertTrue("Initial tasks should not be empty", tasks.isNotEmpty())
        assertTrue("Should include LinkedIn SEO scouting task", tasks.any { it.title.contains("LinkedIn") })
        assertTrue("Should include SEO audit report task", tasks.any { it.title.contains("SEO audit report") })
    }

    @Test
    fun approveTask_updatesLocalTaskStatus() = runBlocking {
        val tasks = repository.observeTasks().first()
        val pendingTask = tasks.first { it.status == "PENDING" }

        val result = repository.approveTask(pendingTask.id)
        assertTrue(result.isSuccess)

        val updated = repository.getTaskById(pendingTask.id).getOrThrow()
        assertEquals("APPROVED", updated.status)
        assertTrue(updated.logs.any { it.contains("Worker dispatched") })
    }

    @Test
    fun rejectTask_updatesStatusAndStoresReason() = runBlocking {
        val tasks = repository.observeTasks().first()
        val pendingTask = tasks.first { it.status == "PENDING" }

        val reason = "Estimated budget too low"
        val result = repository.rejectTask(pendingTask.id, reason)
        assertTrue(result.isSuccess)

        val updated = repository.getTaskById(pendingTask.id).getOrThrow()
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
