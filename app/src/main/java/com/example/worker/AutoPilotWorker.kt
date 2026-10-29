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
import com.example.data.model.OutreachTone
import com.example.data.model.Task
import com.example.data.repository.ReplyRateRepository
import com.example.data.repository.ClientRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.data.service.AiOrchestrationService
import com.example.data.service.BrevoEmailService
import com.example.data.service.CompetitorSpyPrompt
import com.example.data.service.DynamicPricingEngine
import com.example.data.service.SearchProspectService
import com.example.util.BusinessEmailValidator
import com.example.util.LiveWorkerLogStore

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class AutoPilotWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val tag = "AutoPilotWorker"

    override suspend fun doWork(): Result {
        try {
            Log.i(tag, "Auto-Pilot worker triggered in background")
            LiveWorkerLogStore.record("Auto-Pilot", "> Background worker started.")
            val context = applicationContext
            val settingsRepo = SettingsRepository(context)
            try {
                settingsRepo.loadCloudSettings()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "Failed to load cloud settings: ${e.message}", e)
            }
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
            val replyRateRepository = ReplyRateRepository()
            val pricingEngine = DynamicPricingEngine(aiService)

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
                    replyRateRepository = replyRateRepository,
                    pricingEngine = pricingEngine,
                    searchService = searchService,
                    emailService = emailService
                )
            }

            // 2. Discover new leads only when there are no tasks waiting for approval.
            val canRunDiscovery = settings.isSearchConfigured || settings.isAiConfigured
            if (pendingTasks.isEmpty() && canRunDiscovery) {
                Log.i(tag, "Running autonomous lead discovery in background...")
                LiveWorkerLogStore.record("Apollo-Scout", "> Scraping Reddit, LinkedIn, and web search results...")
                val searchResult = searchService.searchWithFallback(
                    query = settings.searchQuery,
                    serperKey = settings.serperApiKey,
                    tavilyKey = settings.tavilyApiKey
                )

                val knownProspects = ProspectDeduplicator(
                    tasks = taskRepo.observeTasks().first(),
                    clients = clientRepo.observeClients().first()
                )
                val leads = searchResult.leads.mapNotNull { prospect ->
                    val discoveredEmail = BusinessEmailValidator.findIn("${prospect.snippet}\n${prospect.title}")
                    if (discoveredEmail == null) {
                        Log.i(tag, "Skipping lead without a valid business email: ${prospect.title}")
                        null
                    } else if (knownProspects.contains(prospect.link, discoveredEmail)) {
                        Log.i(tag, "Skipping existing lead: ${prospect.link}")
                        null
                    } else {
                        knownProspects.remember(prospect.link, discoveredEmail)
                        prospect to discoveredEmail
                    }
                }.take(3)

                for ((prospect, discoveredEmail) in leads) {
                    val outreachTone = replyRateRepository.bestPerformingTone()
                    val targetWebsite = CompetitorSpyPrompt.websiteFor(prospect)
                    LiveWorkerLogStore.record("Apollo-Scout", "> Analyzing business scale for a dynamic quote...")
                    val pricing = pricingEngine.decide(
                        website = targetWebsite,
                        context = "${prospect.title}\n${prospect.snippet}",
                        geminiApiKey = settings.geminiApiKey,
                        openRouterApiKey = settings.openRouterApiKey,
                        geminiModel = settings.geminiModel,
                        openRouterModel = settings.openRouterModel
                    )
                    val dynamicPaymentLink = pricingEngine.paymentLinkForAmount(settings.paymentLinkUrl, pricing.amount)
                    LiveWorkerLogStore.record("Apollo-Scout", "> ${prospect.sourcePlatform}: selected $${pricing.amount}; tone ${outreachTone.promptLabel}.")

                    val prompt = "Write a persuasive personalized pitch.\nTitle: ${prospect.title}\nContext: ${prospect.snippet}\nClient website: ${targetWebsite.ifBlank { "not provided" }}\nSource platform: ${prospect.sourcePlatform}"
                    val systemPrompt = "You are Apollo-Scout. Compare the client's site with three plausible direct competitors and write in a ${outreachTone.promptLabel.lowercase()} tone. Be explicit, concise, and conversion-focused."
                    LiveWorkerLogStore.record("Apollo-Scout", "> Comparing three likely competitors and generating price...")

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

                    if (dynamicPaymentLink.isNotBlank()) {
                        pitchBody += "\n\nThe ${outreachTone.promptLabel.lowercase()} audit package is $${pricing.amount}. Complete payment here: $dynamicPaymentLink"
                    }

                    val autoTask = Task(
                        title = "Auto-Pilot: Outreach to ${prospect.title.take(35)}",
                        category = "Client Acquisition",
                        workerName = "Apollo-Scout",
                        workerRole = "Autonomous Scout",
                        status = "PENDING",
                        priority = "HIGH",
                        client = "${prospect.sourcePlatform} Lead",
                        clientEmail = discoveredEmail,
                        estimatedValue = pricing.amount.toDouble(),
                        details = buildString {
                            append("AUTONOMOUS BACKGROUND OUTREACH:\n\n")
                            append(pitchBody)
                            append("\n\nDiscovered URL: ${prospect.link}\n")
                        },
                        logs = listOf(
                            "⚡ [Auto-Pilot] Discovered prospect via ${searchResult.providerUsed} on ${prospect.sourcePlatform}",
                            "⚡ [Auto-Pilot] Proposal drafted and queued for Auto-Pilot execution.",
                            "Selected outreach tone: ${outreachTone.name}",
                            "Dynamic audit quote: $${pricing.amount} (${pricing.segment})"
                        )
                    )

                    val taskResult = taskRepo.createTask(autoTask)
                    if (taskResult.isFailure) {
                        val error = taskResult.exceptionOrNull()?.message ?: "Unable to save discovered lead task."
                        Log.e(tag, "Failed to save discovered lead ${prospect.link}: $error")
                        continue
                    }

                    val crmLead = CrmClient(
                        name = prospect.title.take(40),
                        company = prospect.sourcePlatform,
                        websiteUrl = prospect.link,
                        email = discoveredEmail,
                        status = "Pitched",
                        leadSource = "Auto-Pilot Discovery",
                        pitchMessage = pitchBody,
                        estimatedValue = pricing.amount.toDouble()
                    )
                    val clientResult = clientRepo.upsertClient(crmLead)
                    if (clientResult.isFailure) {
                        val error = clientResult.exceptionOrNull()?.message ?: "Unable to save lead to CRM."
                        Log.e(tag, "Failed to save discovered lead ${prospect.link} to CRM: $error")
                    }
                }
            }

            return Result.success()
        } catch (e: CancellationException) {
            throw e
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
        replyRateRepository: ReplyRateRepository,
        pricingEngine: DynamicPricingEngine,
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

        val outreachTone = replyRateRepository.bestPerformingTone()
        val targetWebsite = searchResult.leads.firstOrNull()?.let(CompetitorSpyPrompt::websiteFor).orEmpty()
        val pricing = pricingEngine.decide(
            website = targetWebsite,
            context = "${task.client}\n${task.title}\n${task.details}",
            geminiApiKey = settings.geminiApiKey,
            openRouterApiKey = settings.openRouterApiKey,
            geminiModel = settings.geminiModel,
            openRouterModel = settings.openRouterModel
        )
        val dynamicPaymentLink = pricingEngine.paymentLinkForAmount(settings.paymentLinkUrl, pricing.amount)
        LiveWorkerLogStore.record(task.workerName, "> Applying ${outreachTone.promptLabel} tone and $${pricing.amount} quote.")
        LiveWorkerLogStore.record(task.workerName, "> Comparing client website with three likely competitors...")

        // 2. Generate Deliverable with dynamic pricing and payment link
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
            paymentLinkUrl = dynamicPaymentLink,
            outreachTone = outreachTone,
            targetWebsite = targetWebsite,
            dynamicPrice = pricing.amount
        )

        val deliverableContent = if (aiResult.isSuccess) {
            aiResult.getOrThrow().content
        } else {
            val fallbackText = "Comprehensive SEO audit formulated for ${task.client}."
            if (dynamicPaymentLink.isNotBlank()) {
                "$fallbackText\n\nThe ${outreachTone.promptLabel.lowercase()} audit package is $${pricing.amount}. Complete payment here: $dynamicPaymentLink"
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
                    replyRateRepository.recordSentPitch(targetEmail, outreachTone)
                    LiveWorkerLogStore.record(task.workerName, "> Pitch sent; reply-rate event recorded.")
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
            estimatedValue = pricing.amount.toDouble()
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
                4, TimeUnit.HOURS
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
