package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.model.InboxEmail
import com.example.data.repository.InboxRepository
import com.example.data.repository.SettingsRepository
import com.example.data.service.AiCloserService
import com.example.data.service.ImapEmailService
import com.example.util.LiveWorkerLogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ImapSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    private val tag = "ImapSyncWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settingsRepo = SettingsRepository(applicationContext)
        val settings = settingsRepo.settingsFlow.value

        if (!settings.isImapConfigured) {
            Log.d(tag, "IMAP is not configured; skipping sync.")
            return@withContext Result.success()
        }

        val imapService = ImapEmailService()
        val inboxRepo = InboxRepository(applicationContext)
        val aiCloser = AiCloserService()

        LiveWorkerLogStore.record("IMAP-Sync", "> Polling Gmail IMAP for client replies...")

        val fetchResult = imapService.fetchUnseenReplies(
            email = settings.imapEmail,
            appPassword = settings.imapAppPassword
        )

        if (fetchResult.isFailure) {
            val errorMsg = fetchResult.exceptionOrNull()?.message ?: "Unknown IMAP error"
            Log.w(tag, "IMAP fetch failed: $errorMsg")
            LiveWorkerLogStore.record("IMAP-Sync", "> IMAP check notice: $errorMsg")
            return@withContext Result.success()
        }

        val replies = fetchResult.getOrThrow()
        if (replies.isEmpty()) {
            LiveWorkerLogStore.record("IMAP-Sync", "> Inbox up to date. No new unread client replies.")
            return@withContext Result.success()
        }

        val existingEmails = inboxRepo.observeInbox().first()
        var newlyDraftedCount = 0

        for (reply in replies) {
            val isDuplicate = existingEmails.any { email ->
                email.senderEmail.equals(reply.senderEmail, ignoreCase = true) &&
                        email.subject.equals(reply.subject, ignoreCase = true) &&
                        Math.abs(email.receivedAt - reply.receivedAt) < 300_000L
            }
            if (isDuplicate) continue

            val draftResult = if (settings.isAiConfigured) {
                runCatching {
                    aiCloser.analyzeAndDraftReply(
                        senderName = reply.senderName,
                        senderEmail = reply.senderEmail,
                        subject = reply.subject,
                        incomingMessage = reply.body,
                        geminiApiKey = settings.geminiApiKey,
                        openRouterApiKey = settings.openRouterApiKey,
                        geminiModel = settings.geminiModel,
                        openRouterModel = settings.openRouterModel,
                        paymentLinkUrl = settings.paymentLinkUrl,
                        bookingLinkUrl = settings.bookingLinkUrl,
                        agencyName = settings.agencyName
                    ).getOrNull()
                }.getOrNull()
            } else null

            val newInboxEmail = InboxEmail(
                senderName = reply.senderName,
                senderEmail = reply.senderEmail,
                subject = reply.subject,
                incomingMessage = reply.body,
                objectionCategory = draftResult?.objectionCategory ?: "General Inquiry",
                draftedReply = draftResult?.draftedReply.orEmpty(),
                reasoningAnalysis = draftResult?.reasoningAnalysis.orEmpty(),
                reasoningStrategy = draftResult?.reasoningStrategy.orEmpty(),
                nextBestAction = draftResult?.nextBestAction.orEmpty(),
                receivedAt = reply.receivedAt
            )

            inboxRepo.upsertMessage(newInboxEmail)
            newlyDraftedCount++
            LiveWorkerLogStore.record(
                "IMAP-Sync",
                "> Received response from ${reply.senderEmail} ('${reply.subject.take(25)}'). AI Objection closer drafted reply."
            )
        }

        Log.i(tag, "IMAP Sync completed. Processed $newlyDraftedCount new replies.")
        Result.success()
    }
}
