package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.model.CrmClient
import com.example.data.model.DealPipelineStage
import com.example.data.model.OutreachTone
import com.example.data.model.ProspectResult
import com.example.data.model.Task
import com.example.data.repository.ClientRepository
import com.example.data.repository.ReplyRateRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.data.service.AbTestingEngine
import com.example.data.service.AiOrchestrationService
import com.example.data.service.BrevoEmailService
import com.example.data.service.DynamicPricingEngine
import com.example.data.service.FollowUpSequenceEngine
import com.example.data.service.SearchProspectService
import com.example.util.BusinessEmailValidator
import com.example.util.LiveWorkerLogStore
import com.example.util.ProspectDeduplicator
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class AutoPilotWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val tag = "AutoPilotWorker"

    override suspend fun doWork(): Result {
        Log.i(tag, "AutoPilotWorker started execution...")
        LiveWorkerLogStore.record("Auto-Pilot", "Worker cycle initiated.")

        val settingsRepo = SettingsRepository(context)
        val settings = settingsRepo.settingsFlow.value

        if (!settings.autoPilotEnabled) {
            Log.i(tag, "Auto-Pilot is disabled in settings. Skipping cycle.")
            LiveWorkerLogStore.record("Auto-Pilot", "Skipped: Auto-Pilot mode is toggled OFF in Settings.")
            return Result.success()
        }

        return try {
            val taskRepo = TaskRepository(context)
            val clientRepo = ClientRepository(context)
            val aiService = AiOrchestrationService()
            val searchService = SearchProspectService()
            val emailService = BrevoEmailService()
            val replyRateRepository = ReplyRateRepository()
            val pricingEngine = DynamicPricingEngine(aiService)
            val abTestingEngine = AbTestingEngine(context)
            val followUpEngine = FollowUpSequenceEngine()

            val currentTasks = taskRepo.observeTasks().first()
            val pendingTasks = currentTasks.filter { it.status == "PENDING" }

            for (task in pendingTasks) {
                executeTaskAutonomously(
                    task = task,
                    settingsRepo = settingsRepo,
                    taskRepo = taskRepo,
                    clientRepo = clientRepo,
                    aiService = aiService,
                    replyRateRepository = replyRateRepository,
                    pricingEngine = pricingEngine,
                    emailService = emailService,
                    abTestingEngine = abTestingEngine
                )
            }

            checkForStaleFollowUps(taskRepo, clientRepo, settings, emailService, abTestingEngine, followUpEngine)

            val canRunDiscovery = settings.isSearchConfigured || settings.isAiConfigured
            if (pendingTasks.isEmpty() && canRunDiscovery) {
                Log.i(tag, "Running autonomous lead discovery in background...")
                LiveWorkerLogStore.record("Apollo-Scout", "> Scraping Reddit, LinkedIn, and web search results...")

                val searchResult = searchService.searchWithFallback(
                    query = "need SEO help site:reddit.com OR site:linkedin.com",
                    serperKey = settings.serperApiKey,
                    tavilyKey = settings.tavilyApiKey
                )

                LiveWorkerLogStore.record("Apollo-Scout", "> Discovery provider used: ${searchResult.providerUsed}")

                val existingClients = clientRepo.observeClients().first()
                val existingTasks = taskRepo.observeTasks().first()
                val deduplicator = ProspectDeduplicator(tasks = existingTasks, clients = existingClients)

                var newLeadsCount = 0
                for (prospect in searchResult.leads) {
                    val targetWebsite = extractWebsiteUrl(prospect.snippet)
                    val discoveredEmail = BusinessEmailValidator.findIn(prospect.snippet).orEmpty()

                    if (deduplicator.contains(
                            url = targetWebsite.ifBlank { prospect.link },
                            email = discoveredEmail
                        )
                    ) {
                        LiveWorkerLogStore.record(
                            "Apollo-Scout",
                            "> Skipped duplicate lead: ${prospect.title.take(30)}..."
                        )
                        continue
                    }

                    deduplicator.remember(url = targetWebsite.ifBlank { prospect.link }, email = discoveredEmail)

                    val outreachTone = replyRateRepository.bestPerformingTone()
                    val pricing = pricingEngine.decide(
                        website = targetWebsite.ifBlank { prospect.link },
                        context = prospect.snippet,
                        geminiApiKey = settings.geminiApiKey,
                        openRouterApiKey = settings.openRouterApiKey,
                        geminiModel = settings.geminiModel,
                        openRouterModel = settings.openRouterModel
                    )

                    val siteUrl = targetWebsite.ifBlank { prospect.link }
                    val miniAuditPoints = emailService.generateMiniAuditPoints(siteUrl)
                    val leadScore = computeLeadScore(targetWebsite, pricing.amount.toDouble(), discoveredEmail)
                    val assignedVariant = abTestingEngine.selectVariant()

                    val autoTask = Task(
                        title = "Auto-Pilot: Outreach to ${prospect.title.take(35)}",
                        category = "Client Acquisition",
                        workerName = "Apollo-Scout",
                        workerRole = "Lead Acquisition Scout",
                        status = "PENDING",
                        priority = "HIGH",
                        client = prospect.title.take(30),
                        clientEmail = discoveredEmail,
                        details = prospect.snippet,
                        estimatedValue = pricing.amount.toDouble()
                    )

                    val createdId = taskRepo.createTask(autoTask).getOrNull()
                    if (createdId != null) {
                        newLeadsCount++
                        LiveWorkerLogStore.record("Apollo-Scout", "> Enqueued autonomous task for: ${prospect.title.take(40)}")

                        val pitchBody = "Hi ${prospect.title.take(25)},\n\n" +
                            "I noticed your interest in technical optimization on ${prospect.sourcePlatform}.\n\n" +
                            miniAuditPoints.joinToString("\n") + "\n\n" +
                            "Let me know if you would like us to resolve these items for you.\n\n" +
                            "Best regards,\n${settings.agencyName}"

                        val autoClient = CrmClient(
                            name = prospect.title.take(40),
                            company = prospect.sourcePlatform,
                            websiteUrl = targetWebsite.ifBlank { prospect.link },
                            email = discoveredEmail,
                            status = "Pitched",
                            leadSource = "Auto-Pilot Discovery",
                            pitchMessage = pitchBody,
                            estimatedValue = pricing.amount.toDouble(),
                            leadScore = leadScore,
                            pipelineStage = DealPipelineStage.PITCHED,
                            sequenceStage = FollowUpSequenceEngine.FollowUpStage.DAY_1.name,
                            abVariant = assignedVariant,
                            calendarBookingUrl = settings.bookingLinkUrl,
                            lastContactedAt = System.currentTimeMillis(),
                            nextFollowUpAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)
                        )
                        clientRepo.upsertClient(autoClient)

                        // Immediately execute the new task
                        val savedTask = autoTask.copy(id = createdId)
                        executeTaskAutonomously(
                            task = savedTask,
                            settingsRepo = settingsRepo,
                            taskRepo = taskRepo,
                            clientRepo = clientRepo,
                            aiService = aiService,
                            replyRateRepository = replyRateRepository,
                            pricingEngine = pricingEngine,
                            emailService = emailService,
                            abTestingEngine = abTestingEngine
                        )
                    }
                }
                LiveWorkerLogStore.record("Apollo-Scout", "> Cycle finished: $newLeadsCount new leads enqueued and executed.")
            }

            Log.i(tag, "AutoPilotWorker completed cycle successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "AutoPilotWorker cycle encountered an error: ${e.message}", e)
            LiveWorkerLogStore.record("Auto-Pilot", "Cycle error: ${e.message}")
            Result.retry()
        }
    }

    private fun computeLeadScore(websiteUrl: String, price: Double, email: String): Int {
        var score = 40
        if (websiteUrl.isNotBlank()) score += 20
        if (email.isNotBlank()) score += 25
        if (price >= 900.0) score += 15
        return score.coerceIn(10, 100)
    }

    private suspend fun checkForStaleFollowUps(
        taskRepo: TaskRepository,
        clientRepo: ClientRepository,
        settings: com.example.data.repository.AppSettings,
        emailService: BrevoEmailService,
        abTestingEngine: AbTestingEngine,
        followUpEngine: FollowUpSequenceEngine
    ) {
        if (!settings.isBrevoConfigured) {
            return
        }

        // Multi-Stage Automated Follow-Up Cadence on CRM Clients
        try {
            val crmClients = clientRepo.observeClients().first()
            val now = System.currentTimeMillis()

            for (client in crmClients) {
                if (client.email.isBlank()) continue
                val normalizedStage = DealPipelineStage.normalize(client.pipelineStage)
                if (normalizedStage in listOf(DealPipelineStage.WON, DealPipelineStage.LOST, DealPipelineStage.CALL_BOOKED)) {
                    continue
                }

                val currentSeq = FollowUpSequenceEngine.FollowUpStage.fromString(client.sequenceStage)
                val nextSeq = followUpEngine.determineNextFollowUp(currentSeq, client.lastContactedAt, now) ?: continue

                val template = followUpEngine.generateFollowUpTemplate(
                    stage = nextSeq,
                    clientName = client.name.ifBlank { "there" },
                    websiteUrl = client.websiteUrl,
                    bookingLink = settings.bookingLinkUrl,
                    agencyName = settings.agencyName,
                    customFooter = settings.customEmailFooter
                )

                val htmlBody = emailService.buildHtmlEmailBody(template.body, emptyList())
                val emailResult = emailService.sendEmail(
                    apiKey = settings.brevoApiKey,
                    toEmail = client.email,
                    subject = template.subject,
                    bodyText = template.body,
                    htmlContent = htmlBody
                )

                if (emailResult.isSuccess) {
                    val variantToRecord = client.abVariant.ifBlank { "A" }
                    abTestingEngine.recordSend(variantToRecord)
                    LiveWorkerLogStore.record("AutoPilot Follow-Up", "> Sent ${nextSeq.name} follow-up to ${client.email}")

                    val updatedClient = client.copy(
                        sequenceStage = nextSeq.name,
                        pipelineStage = DealPipelineStage.FOLLOW_UP,
                        lastContactedAt = now,
                        nextFollowUpAt = now + TimeUnit.DAYS.toMillis(nextSeq.delayDays.toLong())
                    )
                    clientRepo.upsertClient(updatedClient)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "CRM multi-stage follow-up cadence error: ${e.message}")
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
        emailService: BrevoEmailService,
        abTestingEngine: AbTestingEngine
    ) {
        val settings = settingsRepo.settingsFlow.value
        taskRepo.updateTaskStatus(
            taskId = task.id,
            newStatus = "RUNNING",
            newLog = "Auto-pilot: Executing task '${task.title}' autonomously..."
        )
        LiveWorkerLogStore.record(task.workerName, "Auto-pilot: Executing task '${task.title}' autonomously...")

        var targetEmail = task.clientEmail.trim()
        var targetWebsite = extractWebsiteUrl(task.details)

        val outreachTone = replyRateRepository.bestPerformingTone()
        val pricing = pricingEngine.decide(
            website = targetWebsite.ifBlank { task.title },
            context = task.details,
            geminiApiKey = settings.geminiApiKey,
            openRouterApiKey = settings.openRouterApiKey,
            geminiModel = settings.geminiModel,
            openRouterModel = settings.openRouterModel
        )
        val dynamicPaymentLink = pricingEngine.paymentLinkForAmount(settings.paymentLinkUrl, pricing.amount)
        LiveWorkerLogStore.record(task.workerName, "> Applying ${outreachTone.promptLabel} tone and $${pricing.amount} quote.")

        val assignedVariant = abTestingEngine.selectVariant()

        val aiResult = aiService.generateTaskDeliverable(
            geminiApiKey = settings.geminiApiKey,
            openRouterApiKey = settings.openRouterApiKey,
            geminiModel = settings.geminiModel,
            openRouterModel = settings.openRouterModel,
            taskTitle = task.title,
            category = task.category,
            client = task.client,
            workerName = task.workerName.ifBlank { "Apollo-Scout" },
            workerRole = task.workerRole.ifBlank { "Growth Consultant" },
            leads = emptyList(),
            paymentLinkUrl = dynamicPaymentLink,
            outreachTone = outreachTone,
            targetWebsite = targetWebsite,
            dynamicPrice = pricing.amount,
            agencyName = settings.agencyName,
            calendarBookingUrl = settings.bookingLinkUrl
        )

        if (aiResult.isSuccess) {
            val deliverableContent = aiResult.getOrThrow().content

            taskRepo.updateTaskStatus(
                taskId = task.id,
                newStatus = "COMPLETED",
                newLog = "Task completed autonomously. Deliverable prepared."
            )
            LiveWorkerLogStore.record(task.workerName, "> Task completed autonomously. Deliverable prepared.")

            if (targetEmail.isBlank()) {
                val extracted = BusinessEmailValidator.findIn(deliverableContent).orEmpty()
                if (extracted.isNotBlank()) {
                    targetEmail = extracted
                }
            }

            if (targetWebsite.isBlank()) {
                targetWebsite = extractWebsiteUrl(deliverableContent)
            }

            if (settings.isBrevoConfigured && targetEmail.isNotBlank()) {
                LiveWorkerLogStore.record(task.workerName, "> Auto-pilot: Dispatching pitch to $targetEmail via Brevo...")
                val websiteForAudit = if (targetWebsite.isNotBlank()) targetWebsite else "your website"
                val miniAuditPoints = emailService.generateMiniAuditPoints(websiteForAudit)
                val htmlContent = emailService.buildHtmlEmailBody(deliverableContent, miniAuditPoints)

                val subjectLine = if (assignedVariant == "VARIANT_B") {
                    "Growth audit & technical roadmap for ${task.client.ifBlank { "your brand" }}"
                } else {
                    "Quick SEO audit notes for ${task.client.ifBlank { "your team" }}"
                }

                val emailRes = emailService.sendEmail(
                    apiKey = settings.brevoApiKey,
                    toEmail = targetEmail,
                    subject = subjectLine,
                    bodyText = deliverableContent,
                    htmlContent = htmlContent
                )
                if (emailRes.isSuccess) {
                    abTestingEngine.recordSend(assignedVariant)
                    replyRateRepository.recordSentPitch(targetEmail, outreachTone)
                    LiveWorkerLogStore.record(task.workerName, "> Pitch sent (Variant $assignedVariant); reply-rate event recorded.")
                    taskRepo.updateTaskStatus(
                        taskId = task.id,
                        newStatus = "PITCHED",
                        newLog = "Pitch sent to $targetEmail via Brevo."
                    )
                } else {
                    val errMsg = emailRes.exceptionOrNull()?.message ?: "Unknown Brevo error"
                    LiveWorkerLogStore.record(task.workerName, "> Email dispatch failed: $errMsg")
                }
            } else if (settings.isBrevoConfigured && targetEmail.isBlank()) {
                LiveWorkerLogStore.record(task.workerName, "> Notice: Lead email not found. Task completed without sending email.")
            }

            val autoCrmClient = CrmClient(
                name = task.client.ifBlank { task.title.take(30) },
                company = task.category,
                websiteUrl = targetWebsite,
                email = targetEmail,
                status = "Pitched",
                leadSource = "Auto-Pilot Worker",
                pitchMessage = deliverableContent,
                estimatedValue = pricing.amount.toDouble(),
                leadScore = computeLeadScore(targetWebsite, pricing.amount.toDouble(), targetEmail),
                pipelineStage = DealPipelineStage.PITCHED,
                sequenceStage = FollowUpSequenceEngine.FollowUpStage.DAY_1.name,
                abVariant = assignedVariant,
                calendarBookingUrl = settings.bookingLinkUrl,
                lastContactedAt = System.currentTimeMillis(),
                nextFollowUpAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)
            )
            clientRepo.upsertClient(autoCrmClient)
        } else {
            val errorMsg = aiResult.exceptionOrNull()?.message ?: "Unknown AI error"
            taskRepo.updateTaskStatus(
                taskId = task.id,
                newStatus = "FAILED",
                newLog = "Auto-pilot generation failed: $errorMsg"
            )
            LiveWorkerLogStore.record(task.workerName, "> Auto-pilot generation failed: $errorMsg")
        }
    }

    private fun extractWebsiteUrl(text: String): String {
        val urlRegex = Regex("""https?://[a-zA-Z0-9.\-_]+(?:\.[a-zA-Z]{2,})+(?:/[^\s]*)?""")
        val match = urlRegex.find(text)
        return match?.value ?: ""
    }
}
