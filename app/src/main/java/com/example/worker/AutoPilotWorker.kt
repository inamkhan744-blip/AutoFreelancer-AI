package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.model.CrmClient
import com.example.data.model.Task
import com.example.data.repository.ClientRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.data.service.AiOrchestrationService
import com.example.data.service.BrevoEmailService
import com.example.data.service.SearchProspectService
import com.example.util.BusinessEmailValidator
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class AutoPilotWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "AutoPilotWorker"

    override suspend fun doWork(): Result {
        Log.i(tag, "Auto-Pilot worker triggered in background")
        val context = applicationContext
        val settingsRepo = SettingsRepository(context)
        runCatching { settingsRepo.loadCloudSettings() }
            .onFailure { Log.e(tag, "Failed to load cloud settings: ${it.message}", it) }
        val settings = settingsRepo.settingsFlow.value

        if (!settings.autoPilotEnabled) {
            Log.i(tag, "Auto-Pilot is currently disabled in Settings. Exiting job.")
            return Result.success()
        }

        val taskRepo = TaskRepository(context)
        val clientRepo = ClientRepository(context)
        val aiService = AiOrchestrationService()
        val searchService = SearchProspectService()
        val emailService = BrevoEmailService()

        try {
            // 1. Process all currently PENDING tasks automatically
            val currentTasks = taskRepo.observeTasks().first()
            val pendingTasks = currentTasks.filter { it.status == "PENDING" }

            Log.i(tag, "Found ${pendingTasks.size} pending tasks to auto-execute")

            for (task in pendingTasks) {
                executeTaskAutonomously(
                    task = task,
                    settingsRepo = settingsRepo,
                    taskRepo = taskRepo,
                    clientRepo = clientRepo,
                    aiService = aiService,
                    searchService = searchService,
                    emailService = emailService
                )
            }

            // 2. Autonomous Lead Discovery & Pitch Generation in the background
            val canRunDiscovery = settings.isSearchConfigured || settings.isAiConfigured
            if (canRunDiscovery) {
                Log.i(tag, "Running autonomous lead discovery in background...")
                val searchResult = searchService.searchWithFallback(
                    query = settings.searchQuery,
                    serperKey = settings.serperApiKey,
                    tavilyKey = settings.tavilyApiKey
                )

                val leads = searchResult.leads.mapNotNull { prospect ->
                    BusinessEmailValidator.findIn("${prospect.snippet}\n${prospect.title}")
                        ?.let { prospect to it }
                        ?: run {
                            Log.i(tag, "Skipping lead without a valid business email: ${prospect.title}")
                            null
                        }
                }.take(2)
                for ((prospect, discoveredEmail) in leads) {

                    val prompt = "Write a high-converting, personalized client pitch for this lead looking for an SEO audit:\nTitle: ${prospect.title}\nContext: ${prospect.snippet}\nLink: ${prospect.link}"
                    val systemPrompt = "You are Apollo-Scout, an elite autonomous freelance SEO scout. Formulate a personalized outreach message with clear findings."

                    val aiRes = aiService.generateContentWithFallback(
                        geminiApiKey = settings.geminiApiKey,
                        openRouterApiKey = settings.openRouterApiKey,
                        geminiModel = settings.geminiModel,
                        openRouterModel = settings.openRouterModel,
                        systemPrompt = systemPrompt,
                        userPrompt = prompt,
                        maxTokens = 800
                    )

                    var pitchBody = if (aiRes.isSuccess) {
                        aiRes.getOrThrow().content
                    } else {
                        "Saw your request for an SEO audit. We can deliver a comprehensive technical analysis."
                    }

                    if (settings.paymentLinkUrl.isNotBlank()) {
                        pitchBody += "\n\nTo get the full SEO audit report, please complete the payment here: ${settings.paymentLinkUrl.trim()}"
                    }

                    val autoTask = Task(
                        title = "Auto-Pilot: Outreach to ${prospect.title.take(35)}",
                        category = "Client Acquisition",
                        workerName = "Apollo-Scout",
                        workerRole = "Autonomous Scout",
                        status = "COMPLETED",
                        priority = "HIGH",
                        client = "${prospect.sourcePlatform} Lead",
                        clientEmail = discoveredEmail,
                        estimatedValue = 0.0,
                        details = buildString {
                            append("AUTONOMOUS BACKGROUND OUTREACH:\n\n")
                            append(pitchBody)
                            append("\n\nDiscovered URL: ${prospect.link}\n")
                        },
                        logs = listOf(
                            "⚡ [Auto-Pilot] Discovered prospect via ${searchResult.providerUsed} on ${prospect.sourcePlatform}",
                            "⚡ [Auto-Pilot] Proposal formulated with dynamic CTA and finalized."
                        )
                    )

                    taskRepo.createTask(autoTask)

                    // Send outreach only when Brevo is configured.
                    if (settings.isBrevoConfigured) {
                        if (discoveredEmail.isNotBlank()) {
                            val sendRes = emailService.sendEmail(
                                apiKey = settings.brevoApiKey,
                                toEmail = discoveredEmail,
                                subject = "SEO Audit & Growth Proposal: ${autoTask.title}",
                                bodyText = pitchBody
                            )
                            if (sendRes.isSuccess) {
                                taskRepo.updateTaskStatus(
                                    taskId = autoTask.id,
                                    newStatus = "COMPLETED",
                                    newLog = "Email sent successfully via Brevo"
                                )
                            } else {
                                val err = sendRes.exceptionOrNull()?.message ?: "Unknown delivery error"
                                taskRepo.updateTaskStatus(
                                    taskId = autoTask.id,
                                    newStatus = "COMPLETED",
                                    newLog = "Brevo delivery error: $err"
                                )
                            }
                        } else {
                            taskRepo.updateTaskStatus(
                                taskId = autoTask.id,
                                newStatus = "COMPLETED",
                                newLog = "Error: Client email is missing. Please provide client email to send proposal."
                            )
                        }
                    }

                    // Feature 3: Persist lead into CRM
                    val crmLead = CrmClient(
                        name = prospect.title.take(40),
                        company = prospect.sourcePlatform,
                        websiteUrl = prospect.link,
                        email = discoveredEmail,
                        status = "Pitched",
                        leadSource = "Auto-Pilot Discovery",
                        pitchMessage = pitchBody,
                        estimatedValue = 0.0
                    )
                    clientRepo.upsertClient(crmLead)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Auto-Pilot execution encountered error: ${e.message}", e)
            return Result.failure()
        }
    }

    private suspend fun executeTaskAutonomously(
        task: Task,
        settingsRepo: SettingsRepository,
        taskRepo: TaskRepository,
        clientRepo: ClientRepository,
        aiService: AiOrchestrationService,
        searchService: SearchProspectService,
        emailService: BrevoEmailService
    ) {
        val settings = settingsRepo.settingsFlow.value
        taskRepo.updateTaskStatus(
            taskId = task.id,
            newStatus = "RUNNING",
            newLog = "⚡ [Auto-Pilot] Autonomously executing task approval & deliverable synthesis..."
        )

        // 1. Search leads if needed
        val searchResult = searchService.searchWithFallback(
            query = "SEO audit ${task.title}",
            serperKey = settings.serperApiKey,
            tavilyKey = settings.tavilyApiKey
        )

        // 2. Generate Deliverable with Payment Link
        val aiResult = aiService.generateTaskDeliverable(
            geminiApiKey = settings.geminiApiKey,
            openRouterApiKey = settings.openRouterApiKey,
            geminiModel = settings.geminiModel,
            openRouterModel = settings.openRouterModel,
            taskTitle = task.title,
            category = task.category,
            client = task.client,
            workerName = task.workerName,
            workerRole = task.workerRole,
            leads = searchResult.leads,
            paymentLinkUrl = settings.paymentLinkUrl
        )

        val deliverableContent = if (aiResult.isSuccess) {
            aiResult.getOrThrow().content
        } else {
            val fallbackText = "Comprehensive SEO audit formulated for ${task.client}."
            if (settings.paymentLinkUrl.isNotBlank()) {
                "$fallbackText\n\nTo get the full SEO audit report, please complete the payment here: ${settings.paymentLinkUrl.trim()}"
            } else {
                fallbackText
            }
        }

        val deliverableSection = """
        |========================================
        |AI WORKER DELIVERABLE [AUTO-PILOT]
        |========================================
        |$deliverableContent
        """.trimMargin()

        taskRepo.updateTaskExecution(
            taskId = task.id,
            newStatus = "COMPLETED",
            appendDetails = deliverableSection,
            newLog = "⚡ [Auto-Pilot] Deliverable autonomously synthesized and approved."
        )

        // 3. Auto-dispatch email if target email present
        val targetEmail = task.clientEmail.trim().takeIf(BusinessEmailValidator::isValid)
            ?: searchResult.leads.firstNotNullOfOrNull { item ->
                BusinessEmailValidator.findIn("${item.snippet}\n${item.title}")
            }.orEmpty()

        if (settings.isBrevoConfigured) {
            if (targetEmail.isNotBlank()) {
                val emailRes = emailService.sendEmail(
                    apiKey = settings.brevoApiKey,
                    toEmail = targetEmail,
                    subject = "SEO Audit & Growth Proposal: ${task.title}",
                    bodyText = deliverableContent
                )
                if (emailRes.isSuccess) {
                    val sent = emailRes.getOrThrow()
                    taskRepo.updateTaskStatus(
                        taskId = task.id,
                        newStatus = "COMPLETED",
                        newLog = "Email sent successfully via Brevo"
                    )
                } else {
                    val err = emailRes.exceptionOrNull()?.message ?: "Unknown delivery error"
                    taskRepo.updateTaskStatus(
                        taskId = task.id,
                        newStatus = "COMPLETED",
                        newLog = "Brevo delivery error: $err"
                    )
                }
            } else {
                taskRepo.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "COMPLETED",
                    newLog = "Error: Client email is missing. Please provide client email to send proposal."
                )
            }
        }

        // Feature 3: Persist lead into CRM
        val crmClient = CrmClient(
            name = task.client,
            company = task.category,
            websiteUrl = searchResult.leads.firstOrNull()?.link.orEmpty(),
            email = targetEmail,
            status = "Pitched",
            leadSource = "Auto-Pilot Worker",
            pitchMessage = deliverableContent,
            estimatedValue = task.estimatedValue
        )
        clientRepo.upsertClient(crmClient)
    }
}

object AutoPilotScheduler {
    private const val UNIQUE_WORK_NAME = "auto_pilot_work"
    private const val UNIQUE_IMMEDIATE_WORK_NAME = "auto_pilot_work_immediate"

    fun schedule(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<AutoPilotWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )

            val immediateRequest = OneTimeWorkRequestBuilder<AutoPilotWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(
                UNIQUE_IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            )
            Log.i("AutoPilotScheduler", "Auto-Pilot periodic and immediate work scheduled.")
        } else {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            workManager.cancelUniqueWork(UNIQUE_IMMEDIATE_WORK_NAME)
            Log.i("AutoPilotScheduler", "Auto-Pilot work cancelled.")
        }
    }
}
