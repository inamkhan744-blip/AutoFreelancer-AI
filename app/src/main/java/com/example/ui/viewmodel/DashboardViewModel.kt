package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CrmClient
import com.example.data.model.DealPipelineStage
import com.example.data.model.InboxEmail
import com.example.data.model.OutreachTone
import com.example.data.model.ProspectResult
import com.example.data.model.Task
import com.example.data.repository.AppSettings
import com.example.data.repository.ClientRepository
import com.example.data.repository.InboxRepository
import com.example.data.repository.OfflineAction
import com.example.data.repository.OfflineSyncQueueRepository
import com.example.data.repository.ReplyRateRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.data.service.AbTestingEngine
import com.example.data.service.AiCloserService
import com.example.data.service.AiOrchestrationService
import com.example.data.service.BrevoEmailService
import com.example.data.service.CompetitorSpyPrompt
import com.example.data.service.DynamicPricingEngine
import com.example.data.service.ElevenLabsVoiceService
import com.example.data.service.EmailAttachment
import com.example.data.service.FollowUpSequenceEngine
import com.example.data.service.GeneratedPitch
import com.example.data.service.GeneratedVoiceNote
import com.example.data.service.SearchExecutionResult
import com.example.data.service.SearchProspectService
import com.example.data.service.SeoAuditService
import com.example.data.service.SeoReportResult
import com.example.data.service.ImapEmailService
import com.example.data.service.ReportGeneratorService
import com.example.data.service.TwilioSmsService
import com.example.util.BusinessEmailValidator
import com.example.util.LiveWorkerLogStore
import com.example.util.ProspectDeduplicator
import com.example.worker.AutoPilotScheduler
import com.example.worker.ImapSyncScheduler
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface TasksUiState {
    object Loading : TasksUiState
    data class Success(val tasks: List<Task>) : TasksUiState
    data class Error(val message: String) : TasksUiState
}

data class DashboardMetrics(
    val totalTasks: Int = 0,
    val pendingCount: Int = 0,
    val activeWorkersCount: Int = 0,
    val completedCount: Int = 0,
    val totalEstimatedValue: Double = 0.0
)

