package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ProspectResult
import com.example.data.model.Task
import com.example.data.repository.AppSettings
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.data.service.AiOrchestrationService
import com.example.data.service.GeneratedPitch
import com.example.data.service.ResendEmailService
import com.example.data.service.SearchExecutionResult
import com.example.data.service.SearchProspectService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val searchService: SearchProspectService = SearchProspectService(),
    private val aiService: AiOrchestrationService = AiOrchestrationService(),
    private val emailService: ResendEmailService = ResendEmailService()
) : ViewModel() {

    private val _currentScreen = MutableStateFlow("DASHBOARD") // "DASHBOARD" or "SETTINGS"
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

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

    init {
        // App starts with a clean, empty state ("No tasks yet" and $0.00 revenue)
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun refreshTasks() {
        // Real tasks are observed reactively from repository
    }

    fun saveSettings(newSettings: AppSettings) {
        settingsRepository.saveSettings(newSettings)
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
     * 6. Automatically dispatches the generated proposal via Resend API to the client's email if available and Resend is configured.
     */
    fun approveTask(task: Task) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val approveResult = repository.approveTask(task.id)
            _isOperationLoading.value = false

            if (approveResult.isSuccess) {
                // Show notification that AI worker has started
                _activeNotification.value = WorkerNotification(
                    workerName = task.workerName,
                    taskTitle = task.title
                )

                // Worker lifecycle state: Running
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
                val resendKey = currentConfig.resendApiKey

                // Derive search query tailored to this task
                val query = deriveQueryForTask(task, currentConfig.searchQuery)
                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "RUNNING",
                    newLog = "Searching internet for client prospects: \"$query\""
                )

                // Execute Search API (Serper primary, Tavily backup)
                val searchResult: SearchExecutionResult = searchService.searchWithFallback(
                    query = query,
                    serperKey = serperKey,
                    tavilyKey = tavilyKey
                )

                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "RUNNING",
                    newLog = searchResult.note
                )

                // Log discovered lead summaries
                searchResult.leads.take(3).forEachIndexed { index, lead ->
                    repository.updateTaskStatus(
                        taskId = task.id,
                        newStatus = "RUNNING",
                        newLog = "Lead #${index + 1} [${lead.sourcePlatform}]: ${lead.title} (${lead.link})"
                    )
                }

                // AI Text Generation via Google Gemini with fallback to OpenRouter
                if (geminiKey.isNotBlank() || openRouterKey.isNotBlank()) {
                    repository.updateTaskStatus(
                        taskId = task.id,
                        newStatus = "RUNNING",
                        newLog = "Generating deliverable using AI Engine ($geminiModel / fallback $openRouterModel)..."
                    )

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
                        leads = searchResult.leads
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
                        |AI WORKER DELIVERABLE
                        |========================================
                        |Engine: $providerUsed
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

                        // 3. Resend Email Dispatch on Task Completion
                        val targetEmail = task.clientEmail.trim().ifBlank {
                            // Extract email from discovered search leads snippet/title if present
                            val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
                            searchResult.leads.mapNotNull {
                                emailRegex.find(it.snippet)?.value ?: emailRegex.find(it.title)?.value
                            }.firstOrNull().orEmpty()
                        }

                        if (resendKey.isNotBlank()) {
                            if (targetEmail.isNotBlank()) {
                                repository.updateTaskStatus(
                                    taskId = task.id,
                                    newStatus = "COMPLETED",
                                    newLog = "Dispatching proposal email via Resend to $targetEmail..."
                                )
                                val emailRes = emailService.sendEmail(
                                    apiKey = resendKey,
                                    toEmail = targetEmail,
                                    subject = "SEO Audit & Growth Proposal: ${task.title}",
                                    bodyText = aiContent
                                )
                                if (emailRes.isSuccess) {
                                    val sent = emailRes.getOrThrow()
                                    repository.updateTaskStatus(
                                        taskId = task.id,
                                        newStatus = "COMPLETED",
                                        newLog = "Proposal emailed to $targetEmail via Resend (ID: ${sent.emailId})"
                                    )
                                } else {
                                    val err = emailRes.exceptionOrNull()?.message ?: "Unknown delivery error"
                                    repository.updateTaskStatus(
                                        taskId = task.id,
                                        newStatus = "COMPLETED",
                                        newLog = "Resend auto-dispatch error: $err"
                                    )
                                }
                            } else {
                                repository.updateTaskStatus(
                                    taskId = task.id,
                                    newStatus = "COMPLETED",
                                    newLog = "Proposal ready. No recipient email specified yet — click 'Send Email' in task details to dispatch."
                                )
                            }
                        } else {
                            repository.updateTaskStatus(
                                taskId = task.id,
                                newStatus = "COMPLETED",
                                newLog = "Proposal ready. Add a Resend API Key in Settings to enable automated email dispatch."
                            )
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
                    // AI keys not configured
                    completeWithDiscoveredLeads(
                        task,
                        searchResult,
                        "Notice: Gemini API Key / OpenRouter Key not configured in Settings. Saved discovered search leads without AI synthesis."
                    )
                }
            }
        }
    }

    /**
     * Manually dispatches the task's proposal to the specified recipient via Resend API.
     */
    fun sendTaskEmail(
        task: Task,
        recipientEmail: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val currentConfig = settingsRepository.settingsFlow.value
            val resendKey = currentConfig.resendApiKey

            if (resendKey.isBlank()) {
                onResult(false, "Please configure your Resend API Key in Settings first.")
                return@launch
            }

            val target = recipientEmail.trim().ifBlank { task.clientEmail.trim() }
            if (target.isBlank()) {
                onResult(false, "Please enter a valid recipient email address.")
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
                newLog = "Manually sending proposal email via Resend to $target..."
            )

            val result = emailService.sendEmail(
                apiKey = resendKey,
                toEmail = target,
                subject = "SEO Audit & Growth Proposal: ${task.title}",
                bodyText = contentToSend
            )

            if (result.isSuccess) {
                val sent = result.getOrThrow()
                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = task.status,
                    newLog = "Proposal emailed to $target via Resend (ID: ${sent.emailId})"
                )
                onResult(true, "Proposal sent to $target successfully (ID: ${sent.emailId.take(8)}...)")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to deliver email"
                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = task.status,
                    newLog = "Resend delivery failed: $err"
                )
                onResult(false, "Failed to send email: $err")
            }
        }
    }

    fun testResendKey(key: String, testEmail: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = emailService.testConnection(key, testEmail)
            if (result.isSuccess) {
                onResult(true, result.getOrThrow())
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Resend connection error")
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
            val queryToRun = customQuery ?: currentConfig.searchQuery

            _discoveryProgress.value = LeadDiscoveryProgress(
                isRunning = true,
                stepMessage = "Querying Search API for active SEO audit requests...",
                error = null
            )

            val searchResult = searchService.searchWithFallback(
                query = queryToRun,
                serperKey = currentConfig.serperApiKey,
                tavilyKey = currentConfig.tavilyApiKey
            )

            val prospects = searchResult.leads
            if (prospects.isEmpty()) {
                _discoveryProgress.value = LeadDiscoveryProgress(
                    isRunning = false,
                    stepMessage = "No prospects found for this query.",
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
            val targetProspects = prospects.take(3)

            for ((index, prospect) in targetProspects.withIndex()) {
                _discoveryProgress.value = _discoveryProgress.value.copy(
                    stepMessage = "Formulating proposal ${index + 1}/${targetProspects.size} for ${prospect.sourcePlatform}..."
                )

                val prompt = "Write a high-converting, professional, personalized client pitch for this lead looking for an SEO audit:\nTitle: ${prospect.title}\nContext: ${prospect.snippet}\nLink: ${prospect.link}"
                val systemPrompt = "You are Apollo-Scout, an elite freelance SEO scout. Formulate a personalized outreach message with zero corporate fluff and clear actionable audit findings."

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

                val pitch = GeneratedPitch(
                    prospectNameOrCompany = prospect.sourcePlatform,
                    subjectLine = "Personalized SEO Audit Proposal for ${prospect.title.take(30)}",
                    personalizedPitchMessage = aiText,
                    auditHooks = listOf("Technical SEO Audit", "Competitive Gap Analysis"),
                    suggestedRateOrValue = 0.0, // Clean pipeline, no fake dollars
                    recommendedCta = "Offer a complimentary 4-minute Loom video audit"
                )

                generatedPitches.add(pitch)

                // Check if lead snippet/link has an email
                val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
                val discoveredEmail = emailRegex.find(prospect.snippet)?.value
                    ?: emailRegex.find(prospect.title)?.value
                    ?: ""

                // Add real discovered task to repository
                val task = Task(
                    title = "Pitching SEO Audit to ${prospect.title.take(40)}",
                    category = "Client Acquisition",
                    workerName = "Apollo-Scout",
                    workerRole = "Lead Generation Specialist",
                    status = "PENDING",
                    priority = "HIGH",
                    client = "${prospect.sourcePlatform} • Lead",
                    clientEmail = discoveredEmail,
                    estimatedValue = 0.0,
                    details = buildString {
                        append("Subject: ${pitch.subjectLine}\n\n")
                        append("${pitch.personalizedPitchMessage}\n\n")
                        append("Recommended CTA: ${pitch.recommendedCta}\n")
                        append("Discovered Lead URL: ${prospect.link}\n")
                        append("Lead Snippet: ${prospect.snippet}\n")
                    },
                    logs = listOf(
                        "Discovered via ${searchResult.providerUsed} on ${prospect.sourcePlatform}",
                        "Proposal drafted via $engineUsed"
                    )
                )
                repository.createTask(task)
            }

            _discoveryProgress.value = LeadDiscoveryProgress(
                isRunning = false,
                stepMessage = "Completed! Generated ${generatedPitches.size} live pitches ready for Approval.",
                prospects = prospects,
                generatedPitches = generatedPitches
            )
        }
    }
}
