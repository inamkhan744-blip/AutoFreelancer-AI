package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.InboxEmail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InboxRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = InboxRepository(context)

    @Test
    fun saveEmail_andUpdateDraft_persistsChanges() = runBlocking {
        val email = InboxEmail(
            id = "test_inbox_1",
            senderName = "Lisa Wong",
            senderEmail = "lisa@boutique.co",
            subject = "Price check",
            incomingMessage = "Can you lower the rate?",
            objectionCategory = "Budget / Price",
            draftedReply = "Initial draft reply..."
        )

        repository.upsertMessage(email)
        val listAfterSave = repository.observeInbox().first()
        assertTrue(listAfterSave.any { it.id == "test_inbox_1" })

        repository.updateDraftedReply("test_inbox_1", "Updated convincing close reply.")
        val listAfterUpdate = repository.observeInbox().first()
        val updated = listAfterUpdate.first { it.id == "test_inbox_1" }
        assertEquals("Updated convincing close reply.", updated.draftedReply)

        repository.markApprovedAndSent("test_inbox_1")
        val listAfterSent = repository.observeInbox().first()
        val sent = listAfterSent.first { it.id == "test_inbox_1" }
        assertEquals("Approved & Sent", sent.status)
    }
}
