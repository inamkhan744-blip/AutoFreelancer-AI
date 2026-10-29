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
import com.example.data.repository.ClientRepository
import com.example.data.repository.ReplyRateRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.data.service.AiOrchestrationService
import com.example.data.service.BrevoEmailService
import com.example.data.service.CompetitorSpyPrompt
import com.example.data.service.DynamicPricingEngine
import com.example.data.service.SearchProspectService
import com.example.util.BusinessEmailValidator
import com.example.util.LiveWorkerLogStore
import com.example.util.ProspectDeduplicator
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

            checkForStaleFollowUps(taskRepo, settings, emailService)

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

                    val prompt = "Write a persuasive personalized pitch.\nTitle: ${prospect.title}\nContext: ${prospect.snippet}\nClient website: ${targetWebsite.ifBlank { "not provided" }}\nSource: ${prospect.sourcePlatform}\n"
                    val systemPrompt = "You are Apollo-Scout. Compare the client's site with three plausible direct competitors and write in a ${outreachTone.promptLabel.lowercase()} tone. Be explicit, concise, and focused on conversion."
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

                    val siteUrl = targetWebsite.ifBlank { prospect.link }
                    val miniAuditPoints = emailService.generateMiniAuditPoints(siteUrl)
                    val htmlBody = emailService.buildHtmlEmailBody(pitchBody, miniAuditPoints)
                    val leadScore = computeLeadScore(targetWebsite, pricing.amount, discoveredEmail)

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
                            append("\nMini-audit:\n")
                            miniAuditPoints.forEachIndexed { index, point ->
                                append("${index + 1}. $point\n")
                            }
                            append("\nLead score: $leadScore/100\n")
                        },
                        logs = listOf(
                            "⚡ [Auto-Pilot] Discovered prospect via ${searchResult.providerUsed} on ${prospect.sourcePlatform}",
                            "⚡ [Auto-Pilot] Proposal drafted and queued for Auto-Pilot execution.",
                            "Selected outreach tone: ${outreachTone.name}",
                            "Dynamic audit quote: $${pricing.amount} (${pricing.segment})",
                            "Lead score: $leadScore/100"
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
                        estimatedValue = pricing.amount.toDouble(),
                        leadScore = leadScore,
                        pipelineStage = "Pitched",
                        lastContactedAt = System.currentTimeMillis(),
                        nextFollowUpAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)
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

    private fun computeLeadScore(websiteUrl: String, price: Double, email: String): Int {
        var score = 35
        if (websiteUrl.isNotBlank()) score += 20
        if (email.isNotBlank()) score += 15
        score += when {
            price >= 5000.0 -> 20
            price >= 2000.0 -> 15
            price >= 800.0 -> 10
            else -> 5
        }
        if (score > 100) score = 100
        return score
    }

    private suspend fun checkForStaleFollowUps(
        taskRepo: TaskRepository,
        settings: com.example.data.repository.AppSettings,
        emailService: BrevoEmailService
    ) {
        if (!settings.isBrevoConfigured) {
            return
        }

        val tasks = taskRepo.observeTasks().first()
        val staleTasks = tasks.filter { task ->
            if (task.clientEmail.isBlank()) return@filter false
            if (task.status !in listOf("PITCHED", "RUNNING")) return@filter false

            val lastUpdatedMs = task.updatedAt?.toDate()?.time ?: task.createdAt?.toDate()?.time ?: 0L
            val noReply = task.logs.none { log ->
                log.contains("reply", ignoreCase = true) ||
                    log.contains("replied", ignoreCase = true) ||
                    log.contains("response", ignoreCase = true)
            }
            System.currentTimeMillis() - lastUpdatedMs > TimeUnit.DAYS.toMillis(3) && noReply
        }

        for (task in staleTasks) {
            val followUpBody = buildFollowUpBody(task)
            val miniAuditPoints = emailService.generateMiniAuditPoints(task.client.ifBlank { task.clientEmail })
            val htmlContent = emailService.buildHtmlEmailBody(followUpBody, miniAuditPoints)

            val result = emailService.sendEmail(
                apiKey = settings.brevoApiKey,
                toEmail = task.clientEmail,
                subject = "Just checking in on my previous SEO audit email",
                bodyText = followUpBody,
                htmlContent = htmlContent
            )

            val followUpTask = Task(
                title = "Follow-up: ${task.client.ifBlank { "Client" }}",
                category = "Client Acquisition",
                workerName = "AutoPilot Follow-Up",
                workerRole = "Follow-up Specialist",
                status = "PENDING",
                priority = "MEDIUM",
                client = task.client.ifBlank { "Client" },
                clientEmail = task.clientEmail,
                estimatedValue = task.estimatedValue,
                details = followUpBody,
                logs = listOf(
                    "Follow-up task created automatically after 3+ days with no reply.",
                    "Follow-up email sent to ${task.clientEmail}"
                )
            )
            taskRepo.createTask(followUpTask)

            if (result.isSuccess) {
                taskRepo.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "PITCHED",
                    newLog = "Follow-up sent after 3+ days with no reply."
                )
            } else {
                taskRepo.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "RUNNING",
                    newLog = "Follow-up email failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                )
            }
        }
    }

    private fun buildFollowUpBody(task: Task): String {
        val clientName = task.client.ifBlank { "there" }
        return """
Hi $clientName,

Just checking in to see if you saw my previous email. I know things get busy, but I wanted to circle back because I think there may be value in a quick SEO audit for your site.

I’d be happy to share a brief rundown of the biggest technical and content gaps, and I can also suggest a few practical fixes that would improve rankings and conversions.

If this is helpful, I’d be glad to send over a quick example audit and a short action plan.

Best,
AutoFreelancer AI
        """.trimIndent()
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

        val targetEmail = task.clientEmail.trim().takeIf(BusinessEmailValidator::isValid)
            ?: searchResult.leads.firstNotNullOfOrNull { item ->
                BusinessEmailValidator.findIn("${item.snippet}\n${item.title}")
            }.orEmpty()

        if (settings.isBrevoConfigured) {
            if (targetEmail.isNotBlank()) {
                val siteUrl = targetWebsite.ifBlank { task.client }
                val miniAuditPoints = emailService.generateMiniAuditPoints(siteUrl)
                val pitchBody = buildString {
                    append("Hi ${task.client},\n\n")
                    append("I took a quick look at your website and wanted to share a simple SEO snapshot.\n\n")
                    append(deliverableContent)
                    append("\n\nHere is a quick free audit of your site...\n")
                    miniAuditPoints.forEachIndexed { index, point ->
                        append("${index + 1}. $point\n")
                    }
                    append("\nThanks for your time. I’d be happy to walk through the main opportunities with you.\n")
                }

                val htmlContent = emailService.buildHtmlEmailBody(pitchBody, miniAuditPoints)
                val emailRes = emailService.sendEmail(
                    apiKey = settings.brevoApiKey,
                    toEmail = targetEmail,
                    subject = "SEO Audit & Growth Proposal: ${task.title}",
                    bodyText = pitchBody,
                    htmlContent = htmlContent
                )
                if (emailRes.isSuccess) {
                    replyRateRepository.recordSentPitch(targetEmail, outreachTone)
                    LiveWorkerLogStore.record(task.workerName, "> Pitch sent; reply-rate event recorded.")
                    taskRepo.updateTaskStatus(
                        taskId = task.id,
                        newStatus = "PITCHED",
                        newLog = "Email sent successfully via Brevo"
                    )
                } else {
                    val err = emailRes.exceptionOrNull()?.message ?: "Unknown delivery error"
                    taskRepo.updateTaskStatus(
                        taskId = task.id,
                        newStatus = "RUNNING",
                        newLog = "Brevo delivery error: $err"
                    )
                }
            } else {
                taskRepo.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "RUNNING",
                    newLog = "Error: Client email is missing. Please provide client email to send proposal."
                )
            }
        }

        val crmClient = CrmClient(
            name = task.client,
            company = task.category,
            websiteUrl = searchResult.leads.firstOrNull()?.link.orEmpty(),
            email = targetEmail,
            status = "Pitched",
            leadSource = "Auto-Pilot Worker",
            pitchMessage = deliverableContent,
            estimatedValue = pricing.amount.toDouble(),
            leadScore = computeLeadScore(targetWebsite, pricing.amount, targetEmail),
            pipelineStage = "Pitched",
            lastContactedAt = System.currentTimeMillis(),
            nextFollowUpAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)
        )
        clientRepo.upsertClient(crmClient)
    }
}

object AutoPilotScheduler {
    private const val UNIQUE_WORK_NAME = "auto_pilot_work"
    private const val UNIQUE_IMMEDIATE_WORK_NAME = "auto_pilot_work_immediate"

    fun schedule(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            workManager.cancelUniqueWork(UNIQUE_IMMEDIATE_WORK_NAME)
            Log.i("AutoPilotScheduler", "Auto-Pilot work cancelled.")
            return
        }

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
    }
}