data class WorkerNotification(
    val id: String = UUID.randomUUID().toString(),
    val workerName: String,
    val taskTitle: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class LeadDiscoveryProgress(
    val isRunning: Boolean = false,
    val stepMessage: String = "",
    val prospects: List<ProspectResult> = emptyList(),
    val generatedPitches: List<GeneratedPitch> = emptyList(),
    val error: String? = null
)

class DashboardViewModel(
    private val repository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val clientRepository: ClientRepository? = null,
    private val inboxRepository: InboxRepository? = null,
    private val searchService: SearchProspectService = SearchProspectService(),
    private val aiService: AiOrchestrationService = AiOrchestrationService(),
    private val emailService: BrevoEmailService = BrevoEmailService(),
    private val seoAuditService: SeoAuditService = SeoAuditService(),
    private val elevenLabsService: ElevenLabsVoiceService = ElevenLabsVoiceService(),
    private val closerService: AiCloserService = AiCloserService(),
    private val replyRateRepository: ReplyRateRepository = ReplyRateRepository(),
    private val pricingEngine: DynamicPricingEngine = DynamicPricingEngine(aiService),
    private val twilioService: TwilioSmsService = TwilioSmsService(),
    private val followUpEngine: FollowUpSequenceEngine = FollowUpSequenceEngine(),
    private val offlineSyncRepository: OfflineSyncQueueRepository? = null,
    private val abTestingEngine: AbTestingEngine? = null,
    private val imapService: ImapEmailService = ImapEmailService(),
    private val reportGeneratorService: ReportGeneratorService = ReportGeneratorService()
) : ViewModel() {

    val liveTerminalEntries = LiveWorkerLogStore.entries

    private val _currentScreen = MutableStateFlow("DASHBOARD") // "DASHBOARD", "INBOX", "CLIENTS", "DELIVERABLES", "SETTINGS", "CLIENT_PORTAL"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _selectedClientForPortal = MutableStateFlow<CrmClient?>(null)
    val selectedClientForPortal: StateFlow<CrmClient?> = _selectedClientForPortal.asStateFlow()

    val clients: StateFlow<List<CrmClient>> = (clientRepository?.observeClients() ?: MutableStateFlow(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val inboxEmails: StateFlow<List<InboxEmail>> = (inboxRepository?.observeInbox() ?: MutableStateFlow(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL") // ALL, PENDING, ACTIVE, COMPLETED, REJECTED
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _activeNotification = MutableStateFlow<WorkerNotification?>(null)
    val activeNotification: StateFlow<WorkerNotification?> = _activeNotification.asStateFlow()

    private val _selectedTaskForDetail = MutableStateFlow<Task?>(null)
    val selectedTaskForDetail: StateFlow<Task?> = _selectedTaskForDetail.asStateFlow()

    private val _rejectTargetTask = MutableStateFlow<Task?>(null)
    val rejectTargetTask: StateFlow<Task?> = _rejectTargetTask.asStateFlow()

    private val _showCreateTaskDialog = MutableStateFlow(false)
    val showCreateTaskDialog: StateFlow<Boolean> = _showCreateTaskDialog.asStateFlow()

    private val _isOperationLoading = MutableStateFlow(false)
    val isOperationLoading: StateFlow<Boolean> = _isOperationLoading.asStateFlow()

    private val _discoveryProgress = MutableStateFlow(LeadDiscoveryProgress())
    val discoveryProgress: StateFlow<LeadDiscoveryProgress> = _discoveryProgress.asStateFlow()

    fun dismissDiscoveryProgress() {
        _discoveryProgress.value = LeadDiscoveryProgress(isRunning = false)
    }

    val todayLeadCount: StateFlow<Int> = clients.map { list ->
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        list.count { it.createdAt >= startOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pipelineTotalValue: StateFlow<Double> = clients.map { list ->
        list.sumOf { it.estimatedValue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Gemini Models state
    private val _availableGeminiModels = MutableStateFlow<List<String>>(
        listOf("gemini-3.8-flash", "gemini-3.6-flash", "gemini-2.5-flash")
    )
    val availableGeminiModels: StateFlow<List<String>> = _availableGeminiModels.asStateFlow()

    private val _isFetchingGeminiModels = MutableStateFlow(false)
    val isFetchingGeminiModels: StateFlow<Boolean> = _isFetchingGeminiModels.asStateFlow()

    private val _geminiModelsMessage = MutableStateFlow<Pair<Boolean, String>?>(null)
    val geminiModelsMessage: StateFlow<Pair<Boolean, String>?> = _geminiModelsMessage.asStateFlow()

    // OpenRouter Models state
    private val _availableOpenRouterModels = MutableStateFlow<List<String>>(
        listOf(
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemma-4-26b-a4b-it:free",
            "mistralai/mistral-small-24b-instruct-2501:free"
        )
    )
    val availableOpenRouterModels: StateFlow<List<String>> = _availableOpenRouterModels.asStateFlow()

    private val _isFetchingOpenRouterModels = MutableStateFlow(false)
    val isFetchingOpenRouterModels: StateFlow<Boolean> = _isFetchingOpenRouterModels.asStateFlow()

    private val _openRouterModelsMessage = MutableStateFlow<Pair<Boolean, String>?>(null)
    val openRouterModelsMessage: StateFlow<Pair<Boolean, String>?> = _openRouterModelsMessage.asStateFlow()

    val tasksUiState: StateFlow<TasksUiState> = repository.observeTasks()
        .combine(_searchQuery) { tasks, query ->
            if (query.isBlank()) tasks
            else tasks.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.client.contains(query, ignoreCase = true) ||
                it.clientEmail.contains(query, ignoreCase = true) ||
                it.workerName.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }
        .combine(_statusFilter) { tasks, filter ->
            when (filter) {
                "PENDING" -> tasks.filter { it.status == "PENDING" }
                "ACTIVE" -> tasks.filter { it.status == "APPROVED" || it.status == "RUNNING" }
                "COMPLETED" -> tasks.filter { it.status == "COMPLETED" }
                "REJECTED" -> tasks.filter { it.status == "REJECTED" }
                else -> tasks
            }
        }
        .combine(repository.observeTasks()) { filteredList, _ ->
            TasksUiState.Success(filteredList)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TasksUiState.Loading
        )

    val metrics: StateFlow<DashboardMetrics> = repository.observeTasks()
        .combine(_searchQuery) { tasks, _ ->
            val pending = tasks.count { it.status == "PENDING" }
            val active = tasks.count { it.status == "APPROVED" || it.status == "RUNNING" }
            val completed = tasks.count { it.status == "COMPLETED" }
            val totalValue = tasks.filter { it.status != "REJECTED" }.sumOf { it.estimatedValue }
            DashboardMetrics(
                totalTasks = tasks.size,
                pendingCount = pending,
                activeWorkersCount = active,
                completedCount = completed,
                totalEstimatedValue = totalValue
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardMetrics()
        )

    val isAutoPilotActive: StateFlow<Boolean> = settings
        .map { it.autoPilotEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = settings.value.autoPilotEnabled
        )

    private val _settingsLoadError = MutableStateFlow<String?>(null)
    val settingsLoadError: StateFlow<String?> = _settingsLoadError.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { settingsRepository.loadCloudSettings() }
                .onFailure { _settingsLoadError.value = it.message ?: "Unable to load cloud settings." }
        }

        // Reactive Auto-Pilot engine: when auto-pilot is ON, automatically approve and execute pending tasks
        viewModelScope.launch {
            combine(settings, repository.observeTasks()) { currentSettings, tasks ->
                Pair(currentSettings, tasks)
            }.collect { (currentSettings, tasks) ->
                if (currentSettings.autoPilotEnabled && !_isOperationLoading.value) {
                    val pendingTasks = tasks.filter { it.status == "PENDING" }
                    if (pendingTasks.isNotEmpty()) {
                        for (task in pendingTasks) {
                            approveTask(task)
                        }
                    }
                }
            }
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun openClientPortal(client: CrmClient) {
        _selectedClientForPortal.value = client
        _currentScreen.value = "CLIENT_PORTAL"
    }

    fun closeClientPortal() {
        _selectedClientForPortal.value = null
        _currentScreen.value = "CLIENTS"
    }

    fun refreshTasks() {
        // Real tasks are observed reactively from repository
    }

    fun saveSettings(
        newSettings: AppSettings,
        context: Context? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val previous = settings.value
        viewModelScope.launch {
            try {
                settingsRepository.saveSettings(newSettings)
                if (context != null && previous.autoPilotEnabled != newSettings.autoPilotEnabled) {
                    AutoPilotScheduler.schedule(context, newSettings.autoPilotEnabled)
                }
                if (context != null) {
                    ImapSyncScheduler.schedule(context, newSettings.isImapConfigured)
                }
                val isGuest = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull() == null
                val msg = if (isGuest) {
                    "Settings & API keys saved locally to device."
                } else {
                    "Settings & API keys saved to your account."
                }
                onResult(true, msg)
            } catch (e: Exception) {
                onResult(false, e.message ?: "Unable to save settings.")
            }
        }
    }

    fun toggleAutoPilot(enabled: Boolean, context: Context? = null) {
        val updated = settings.value.copy(autoPilotEnabled = enabled)
        viewModelScope.launch {
            try {
                settingsRepository.saveSettings(updated)
                if (context != null) AutoPilotScheduler.schedule(context, enabled)
            } catch (e: Exception) {
                _settingsLoadError.value = e.message ?: "Unable to save Auto-Pilot settings."
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChanged(filter: String) {
        _statusFilter.value = filter
    }

    fun selectTaskForDetail(task: Task?) {
        _selectedTaskForDetail.value = task
    }

    fun openRejectDialog(task: Task) {
        _rejectTargetTask.value = task
    }

    fun closeRejectDialog() {
        _rejectTargetTask.value = null
    }

    fun openCreateTaskDialog() {
        _showCreateTaskDialog.value = true
    }

    fun closeCreateTaskDialog() {
        _showCreateTaskDialog.value = false
    }

    fun dismissNotification() {
        _activeNotification.value = null
    }

    fun fetchAvailableGeminiModels(apiKey: String) {
        viewModelScope.launch {
            _isFetchingGeminiModels.value = true
            _geminiModelsMessage.value = null
            val res = aiService.fetchAvailableGeminiModels(apiKey)
            _isFetchingGeminiModels.value = false
            if (res.isSuccess) {
                val models = res.getOrThrow()
                _availableGeminiModels.value = models
                _geminiModelsMessage.value = Pair(true, "Found ${models.size} available Gemini models")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Failed to query Gemini models"
                _geminiModelsMessage.value = Pair(false, err)
            }
        }
    }

    fun fetchAvailableOpenRouterModels(apiKey: String) {
        viewModelScope.launch {
            _isFetchingOpenRouterModels.value = true
            _openRouterModelsMessage.value = null
            val res = aiService.fetchAvailableOpenRouterModels(apiKey)
            _isFetchingOpenRouterModels.value = false
            if (res.isSuccess) {
                val models = res.getOrThrow()
                _availableOpenRouterModels.value = models
                _openRouterModelsMessage.value = Pair(true, "Found ${models.size} available OpenRouter models")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Failed to query OpenRouter models"
                _openRouterModelsMessage.value = Pair(false, err)
            }
        }
    }

    /**
     * Executes real worker logic on task approval:
     * 1. Updates task status to APPROVED -> RUNNING.
     * 2. Shows notification that AI worker has started.
     * 3. Queries live Search API (Serper, with Tavily backup) for real client leads.
     * 4. Passes search context to Google Gemini (with OpenRouter fallback) to generate a personalized outreach message and SEO audit deliverable.
     * 5. Saves real findings and generated content into task details, marks task COMPLETED.
     * 6. Automatically dispatches the generated proposal via Brevo to a valid business email when configured.
     */
    fun approveTask(task: Task) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val approveResult = repository.approveTask(task.id)
            _isOperationLoading.value = false

            if (approveResult.isSuccess) {
                _activeNotification.value = WorkerNotification(
                    workerName = task.workerName,
                    taskTitle = task.title
                )

                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "RUNNING",
                    newLog = "${task.workerName} started: Initiating real-time lead scouting and AI deliverable pipeline."
                )

                val currentConfig = settingsRepository.settingsFlow.value
                val serperKey = currentConfig.serperApiKey
                val tavilyKey = currentConfig.tavilyApiKey
                val geminiKey = currentConfig.geminiApiKey
                val openRouterKey = currentConfig.openRouterApiKey
                val geminiModel = currentConfig.geminiModel
                val openRouterModel = currentConfig.openRouterModel
                val brevoKey = currentConfig.brevoApiKey

                val query = deriveQueryForTask(task, currentConfig.searchQuery)
                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "RUNNING",
                    newLog = "Searching internet for client prospects: \"$query\""
                )

                val searchResult: SearchExecutionResult = searchService.searchWithFallback(
                    query = query,
                    serperKey = serperKey,
                    tavilyKey = tavilyKey
                )
                LiveWorkerLogStore.record(task.workerName, "> Search returned ${searchResult.leads.size} candidate leads.")

                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "RUNNING",
                    newLog = searchResult.note
                )

                searchResult.leads.take(3).forEachIndexed { index, lead ->
                    repository.updateTaskStatus(
                        taskId = task.id,
                        newStatus = "RUNNING",
                        newLog = "Lead #${index + 1} [${lead.sourcePlatform}]: ${lead.title} (${lead.link})"
                    )
                }

                if (geminiKey.isNotBlank() || openRouterKey.isNotBlank()) {
                    val outreachTone = replyRateRepository.bestPerformingTone()
                    val targetWebsite = searchResult.leads.firstOrNull()?.let(CompetitorSpyPrompt::websiteFor).orEmpty()
                    val pricing = pricingEngine.decide(
                        website = targetWebsite,
                        context = "${task.client}\n${task.title}\n${task.details}",
                        geminiApiKey = geminiKey,
                        openRouterApiKey = openRouterKey,
                        geminiModel = geminiModel,
                        openRouterModel = openRouterModel
                    )
                    val dynamicPaymentLink = pricingEngine.paymentLinkForAmount(currentConfig.paymentLinkUrl, pricing.amount)
                    LiveWorkerLogStore.record(task.workerName, "> Tone ${outreachTone.promptLabel}; quote $${pricing.amount} (${pricing.segment}).")
                    LiveWorkerLogStore.record(task.workerName, "> Running competitor comparison for ${targetWebsite.ifBlank { task.client }}...")
                    repository.updateTaskStatus(
                        taskId = task.id,
                        newStatus = "RUNNING",
                        newLog = "Generating ${outreachTone.promptLabel.lowercase()} deliverable; selected $${pricing.amount} quote."
                    )

                    val assignedVariant = abTestingEngine?.selectVariant() ?: "A"

                    val aiResult = aiService.generateTaskDeliverable(
                        geminiApiKey = geminiKey,
                        openRouterApiKey = openRouterKey,
                        geminiModel = geminiModel,
                        openRouterModel = openRouterModel,
                        taskTitle = task.title,
                        category = task.category,
                        client = task.client,
                        workerName = task.workerName,
                        workerRole = task.workerRole,
                        leads = searchResult.leads,
                        paymentLinkUrl = dynamicPaymentLink,
                        outreachTone = outreachTone,
                        targetWebsite = targetWebsite,
                        dynamicPrice = pricing.amount,
                        agencyName = currentConfig.agencyName,
                        calendarBookingUrl = currentConfig.bookingLinkUrl
                    )

                    if (aiResult.isSuccess) {
                        val generation = aiResult.getOrThrow()
                        val aiContent = generation.content
                        val providerUsed = generation.providerUsed

                        val leadsFormatted = if (searchResult.leads.isNotEmpty()) {
                            searchResult.leads.mapIndexed { idx, item ->
                                "${idx + 1}. [${item.sourcePlatform}] ${item.title}\n   URL: ${item.link}\n   Context: ${item.snippet}"
                            }.joinToString("\n\n")
                        } else {
                            "Direct client inquiry."
                        }

                        val deliverableSection = """
                        |========================================
                        |AI WORKER DELIVERABLE [A/B: $assignedVariant]
                        |========================================
                        |Engine: $providerUsed
                        |Agency: ${currentConfig.agencyName}
                        |Search Engine: ${searchResult.providerUsed}
                        |
                        |$aiContent
                        |
                        |========================================
                        |VERIFIED PROSPECT LEADS
                        |========================================
                        |$leadsFormatted
                        """.trimMargin()

                        repository.updateTaskExecution(
                            taskId = task.id,
                            newStatus = "COMPLETED",
                            appendDetails = deliverableSection,
                            newLog = "AI deliverable generated via $providerUsed (${aiContent.length} chars). Finalized and saved."
                        )

                        val targetEmail = task.clientEmail.trim().takeIf(BusinessEmailValidator::isValid)
                            ?: searchResult.leads.firstNotNullOfOrNull {
                                BusinessEmailValidator.findIn("${it.snippet}\n${it.title}")
                            }.orEmpty()

                        if (brevoKey.isNotBlank()) {
                            if (targetEmail.isNotBlank()) {
                                repository.updateTaskStatus(
                                    taskId = task.id,
                                    newStatus = "COMPLETED",
                                    newLog = "Dispatching proposal email via Brevo to $targetEmail (Variant $assignedVariant)..."
                                )
                                val subject = if (assignedVariant == "B") {
                                    "Actionable SEO Roadmap & Growth Opportunities: ${task.title}"
                                } else {
                                    "Quick Technical Observation Regarding ${task.title}"
                                }
                                val emailRes = emailService.sendEmail(
                                    apiKey = brevoKey,
                                    toEmail = targetEmail,
                                    subject = subject,
                                    bodyText = aiContent
                                )
                                if (emailRes.isSuccess) {
                                    abTestingEngine?.recordSend(assignedVariant)
                                    repository.updateTaskStatus(
                                        taskId = task.id,
                                        newStatus = "COMPLETED",
                                        newLog = "Email sent successfully via Brevo"
                                    )
                                    replyRateRepository.recordSentPitch(targetEmail, outreachTone)
                                    LiveWorkerLogStore.record(task.workerName, "> Pitch sent; reply-rate event recorded.")
                                } else {
                                    val err = emailRes.exceptionOrNull()?.message ?: "Unknown delivery error"
                                    repository.updateTaskStatus(
                                        taskId = task.id,
                                        newStatus = "COMPLETED",
                                        newLog = "Brevo auto-dispatch error: $err"
                                    )
                                }
                            } else {
                                repository.updateTaskStatus(
                                    taskId = task.id,
                                    newStatus = "COMPLETED",
                                    newLog = "Error: Client email is missing. Please provide client email to send proposal."
                                )
                            }
                        } else {
                            if (targetEmail.isBlank()) {
                                repository.updateTaskStatus(
                                    taskId = task.id,
                                    newStatus = "COMPLETED",
                                    newLog = "Error: Client email is missing. Please provide client email to send proposal."
                                )
                            } else {
                                repository.updateTaskStatus(
                                    taskId = task.id,
                                    newStatus = "COMPLETED",
                                    newLog = "Proposal ready. Add a Brevo API Key in Settings to enable automated email dispatch."
                                )
                            }
                        }

                        val crmLeadUrl = searchResult.leads.firstOrNull()?.link.orEmpty()
                        val existingTasks = repository.observeTasks().first()
                            .filterNot { it.id == task.id }
                        val knownProspects = ProspectDeduplicator(
                            tasks = existingTasks,
                            clients = clientRepository?.observeClients()?.first().orEmpty()
                        )
                        if (knownProspects.contains(crmLeadUrl, targetEmail)) {
                            LiveWorkerLogStore.record(
                                task.workerName,
                                "> Skipped CRM creation because this prospect already exists."
                            )
                        } else {
                            val crmClient = CrmClient(
                                name = task.client,
                                company = task.category,
                                websiteUrl = crmLeadUrl,
                                email = targetEmail,
                                status = "Pitched",
                                pipelineStage = DealPipelineStage.PITCHED,
                                sequenceStage = FollowUpSequenceEngine.FollowUpStage.DAY_1.name,
                                abVariant = assignedVariant,
                                calendarBookingUrl = currentConfig.bookingLinkUrl,
                                leadSource = "AI Worker",
                                pitchMessage = aiContent,
                                estimatedValue = pricing.amount.toDouble()
                            )
                            clientRepository?.upsertClient(crmClient)
                        }
                    } else {
                        val err = aiResult.exceptionOrNull()?.message ?: "AI generation failed"
                        repository.updateTaskStatus(
                            taskId = task.id,
                            newStatus = "RUNNING",
                            newLog = "AI Error: $err"
                        )
                        completeWithDiscoveredLeads(task, searchResult, "AI generation failed: $err")
                    }
                } else {
                    completeWithDiscoveredLeads(
                        task,
                        searchResult,
                        "Notice: Gemini API Key / OpenRouter Key not configured in Settings. Saved discovered search leads without AI synthesis."
                    )
                }
            }
        }
    }

    fun sendTaskEmail(
        task: Task,
        recipientEmail: String,
        voiceNote: GeneratedVoiceNote? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val currentConfig = settingsRepository.settingsFlow.value
            val brevoKey = currentConfig.brevoApiKey

            if (brevoKey.isBlank()) {
                onResult(false, "Please configure your Brevo API key in Settings first.")
                return@launch
            }

            val target = recipientEmail.trim().ifBlank { task.clientEmail.trim() }
            if (target.isBlank()) {
                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = task.status,
                    newLog = "Error: Client email is missing. Please provide client email to send proposal."
                )
                onResult(false, "Error: Client email is missing. Please provide client email to send proposal.")
                return@launch
            }

            val contentToSend = if (task.details.isNotBlank()) {
                task.details
            } else {
                "SEO Audit Proposal for ${task.title}\n\nClient: ${task.client}\nCategory: ${task.category}"
            }

            repository.updateTaskStatus(
                taskId = task.id,
                newStatus = task.status,
                newLog = "Manually sending proposal email via Brevo to $target..."
            )

            val attachments = if (voiceNote != null) {
                listOf(EmailAttachment(filename = voiceNote.fileName, contentBase64 = voiceNote.base64Content))
            } else {
                emptyList()
            }

            val result = emailService.sendEmail(
                apiKey = brevoKey,
                toEmail = target,
                subject = "SEO Audit & Growth Proposal: ${task.title}",
                bodyText = contentToSend,
                attachments = attachments
            )

            if (result.isSuccess) {
                val sent = result.getOrThrow()
                replyRateRepository.recordSentPitch(target, toneForTask(task))
                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = task.status,
                    newLog = "Email sent successfully via Brevo"
                )
                onResult(true, "Email sent successfully via Brevo (ID: ${sent.emailId.take(8)}...)")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to deliver email"
                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = task.status,
                    newLog = "Brevo delivery failed: $err"
                )
                onResult(false, "Failed to send email: $err")
            }
        }
    }

    fun testBrevoKey(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = emailService.testConnection(key)
            if (result.isSuccess) {
                onResult(true, result.getOrThrow())
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Brevo connection error")
            }
        }
    }

    private suspend fun completeWithDiscoveredLeads(
        task: Task,
        searchResult: SearchExecutionResult,
        statusNote: String
    ) {
        val leadsFormatted = if (searchResult.leads.isNotEmpty()) {
            searchResult.leads.mapIndexed { idx, item ->
                "${idx + 1}. [${item.sourcePlatform}] ${item.title}\n   URL: ${item.link}\n   Context: ${item.snippet}"
            }.joinToString("\n\n")
        } else {
            "No web results available."
        }

        val deliverableSection = """
        |========================================
        |LIVE DISCOVERED LEADS
        |========================================
        |Search Engine: ${searchResult.providerUsed}
        |
        |$leadsFormatted
        |
        |========================================
        |AI WORKER NOTE
        |========================================
        |$statusNote
        |To generate automated AI proposals, enter your Gemini API Key in Settings.
        """.trimMargin()

        repository.updateTaskExecution(
            taskId = task.id,
            newStatus = "COMPLETED",
            appendDetails = deliverableSection,
            newLog = statusNote
        )
    }

    private fun deriveQueryForTask(task: Task, defaultQuery: String): String {
        val titleLower = task.title.lowercase()
        return when {
            titleLower.contains("linkedin") -> "looking for SEO audit OR \"need an SEO audit\" site:linkedin.com"
            titleLower.contains("reddit") -> "looking for SEO audit OR \"need an SEO audit\" site:reddit.com"
            titleLower.contains("twitter") || titleLower.contains(" x ") -> "looking for SEO audit site:x.com OR site:twitter.com"
            titleLower.contains("shopify") || titleLower.contains("ecommerce") -> "Shopify store SEO audit OR \"need SEO audit\""
            titleLower.contains("competitor") -> "competitor keyword research SEO audit"
            titleLower.contains("audit") -> "looking for SEO audit OR \"need someone to do an SEO audit\""
            defaultQuery.isNotBlank() -> defaultQuery
            else -> "looking for SEO audit"
        }
    }

    fun rejectTask(taskId: String, reason: String) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            repository.rejectTask(taskId, reason)
            _isOperationLoading.value = false
            _rejectTargetTask.value = null
            if (_selectedTaskForDetail.value?.id == taskId) {
                _selectedTaskForDetail.value = null
            }
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            repository.updateTaskStatus(
                taskId = taskId,
                newStatus = "COMPLETED",
                newLog = "AI Worker completed all deliverables successfully and archived project."
            )
        }
    }

    fun testGeminiKey(key: String, model: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val targetModel = model ?: settings.value.geminiModel
            val result = aiService.testGeminiConnection(key, targetModel)
            if (result.isSuccess) {
                onResult(true, result.getOrThrow())
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Gemini connection error")
            }
        }
    }

    fun testOpenRouterKey(key: String, model: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val targetModel = model ?: settings.value.openRouterModel
            val result = aiService.testOpenRouterConnection(key, targetModel)
            if (result.isSuccess) {
                onResult(true, result.getOrThrow())
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "OpenRouter connection error")
            }
        }
    }

    fun testSerperKey(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = searchService.testSerper(key)
            if (result.isSuccess) {
                onResult(true, result.getOrThrow())
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Serper test failed")
            }
        }
    }

    fun testTavilyKey(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = searchService.testTavily(key)
            if (result.isSuccess) {
                onResult(true, result.getOrThrow())
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Tavily test failed")
            }
        }
    }

    fun testBraveKey(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = searchService.testBrave(key)
            if (result.isSuccess) {
                onResult(true, result.getOrThrow())
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Brave Search test failed")
            }
        }
    }

    fun testGoogleCseKey(key: String, cx: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = searchService.testGoogleCse(key, cx)
            if (result.isSuccess) {
                onResult(true, result.getOrThrow())
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Google Custom Search test failed")
            }
        }
    }

    fun createCustomTask(
        title: String,
        category: String,
        workerName: String,
        workerRole: String,
        client: String,
        clientEmail: String = "",
        estimatedValue: Double,
        details: String,
        priority: String = "MEDIUM"
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val newTask = Task(
                title = title.ifBlank { "Client Task" },
                category = category.ifBlank { "Client Acquisition" },
                workerName = workerName.ifBlank { "AutoWorker-AI" },
                workerRole = workerRole.ifBlank { "Freelance AI Specialist" },
                status = "PENDING",
                priority = priority,
                client = client.ifBlank { "Client Prospect" },
                clientEmail = clientEmail.trim(),
                estimatedValue = estimatedValue.coerceAtLeast(0.0),
                details = details.ifBlank { "Autonomous freelancing task created by director." },
                logs = listOf("Task queued by director in pending review state.")
            )
            repository.createTask(newTask)
            _isOperationLoading.value = false
            _showCreateTaskDialog.value = false
        }
    }

    fun runLeadDiscovery(customQuery: String? = null) {
        viewModelScope.launch {
            val currentConfig = settings.value

            // 1. Daily Targets & Quota Enforcement
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val todayStart = calendar.timeInMillis
            val allExistingClients = clientRepository?.observeClients()?.first().orEmpty()
            val todayLeadsCount = allExistingClients.count { it.createdAt >= todayStart }

            if (todayLeadsCount >= currentConfig.dailyLeadTarget) {
                val quotaMessage = "Daily target achieved ($todayLeadsCount/${currentConfig.dailyLeadTarget} leads). Sleeping until tomorrow."
                LiveWorkerLogStore.record("Apollo-Scout", "> $quotaMessage")
                _discoveryProgress.value = LeadDiscoveryProgress(
                    isRunning = false,
                    stepMessage = quotaMessage,
                    prospects = emptyList()
                )
                return@launch
            }

            // 2. Dynamic Search Queries (Seeking clients who need help, not agencies)
            val dynamicQueries = listOf(
                "\"need SEO audit\" \"@gmail.com\"",
                "site:reddit.com \"hiring SEO expert\" OR \"need SEO help\"",
                "\"looking for SEO freelancer\" \"contact me\"",
                "\"my website traffic dropped\" \"need help\"",
                "\"need someone to do SEO\" site:linkedin.com"
            )
            val queryToRun = customQuery ?: dynamicQueries.random()
            LiveWorkerLogStore.record("Apollo-Scout", "> Selected targeted query: $queryToRun")

            _discoveryProgress.value = LeadDiscoveryProgress(
                isRunning = true,
                stepMessage = "Querying Serper, Brave, and Google CSE for active prospects...",
                error = null
            )

            // 3. Multi-Engine Search Execution
            val searchResult = searchService.searchWithFallback(
                query = queryToRun,
                serperKey = currentConfig.serperApiKey,
                tavilyKey = currentConfig.tavilyApiKey,
                braveKey = currentConfig.braveSearchApiKey,
                googleApiKey = currentConfig.googleCustomSearchApiKey,
                googleCseCx = currentConfig.googleCseCx
            )

            val knownProspects = ProspectDeduplicator(
                tasks = repository.observeTasks().first(),
                clients = allExistingClients
            )

            // 4. The Email Hunter: Deep scraping & validation
            val qualifiedProspects = mutableListOf<Pair<ProspectResult, String>>()
            for (prospect in searchResult.leads) {
                // Pre-check: Skip SEO agencies (like SmartSites) to only hunt prospective clients
                if (SearchProspectService.isAgency(prospect.title, prospect.snippet, prospect.link)) {
                    LiveWorkerLogStore.record(
                        "Apollo-Scout",
                        "> [Agency Filter] Skipped agency: ${prospect.title.take(30)} (${prospect.link})"
                    )
                    continue
                }

                // Step A: Inspect title and snippet for direct emails
                var discoveredEmail = BusinessEmailValidator.findIn("${prospect.snippet}\n${prospect.title}")
                    ?: searchService.findValidEmail("${prospect.snippet}\n${prospect.title}")

                // Step B: Deep Scraping website and contact pages
                if (discoveredEmail.isNullOrBlank()) {
                    val targetSite = CompetitorSpyPrompt.websiteFor(prospect).ifBlank { prospect.link }
                    LiveWorkerLogStore.record("Apollo-Scout", "> [Email Hunter] Deep scraping contact pages on $targetSite...")
                    discoveredEmail = searchService.extractEmailFromWebsite(targetSite)
                }

                if (discoveredEmail.isNullOrBlank()) {
                    LiveWorkerLogStore.record(
                        "Apollo-Scout",
                        "> [Email Hunter] No valid business email found for ${prospect.title.take(30)}. Skipping lead."
                    )
                    continue
                }

                if (knownProspects.contains(prospect.link, discoveredEmail)) {
                    LiveWorkerLogStore.record(
                        "Apollo-Scout",
                        "> Skipping existing prospect: ${prospect.link}"
                    )
                    continue
                }

                knownProspects.remember(prospect.link, discoveredEmail)
                qualifiedProspects.add(prospect to discoveredEmail)
            }

            val prospects = qualifiedProspects.map { it.first }
            if (prospects.isEmpty()) {
                _discoveryProgress.value = LeadDiscoveryProgress(
                    isRunning = false,
                    stepMessage = "No prospects with valid business email addresses were found.",
                    prospects = emptyList()
                )
                return@launch
            }

            _discoveryProgress.value = LeadDiscoveryProgress(
                isRunning = true,
                stepMessage = "Found ${prospects.size} leads via ${searchResult.providerUsed}. Generating AI pitch proposals...",
                prospects = prospects
            )

            val generatedPitches = mutableListOf<GeneratedPitch>()
            val remainingQuota = (currentConfig.dailyLeadTarget - todayLeadsCount).coerceAtLeast(1)
            val targetProspects = qualifiedProspects.take(remainingQuota.coerceAtMost(3))

            for ((index, qualifiedProspect) in targetProspects.withIndex()) {
                val (prospect, discoveredEmail) = qualifiedProspect
                val outreachTone = replyRateRepository.bestPerformingTone()
                val targetWebsite = CompetitorSpyPrompt.websiteFor(prospect)
                LiveWorkerLogStore.record("Apollo-Scout", "> Analyzing business scale for a dynamic quote...")
                val pricing = pricingEngine.decide(
                    website = targetWebsite,
                    context = "${prospect.title}\n${prospect.snippet}",
                    geminiApiKey = currentConfig.geminiApiKey,
                    openRouterApiKey = currentConfig.openRouterApiKey,
                    geminiModel = currentConfig.geminiModel,
                    openRouterModel = currentConfig.openRouterModel
                )
                val dynamicPaymentLink = pricingEngine.paymentLinkForAmount(currentConfig.paymentLinkUrl, pricing.amount)
                LiveWorkerLogStore.record("Apollo-Scout", "> Reply-rate winner: ${outreachTone.promptLabel}; selected $${pricing.amount} ${pricing.segment.lowercase()} tier.")
                _discoveryProgress.value = _discoveryProgress.value.copy(
                    stepMessage = "Formulating proposal ${index + 1}/${targetProspects.size} for ${prospect.sourcePlatform}..."
                )

                val prompt = "Write a persuasive client pitch for this SEO audit lead.\nTitle: ${prospect.title}\nContext: ${prospect.snippet}\nWebsite: ${targetWebsite.ifBlank { "not provided" }}\nSource: ${prospect.link}\n\n${CompetitorSpyPrompt.instructionsFor(prospect)}\n\nQuote exactly $${pricing.amount} using a ${outreachTone.promptLabel.lowercase()} tone."
                val systemPrompt = "You are Apollo-Scout at ${currentConfig.agencyName}. Compare the client's site with three plausible direct competitors, use evidence carefully, and write in a ${outreachTone.promptLabel.lowercase()} tone. Never claim live research you did not perform."
                LiveWorkerLogStore.record("Apollo-Scout", "> Comparing the prospect with three likely competitors...")

                var aiText = "Saw your request for an SEO audit. We can deliver a comprehensive technical analysis."
                var engineUsed = "Manual Template"

                if (currentConfig.geminiApiKey.isNotBlank() || currentConfig.openRouterApiKey.isNotBlank()) {
                    val aiRes = aiService.generateContentWithFallback(
                        geminiApiKey = currentConfig.geminiApiKey,
                        openRouterApiKey = currentConfig.openRouterApiKey,
                        geminiModel = currentConfig.geminiModel,
                        openRouterModel = currentConfig.openRouterModel,
                        systemPrompt = systemPrompt,
                        userPrompt = prompt,
                        maxTokens = 400
                    )
                    if (aiRes.isSuccess) {
                        val gen = aiRes.getOrThrow()
                        aiText = gen.content
                        engineUsed = gen.providerUsed
                    }
                }

                if (dynamicPaymentLink.isNotBlank()) {
                    aiText += "\n\nThe ${outreachTone.promptLabel.lowercase()} audit package is $${pricing.amount}. To get the full SEO audit report, complete payment here: $dynamicPaymentLink"
                }

                val pitch = GeneratedPitch(
                    prospectNameOrCompany = prospect.sourcePlatform,
                    subjectLine = "Personalized SEO Audit Proposal for ${prospect.title.take(30)}",
                    personalizedPitchMessage = aiText,
                    auditHooks = listOf("Technical SEO Audit", "Competitive Gap Analysis"),
                    suggestedRateOrValue = pricing.amount.toDouble(),
                    recommendedCta = "Offer a complimentary 4-minute Loom video audit",
                    outreachTone = outreachTone
                )

                val task = Task(
                    title = "Pitching SEO Audit to ${prospect.title.take(40)}",
                    category = "Client Acquisition",
                    workerName = "Apollo-Scout",
                    workerRole = "Lead Generation Specialist",
                    status = "PENDING",
                    priority = "HIGH",
                    client = "${prospect.sourcePlatform} • Lead",
                    clientEmail = discoveredEmail,
                    estimatedValue = pricing.amount.toDouble(),
                    details = buildString {
                        append("Subject: ${pitch.subjectLine}\n\n")
                        append("${pitch.personalizedPitchMessage}\n\n")
                        append("Recommended CTA: ${pitch.recommendedCta}\n")
                        append("Discovered Lead URL: ${prospect.link}\n")
                        append("Lead Snippet: ${prospect.snippet}\n")
                    },
                    logs = listOf(
                        "Discovered via ${searchResult.providerUsed} on ${prospect.sourcePlatform}",
                        "Proposal drafted via $engineUsed",
                        "Selected outreach tone: ${outreachTone.name}",
                        "Dynamic audit quote: $${pricing.amount} (${pricing.segment})"
                    )
                )
                val taskResult = repository.createTask(task)
                if (taskResult.isFailure) {
                    val error = taskResult.exceptionOrNull()?.message ?: "Unable to save discovered lead task."
                    LiveWorkerLogStore.record("Apollo-Scout", "> Failed to save task for ${prospect.link}: $error")
                    continue
                }
                generatedPitches.add(pitch)

                val crmLead = CrmClient(
                    name = prospect.title.take(40),
                    company = prospect.sourcePlatform,
                    websiteUrl = prospect.link,
                    email = discoveredEmail,
                    status = "Pitched",
                    pipelineStage = DealPipelineStage.PITCHED,
                    leadSource = prospect.sourcePlatform,
                    pitchMessage = pitch.personalizedPitchMessage,
                    estimatedValue = pricing.amount.toDouble()
                )
                clientRepository?.upsertClient(crmLead)
            }

            _discoveryProgress.value = LeadDiscoveryProgress(
                isRunning = false,
                stepMessage = "Completed! Generated ${generatedPitches.size} live pitches ready for Approval.",
                prospects = prospects,
                generatedPitches = generatedPitches
            )
        }
    }

    // ==========================================
    // CRM (Client Database) & Deliverables Logic
    // ==========================================

    fun updateClientStatus(clientId: String, newStatus: String) {
        viewModelScope.launch {
            val normalized = DealPipelineStage.normalize(newStatus)
            clientRepository?.updateClientStatus(clientId, normalized)
            offlineSyncRepository?.enqueueAction(
                OfflineAction(
                    type = "CLIENT_STAGE_CHANGE",
                    entityId = clientId,
                    payloadJson = JSONObject().apply {
                        put("pipelineStage", normalized)
                        put("status", normalized)
                        put("updatedAt", System.currentTimeMillis())
                    }.toString()
                )
            )

            // Autonomous Fulfillment: Generate HTML Report and deliver via Brevo when marked Won or Paid
            if (normalized.equals(DealPipelineStage.WON, ignoreCase = true) ||
                normalized.equals("Won", ignoreCase = true) ||
                normalized.equals("Paid", ignoreCase = true)
            ) {
                val targetClient = clients.value.find { it.id == clientId }
                if (targetClient != null) {
                    val currentConfig = settings.value
                    val htmlReport = reportGeneratorService.generateHtmlReport(
                        client = targetClient,
                        agencyName = currentConfig.agencyName.ifBlank { "Autonomous AI Sales Agency" },
                        bookingLink = currentConfig.bookingLinkUrl,
                        customThankYou = "Thank you for partnering with us! We have initiated your deliverables."
                    )
                    clientRepository?.updateClientReport(clientId, htmlReport)
                    LiveWorkerLogStore.record("ReportGenerator", "Generated HTML SEO Report for ${targetClient.name} (${targetClient.websiteUrl})")

                    if (currentConfig.isBrevoConfigured && targetClient.email.isNotBlank()) {
                        emailService.sendEmail(
                            apiKey = currentConfig.brevoApiKey,
                            toEmail = targetClient.email,
                            subject = "Your SEO Audit & Strategy Report - ${targetClient.company.ifBlank { targetClient.websiteUrl }}",
                            bodyText = "Please view your delivered report in HTML.",
                            htmlContent = htmlReport
                        )
                        LiveWorkerLogStore.record("ReportGenerator", "Dispatched HTML Deliverable via Brevo to ${targetClient.email}")
                    }
                }
            }
        }
    }

    fun updateClientPipelineStage(clientId: String, newStage: String) {
        updateClientStatus(clientId, newStage)
    }

    fun updateClientReport(clientId: String, report: String) {
        viewModelScope.launch {
            clientRepository?.updateClientReport(clientId, report)
        }
    }

    fun upsertClient(client: CrmClient) {
        viewModelScope.launch {
            clientRepository?.upsertClient(client)
            offlineSyncRepository?.enqueueAction(
                OfflineAction(
                    type = "CRM_CREATE",
                    entityId = client.id,
                    payloadJson = client.toJson().toString()
                )
            )
        }
    }

    fun generateSeoReportForClient(
        client: CrmClient,
        onResult: (Boolean, SeoReportResult?, String) -> Unit
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val config = settings.value
            val result = seoAuditService.generate5PointAuditReport(
                rawUrl = client.websiteUrl,
                geminiApiKey = config.geminiApiKey,
                openRouterApiKey = config.openRouterApiKey,
                geminiModel = config.geminiModel,
                openRouterModel = config.openRouterModel,
                clientName = client.name,
                paymentLinkUrl = config.paymentLinkUrl
            )
            _isOperationLoading.value = false
            if (result.isSuccess) {
                val reportResult = result.getOrThrow()
                val updatedClient = client.copy(
                    seoReport = reportResult.generatedReport,
                    updatedAt = System.currentTimeMillis()
                )
                clientRepository?.upsertClient(updatedClient)
                onResult(true, reportResult, "Report generated successfully via ${reportResult.engineUsed}")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to generate report"
                onResult(false, null, err)
            }
        }
    }

    fun generateSeoReportForUrl(
        url: String,
        clientName: String = "Client",
        clientEmail: String = "",
        onResult: (Boolean, SeoReportResult?, String) -> Unit
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val config = settings.value
            val result = seoAuditService.generate5PointAuditReport(
                rawUrl = url,
                geminiApiKey = config.geminiApiKey,
                openRouterApiKey = config.openRouterApiKey,
                geminiModel = config.geminiModel,
                openRouterModel = config.openRouterModel,
                clientName = clientName,
                paymentLinkUrl = config.paymentLinkUrl
            )
            _isOperationLoading.value = false
            if (result.isSuccess) {
                val reportResult = result.getOrThrow()
                val existingClient = clients.value.find { it.websiteUrl.equals(url.trim(), ignoreCase = true) }
                if (existingClient != null) {
                    val updated = existingClient.copy(
                        seoReport = reportResult.generatedReport,
                        updatedAt = System.currentTimeMillis()
                    )
                    clientRepository?.upsertClient(updated)
                } else if (url.isNotBlank()) {
                    val newClient = CrmClient(
                        name = clientName,
                        email = clientEmail,
                        websiteUrl = url.trim(),
                        status = "Pitched",
                        pipelineStage = DealPipelineStage.PITCHED,
                        seoReport = reportResult.generatedReport
                    )
                    clientRepository?.upsertClient(newClient)
                }
                onResult(true, reportResult, "Report generated successfully via ${reportResult.engineUsed}")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to generate report"
                onResult(false, null, err)
            }
        }
    }

    fun generateClientProposalDeliverable(
        client: CrmClient,
        quoteAmount: Int,
        onResult: (Boolean, String, String) -> Unit
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val config = settings.value
            val res = aiService.generateClientProposal(
                geminiApiKey = config.geminiApiKey,
                openRouterApiKey = config.openRouterApiKey,
                geminiModel = config.geminiModel,
                openRouterModel = config.openRouterModel,
                clientName = client.name,
                websiteUrl = client.websiteUrl,
                quotePrice = quoteAmount,
                agencyName = config.agencyName,
                calendarBookingUrl = config.bookingLinkUrl,
                paymentLinkUrl = config.paymentLinkUrl
            )
            _isOperationLoading.value = false
            if (res.isSuccess) {
                val proposalContent = res.getOrThrow().content
                val updatedClient = client.copy(
                    proposalHtml = proposalContent,
                    proposalPrice = quoteAmount.toDouble(),
                    pipelineStage = DealPipelineStage.PROPOSAL_SENT,
                    updatedAt = System.currentTimeMillis()
                )
                clientRepository?.upsertClient(updatedClient)
                onResult(true, proposalContent, "Proposal generated for ${client.name} ($$quoteAmount USD)")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Failed to generate proposal"
                onResult(false, "", err)
            }
        }
    }

    fun requestPaymentFromClient(
        client: CrmClient,
        amount: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val config = settings.value
            val targetEmail = client.email.trim()
            if (targetEmail.isBlank()) {
                onResult(false, "Client email is required to request payment.")
                return@launch
            }
            val paymentUrl = if (config.paymentLinkUrl.isNotBlank()) {
                pricingEngine.paymentLinkForAmount(config.paymentLinkUrl, amount)
            } else {
                "https://buy.stripe.com/invoice_quote_$amount"
            }

            val subject = "Invoice & Secure Payment Link: ${client.name} SEO Sprint"
            val body = """
                Hi ${client.name},

                Thank you for reviewing our proposal with ${config.agencyName}.

                We have prepared your custom deliverables and sprint kickoff.
                Total Investment: $$amount USD

                You can complete your payment securely via the link below:
                $paymentUrl

                Once confirmed, our team will deliver the full technical audit, remediation plan, and begin monitoring immediately.

                Best regards,
                ${config.agencyName} Team
                ${config.customEmailFooter}
            """.trimIndent()

            val html = emailService.buildHtmlEmailBody(body, emptyList())
            val sendResult = emailService.sendEmail(
                apiKey = config.brevoApiKey,
                toEmail = targetEmail,
                subject = subject,
                bodyText = body,
                htmlContent = html
            )

            val updatedClient = client.copy(
                paymentStatus = "REQUESTED ($$amount)",
                paymentLink = paymentUrl,
                pipelineStage = DealPipelineStage.PROPOSAL_SENT,
                updatedAt = System.currentTimeMillis()
            )
            clientRepository?.upsertClient(updatedClient)

            if (sendResult.isSuccess) {
                onResult(true, "Payment request for $$amount dispatched via Brevo to $targetEmail")
            } else {
                onResult(true, "Payment link recorded ($paymentUrl). (Email dispatch note: ${sendResult.exceptionOrNull()?.message ?: "Brevo not configured"})")
            }
        }
    }

    fun sendTwilioMessage(
        client: CrmClient,
        messageText: String,
        isWhatsApp: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val config = settings.value
            val recipientPhone = client.phone.ifBlank { "+15551234567" }
            val res = twilioService.sendMessage(
                accountSid = config.twilioAccountSid,
                authToken = config.twilioAuthToken,
                fromNumber = config.twilioPhoneNumber,
                toNumber = recipientPhone,
                body = messageText,
                isWhatsApp = isWhatsApp
            )
            if (res.isSuccess) {
                val r = res.getOrThrow()
                val note = if (r.isSimulated) {
                    "[Simulated ${r.channel}] Message queued to $recipientPhone: ${r.preview}"
                } else {
                    "[Twilio ${r.channel}] Dispatched to $recipientPhone (SID: ${r.messageSid})"
                }
                LiveWorkerLogStore.record("Twilio Outreach", "> $note")
                onResult(true, note)
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Twilio message failed")
            }
        }
    }

    fun sendFollowUpSequenceNow(
        client: CrmClient,
        stage: FollowUpSequenceEngine.FollowUpStage,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val config = settings.value
            val targetEmail = client.email.trim()
            if (targetEmail.isBlank()) {
                onResult(false, "Client email is required for sequence outreach.")
                return@launch
            }

            val template = followUpEngine.generateFollowUpTemplate(
                stage = stage,
                clientName = client.name.ifBlank { "there" },
                websiteUrl = client.websiteUrl,
                bookingLink = config.bookingLinkUrl,
                agencyName = config.agencyName,
                customFooter = config.customEmailFooter
            )

            val htmlBody = emailService.buildHtmlEmailBody(template.body, emptyList())
            val res = emailService.sendEmail(
                apiKey = config.brevoApiKey,
                toEmail = targetEmail,
                subject = template.subject,
                bodyText = template.body,
                htmlContent = htmlBody
            )

            val updatedClient = client.copy(
                sequenceStage = stage.name,
                pipelineStage = DealPipelineStage.FOLLOW_UP,
                lastContactedAt = System.currentTimeMillis()
            )
            clientRepository?.upsertClient(updatedClient)

            if (res.isSuccess) {
                abTestingEngine?.recordSend(client.abVariant.ifBlank { "A" })
                onResult(true, "Sent ${stage.name} follow-up to $targetEmail via Brevo")
            } else {
                onResult(false, "Brevo delivery failed: ${res.exceptionOrNull()?.message ?: "Unknown error"}")
            }
        }
    }

    fun syncOfflineQueue(onResult: (Boolean, Int, String) -> Unit) {
        viewModelScope.launch {
            val repo = offlineSyncRepository
            if (repo == null) {
                onResult(true, 0, "Offline queue is up to date.")
                return@launch
            }
            val firestore = runCatching { com.google.firebase.firestore.FirebaseFirestore.getInstance() }.getOrNull()
            val userId = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
            val res = repo.flushSyncToFirestore(firestore, userId)
            if (res.isSuccess) {
                val count = res.getOrThrow()
                onResult(true, count, "Synced $count pending actions to Firestore.")
            } else {
                onResult(false, 0, res.exceptionOrNull()?.message ?: "Sync failed")
            }
        }
    }

    fun sendEmailToClient(
        toEmail: String,
        subject: String,
        bodyText: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val brevoKey = settings.value.brevoApiKey
            if (brevoKey.isBlank()) {
                onResult(false, "Please configure your Brevo API key in Settings first.")
                return@launch
            }
            if (toEmail.isBlank()) {
                onResult(false, "Error: Client email is missing. Please provide client email to send proposal.")
                return@launch
            }
            val res = emailService.sendEmail(
                apiKey = brevoKey,
                toEmail = toEmail,
                subject = subject,
                bodyText = bodyText
            )
            if (res.isSuccess) {
                onResult(true, "Email sent successfully via Brevo")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Failed to deliver email"
                onResult(false, "Brevo delivery error: $err")
            }
        }
    }

    // ==========================================
    // Feature 1: Jarvis Voice Command Actions
    // ==========================================

    fun approveAllPendingTasks(onResult: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val allTasks = repository.observeTasks().first()
            val pendingTasks = allTasks.filter { it.status == "PENDING" }
            for (task in pendingTasks) {
                approveTask(task)
            }
            onResult?.invoke(pendingTasks.size)
        }
    }

    // ==========================================
    // Feature 2: ElevenLabs AI Voice Notes
    // ==========================================

    fun generateVoiceNoteForText(
        context: Context,
        text: String,
        outputFileName: String? = null,
        onResult: (Boolean, GeneratedVoiceNote?, String) -> Unit
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val config = settings.value
            val res = elevenLabsService.textToSpeech(
                context = context,
                apiKey = config.elevenLabsApiKey,
                text = text,
                voiceId = config.elevenLabsVoiceId,
                outputFileName = outputFileName
            )
            _isOperationLoading.value = false
            if (res.isSuccess) {
                val voiceNote = res.getOrThrow()
                onResult(true, voiceNote, "AI Voice note generated (${voiceNote.fileSizeKb} KB)")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Voice note generation failed"
                onResult(false, null, err)
            }
        }
    }

    fun testElevenLabsKey(apiKey: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = elevenLabsService.testConnection(apiKey)
            if (res.isSuccess) {
                onResult(true, res.getOrThrow())
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "ElevenLabs authentication failed")
            }
        }
    }

    fun sendEmailWithVoiceNote(
        toEmail: String,
        subject: String,
        bodyText: String,
        voiceNote: GeneratedVoiceNote? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val brevoKey = settings.value.brevoApiKey
            if (brevoKey.isBlank()) {
                onResult(false, "Please configure your Brevo API key in Settings first.")
                return@launch
            }
            if (toEmail.isBlank()) {
                onResult(false, "Error: Client email is missing. Please provide client email to send proposal.")
                return@launch
            }

            val attachments = if (voiceNote != null) {
                listOf(EmailAttachment(filename = voiceNote.fileName, contentBase64 = voiceNote.base64Content))
            } else {
                emptyList()
            }

            val res = emailService.sendEmail(
                apiKey = brevoKey,
                toEmail = toEmail,
                subject = subject,
                bodyText = bodyText,
                attachments = attachments
            )
            if (res.isSuccess) {
                onResult(true, "Email sent successfully via Brevo" + if (voiceNote != null) " with AI Voice Note" else "")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Failed to deliver email"
                onResult(false, "Brevo delivery error: $err")
            }
        }
    }

    // ==========================================
    // Feature 3: The AI Closer (Auto-Reply to Inbound Emails)
    // ==========================================

    fun draftObjectionReply(
        senderName: String,
        senderEmail: String,
        subject: String,
        incomingMessage: String,
        onResult: (Boolean, InboxEmail?, String) -> Unit
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val config = settings.value
            val res = closerService.analyzeAndDraftReply(
                senderName = senderName,
                senderEmail = senderEmail,
                subject = subject,
                incomingMessage = incomingMessage,
                geminiApiKey = config.geminiApiKey,
                openRouterApiKey = config.openRouterApiKey,
                geminiModel = config.geminiModel,
                openRouterModel = config.openRouterModel,
                paymentLinkUrl = config.paymentLinkUrl,
                bookingLinkUrl = config.bookingLinkUrl,
                agencyName = config.agencyName
            )
            _isOperationLoading.value = false
            if (res.isSuccess) {
                val result = res.getOrThrow()
                val newEmail = InboxEmail(
                    senderName = senderName,
                    senderEmail = senderEmail,
                    subject = subject,
                    incomingMessage = incomingMessage,
                    objectionCategory = result.objectionCategory,
                    draftedReply = result.draftedReply,
                    status = "Awaiting Review",
                    reasoningAnalysis = result.reasoningAnalysis,
                    reasoningStrategy = result.reasoningStrategy,
                    nextBestAction = result.nextBestAction
                )
                inboxRepository?.upsertMessage(newEmail)
                onResult(true, newEmail, "Objection analyzed by Gemini (${result.objectionCategory})")
            } else {
                val err = res.exceptionOrNull()?.message ?: "AI Closer failed"
                onResult(false, null, err)
            }
        }
    }

    fun approveAndSendInboxReply(
        email: InboxEmail,
        voiceNote: GeneratedVoiceNote? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val brevoKey = settings.value.brevoApiKey
            if (brevoKey.isBlank()) {
                onResult(false, "Please configure your Brevo API key in Settings first.")
                return@launch
            }
            if (email.senderEmail.isBlank()) {
                onResult(false, "Error: Client email is missing. Please provide client email to send proposal.")
                return@launch
            }

            val attachments = if (voiceNote != null) {
                listOf(EmailAttachment(filename = voiceNote.fileName, contentBase64 = voiceNote.base64Content))
            } else {
                emptyList()
            }

            val res = emailService.sendEmail(
                apiKey = brevoKey,
                toEmail = email.senderEmail,
                subject = "Re: ${email.subject}",
                bodyText = email.draftedReply,
                attachments = attachments
            )

            if (res.isSuccess) {
                inboxRepository?.markApprovedAndSent(email.id)
                onResult(true, "Email sent successfully via Brevo")
            } else {
                val err = res.exceptionOrNull()?.message ?: "Delivery failed"
                onResult(false, "Brevo delivery error: $err")
            }
        }
    }

    fun updateInboxDraft(messageId: String, newDraft: String) {
        viewModelScope.launch {
            inboxRepository?.updateDraftedReply(messageId, newDraft)
        }
    }

    fun dismissInboxEmail(messageId: String) {
        viewModelScope.launch {
            inboxRepository?.dismissMessage(messageId)
        }
    }

    fun simulateIncomingClientObjection(scenarioType: String = "Budget") {
        viewModelScope.launch {
            val scenario = when (scenarioType.lowercase()) {
                "budget" -> Triple(
                    "Marcus Vance",
                    "marcus@vancestore.com",
                    "Your audit proposal looks high - our budget is only $200 right now. Can you discount?"
                )
                "timing" -> Triple(
                    "Elena Rostova",
                    "elena@growthcloud.io",
                    "We're slammed with Q4 inventory right now. Can we talk about this next quarter?"
                )
                "trust" -> Triple(
                    "David Miller",
                    "david@millerlaw.com",
                    "We hired an SEO freelancer last year who delivered zero ranking improvements. How do I know this will work?"
                )
                else -> Triple(
                    "Sarah Jenkins",
                    "sarah@ecoboutique.com",
                    "We already have an internal content writer. What value does a technical audit add?"
                )
            }

            draftObjectionReply(
                senderName = scenario.first,
                senderEmail = scenario.second,
                subject = "Re: SEO Audit Proposal",
                incomingMessage = scenario.third
            ) { _, _, _ -> }
        }
    }

    fun fetchWebhookInboundEmails(onResult: (Boolean, Int, String) -> Unit) {
        viewModelScope.launch {
            val webhookUrl = settings.value.inboundEmailWebhookUrl
            if (webhookUrl.isBlank()) {
                onResult(false, 0, "No Webhook URL configured in Settings.")
                return@launch
            }
            _isOperationLoading.value = true
            val res = closerService.fetchInboundEmailsFromWebhook(webhookUrl)
            _isOperationLoading.value = false
            if (res.isSuccess) {
                val list = res.getOrThrow()
                for (item in list) {
                    replyRateRepository.recordInboundReply(item.senderEmail)
                    LiveWorkerLogStore.record("AI Closer", "> Matched inbound reply to a prior pitch using hashed recipient data.")
                    draftObjectionReply(
                        senderName = item.senderName,
                        senderEmail = item.senderEmail,
                        subject = item.subject,
                        incomingMessage = item.incomingMessage
                    ) { _, _, _ -> }
                }
                onResult(true, list.size, "Fetched ${list.size} emails from Webhook")
            } else {
                onResult(false, 0, res.exceptionOrNull()?.message ?: "Webhook fetch failed")
            }
        }
    }

    private fun toneForTask(task: Task): OutreachTone {
        val savedTone = task.logs.firstNotNullOfOrNull { log ->
            log.substringAfter("Selected outreach tone:", "")
                .trim()
                .takeIf { it.isNotBlank() }
        }
        return OutreachTone.values().firstOrNull { it.name.equals(savedTone, ignoreCase = true) }
            ?: OutreachTone.PROFESSIONAL
    }

    // ==========================================
    // IMAP Inbox Reply Sync
    // ==========================================

    fun testImapConnection(
        email: String,
        appPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            try {
                val res = imapService.testConnection(email.trim(), appPassword.trim())
                if (res.isSuccess) {
                    onResult(true, "IMAP connected successfully! Gmail inbox is reachable.")
                } else {
                    val err = res.exceptionOrNull()?.message ?: "IMAP connection failed"
                    onResult(false, err)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "IMAP connection error")
            } finally {
                _isOperationLoading.value = false
            }
        }
    }

    fun syncInboxReplies(onComplete: ((Int) -> Unit)? = null) {
        val config = settings.value
        if (!config.isImapConfigured) {
            LiveWorkerLogStore.record("IMAP Sync", "IMAP is not configured in Settings. Skipping.")
            onComplete?.invoke(0)
            return
        }

        viewModelScope.launch {
            LiveWorkerLogStore.record("IMAP Sync", "Connecting to IMAP inbox for ${config.imapEmail}...")
            val result = imapService.fetchUnseenReplies(config.imapEmail, config.imapAppPassword)
            if (result.isFailure) {
                val err = result.exceptionOrNull()?.message ?: "Unknown IMAP error"
                LiveWorkerLogStore.record("IMAP Sync", "Sync error: $err")
                onComplete?.invoke(0)
                return@launch
            }

            val emails = result.getOrNull().orEmpty()
            LiveWorkerLogStore.record("IMAP Sync", "Fetched ${emails.size} new replies from inbox.")
            var processedCount = 0

            for (email in emails) {
                val draftResult = if (config.isAiConfigured) {
                    runCatching {
                        closerService.analyzeAndDraftReply(
                            senderName = email.senderName,
                            senderEmail = email.senderEmail,
                            subject = email.subject,
                            incomingMessage = email.body,
                            geminiApiKey = config.geminiApiKey,
                            openRouterApiKey = config.openRouterApiKey,
                            geminiModel = config.geminiModel,
                            openRouterModel = config.openRouterModel,
                            paymentLinkUrl = config.paymentLinkUrl,
                            bookingLinkUrl = config.bookingLinkUrl,
                            agencyName = config.agencyName
                        ).getOrNull()
                    }.getOrNull()
                } else null

                val inboxEmail = InboxEmail(
                    senderName = email.senderName,
                    senderEmail = email.senderEmail,
                    subject = email.subject,
                    incomingMessage = email.body,
                    objectionCategory = draftResult?.objectionCategory ?: "General Inquiry",
                    draftedReply = draftResult?.draftedReply ?: "Thanks for getting back to us! Would you be open for a brief 10-minute chat this week? Booking link: ${config.bookingLinkUrl}",
                    reasoningAnalysis = draftResult?.reasoningAnalysis.orEmpty(),
                    reasoningStrategy = draftResult?.reasoningStrategy.orEmpty(),
                    nextBestAction = draftResult?.nextBestAction.orEmpty(),
                    receivedAt = email.receivedAt
                )
                inboxRepository?.upsertMessage(inboxEmail)

                // Match sender to CRM client and elevate to INTERESTED
                val existingClient = clients.value.find { c ->
                    (c.email.isNotBlank() && c.email.equals(email.senderEmail, ignoreCase = true)) ||
                    (email.senderEmail.contains(c.websiteUrl.replace("https://", "").replace("http://", "").take(10), ignoreCase = true))
                }
                if (existingClient != null) {
                    clientRepository?.updateClientStatus(existingClient.id, DealPipelineStage.INTERESTED)
                    LiveWorkerLogStore.record("IMAP Sync", "Elevated client ${existingClient.name} to INTERESTED based on email reply.")
                }

                processedCount++
            }

            LiveWorkerLogStore.record("IMAP Sync", "Completed inbox sync. Processed $processedCount emails.")
            onComplete?.invoke(processedCount)
        }
    }

    // ==========================================
    // Data Backup / Restore (JSON)
    // ==========================================

    fun exportDataToJson(context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentTasks = tasksUiState.value.let {
                    if (it is TasksUiState.Success) it.tasks else repository.observeTasks().first()
                }
                val currentClients = clients.value

                val rootJson = JSONObject().apply {
                    put("exportedAt", System.currentTimeMillis())
                    put("version", 1)
                    put("tasks", JSONArray().apply {
                        currentTasks.forEach { put(it.toJson()) }
                    })
                    put("clients", JSONArray().apply {
                        currentClients.forEach { put(it.toJson()) }
                    })
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "autofreelancer_backup_$timestamp.json"

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null && !downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val targetFile = File(downloadsDir, fileName)
                targetFile.writeText(rootJson.toString(2))

                onResult(true, "Data exported successfully to Downloads/$fileName (${currentTasks.size} tasks, ${currentClients.size} clients)")
            } catch (e: Exception) {
                // Fallback to internal app storage
                try {
                    val fallbackFile = File(context.filesDir, "autofreelancer_backup.json")
                    val currentTasks = repository.observeTasks().first()
                    val currentClients = clients.value
                    val rootJson = JSONObject().apply {
                        put("exportedAt", System.currentTimeMillis())
                        put("tasks", JSONArray().apply { currentTasks.forEach { put(it.toJson()) } })
                        put("clients", JSONArray().apply { currentClients.forEach { put(it.toJson()) } })
                    }
                    fallbackFile.writeText(rootJson.toString(2))
                    onResult(true, "Data exported to internal app storage (${fallbackFile.name}).")
                } catch (fallbackEx: Exception) {
                    onResult(false, "Export failed: ${e.message}")
                }
            }
        }
    }

    fun importDataFromJson(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: throw IllegalArgumentException("Could not read backup file")

                val rootJson = JSONObject(content)
                val tasksJson = rootJson.optJSONArray("tasks") ?: JSONArray()
                val clientsJson = rootJson.optJSONArray("clients") ?: JSONArray()

                val restoredTasks = mutableListOf<Task>()
                for (i in 0 until tasksJson.length()) {
                    restoredTasks.add(Task.fromJson(tasksJson.getJSONObject(i)))
                }

                val restoredClients = mutableListOf<CrmClient>()
                for (i in 0 until clientsJson.length()) {
                    restoredClients.add(CrmClient.fromJson(clientsJson.getJSONObject(i)))
                }

                if (restoredTasks.isNotEmpty()) {
                    repository.restoreTasks(restoredTasks)
                }
                if (restoredClients.isNotEmpty()) {
                    clientRepository?.restoreClients(restoredClients)
                }

                onResult(true, "Imported ${restoredTasks.size} tasks and ${restoredClients.size} clients successfully!")
            } catch (e: Exception) {
                onResult(false, "Failed to import data: ${e.message}")
            }
        }
    }
}
