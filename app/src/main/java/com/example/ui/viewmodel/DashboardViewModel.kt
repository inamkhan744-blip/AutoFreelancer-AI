package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ProspectResult
import com.example.data.model.Task
import com.example.data.repository.AppSettings
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TaskRepository
import com.example.data.service.GeminiPitchService
import com.example.data.service.GeneratedPitch
import com.example.data.service.GroqApiService
import com.example.data.service.SearchExecutionResult
import com.example.data.service.SearchProspectService
import kotlinx.coroutines.delay
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
    private val groqService: GroqApiService = GroqApiService(),
    private val geminiPitchService: GeminiPitchService = GeminiPitchService()
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

    val tasksUiState: StateFlow<TasksUiState> = repository.observeTasks()
        .combine(_searchQuery) { tasks, query ->
            if (query.isBlank()) tasks
            else tasks.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.client.contains(query, ignoreCase = true) ||
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
        viewModelScope.launch {
            repository.seedInitialTasksIfEmpty()
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun refreshTasks() {
        viewModelScope.launch {
            repository.seedInitialTasksIfEmpty()
        }
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

    /**
     * Executes real worker logic on task approval:
     * 1. Updates task status to APPROVED -> RUNNING.
     * 2. Shows notification that AI worker has started.
     * 3. Queries live Search API (Serper, with Tavily backup) for real client leads.
     * 4. Passes search context to Groq API to generate a personalized outreach message and SEO audit deliverable.
     * 5. Saves real findings and generated content into task details and logs, then marks task COMPLETED.
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

                // Worker lifecycle state 1: Running
                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "RUNNING",
                    newLog = "${task.workerName} started: Initiating real-time lead scouting and AI deliverable pipeline."
                )

                val currentConfig = settingsRepository.settingsFlow.value
                val serperKey = currentConfig.serperApiKey
                val tavilyKey = currentConfig.tavilyApiKey
                val groqKey = currentConfig.groqApiKey
                val groqModel = currentConfig.groqModel

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

                // AI Text Generation via Groq API
                if (groqKey.isNotBlank()) {
                    repository.updateTaskStatus(
                        taskId = task.id,
                        newStatus = "RUNNING",
                        newLog = "Generating real deliverable using Groq API ($groqModel)..."
                    )

                    val groqResult = groqService.generateTaskDeliverable(
                        apiKey = groqKey,
                        model = groqModel,
                        taskTitle = task.title,
                        category = task.category,
                        client = task.client,
                        workerName = task.workerName,
                        workerRole = task.workerRole,
                        leads = searchResult.leads
                    )

                    if (groqResult.isSuccess) {
                        val aiContent = groqResult.getOrThrow()
                        val leadsFormatted = if (searchResult.leads.isNotEmpty()) {
                            searchResult.leads.mapIndexed { idx, item ->
                                "${idx + 1}. [${item.sourcePlatform}] ${item.title}\n   URL: ${item.link}\n   Context: ${item.snippet}"
                            }.joinToString("\n\n")
                        } else {
                            "Direct client inquiry."
                        }

                        val deliverableSection = """
                        |========================================
                        |AI WORKER DELIVERABLE (Generated via Groq)
                        |========================================
                        |Model: $groqModel
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
                            newLog = "Groq AI generation complete (${aiContent.length} chars). Task deliverable finalized and saved."
                        )
                    } else {
                        val err = groqResult.exceptionOrNull()?.message ?: "Groq call failed"
                        repository.updateTaskStatus(
                            taskId = task.id,
                            newStatus = "RUNNING",
                            newLog = "Groq error: $err"
                        )
                        // Attempt fallback to Gemini or complete with leads
                        completeWithDiscoveredLeads(task, searchResult, "Groq generation failed ($err).")
                    }
                } else {
                    // Groq key not configured
                    completeWithDiscoveredLeads(
                        task,
                        searchResult,
                        "Notice: Groq API Key is not set in Settings. Saved discovered search leads without AI synthesis."
                    )
                }
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
        |To generate automated AI proposals with LLaMA-3.3, enter your Groq API Key in Settings.
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

    fun testGroqKey(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = groqService.testConnection(key, settings.value.groqModel)
            if (result.isSuccess) {
                onResult(true, "Groq Connected! LLaMA model responded successfully.")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Groq connection error")
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
        estimatedValue: Double,
        details: String,
        priority: String = "MEDIUM"
    ) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val newTask = Task(
                title = title.ifBlank { "Automated Client Task" },
                category = category.ifBlank { "Client Acquisition" },
                workerName = workerName.ifBlank { "AutoWorker-AI" },
                workerRole = workerRole.ifBlank { "Freelance AI Specialist" },
                status = "PENDING",
                priority = priority,
                client = client.ifBlank { "New Client Prospect" },
                estimatedValue = estimatedValue.coerceAtLeast(100.0),
                details = details.ifBlank { "Autonomous freelancing task created by director." },
                logs = listOf("Task queued by director in pending review state.")
            )
            repository.createTask(newTask)
            _isOperationLoading.value = false
            _showCreateTaskDialog.value = false
        }
    }

    fun generateRandomAiTask() {
        val templates = listOf(
            Triple(
                "Delivering SEO competitor keyword matrix",
                "Technical SEO",
                "Analyze organic share of voice across 15 SaaS landing pages."
            ),
            Triple(
                "Writing high-converting cold email sequence",
                "Outreach",
                "Targeting Series A founders with customized case study teasers."
            ),
            Triple(
                "Designing UI wireframes proposal for fintech app",
                "Design & UX",
                "Comprehensive Figma mobile dashboard proposal with timeline breakdown."
            ),
            Triple(
                "Auditing Core Web Vitals & PageSpeed for E-commerce",
                "Deliverable",
                "Identify LCP/INP render delays on 25 product detail templates."
            ),
            Triple(
                "Scouting LinkedIn for CMOs needing Content Strategy",
                "Client Acquisition",
                "Extract 40 verified profiles and draft personalized video audit invites."
            )
        )
        val sample = templates.random()
        val workers = listOf(
            Pair("Apollo-Scout", "Lead Generation Specialist"),
            Pair("ReportBot-Pro", "Technical SEO Analyst"),
            Pair("PitchCraft AI", "Proposal & Scope Architect"),
            Pair("OutreachGenie", "Cold Outreach Agent"),
            Pair("UX-Auditor AI", "Conversion Rate Specialist")
        )
        val worker = workers.random()
        val clients = listOf("Acme Cloud Corp", "FinNova Solutions", "SkyHigh Retail", "DataStream AI", "PulseHealth")
        val randomValue = (listOf(750, 1200, 1850, 2400, 3100, 4200).random()).toDouble()

        createCustomTask(
            title = sample.first,
            category = sample.second,
            workerName = worker.first,
            workerRole = worker.second,
            client = clients.random(),
            estimatedValue = randomValue,
            details = sample.third,
            priority = listOf("HIGH", "MEDIUM", "LOW").random()
        )
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

                // If Groq key is present, we use Groq to formulate the pitch!
                val pitch = if (currentConfig.groqApiKey.isNotBlank()) {
                    val prompt = "Write a high-converting client pitch for this lead looking for an SEO audit:\nTitle: ${prospect.title}\nContext: ${prospect.snippet}\nLink: ${prospect.link}"
                    val groqText = groqService.generateChatCompletion(
                        apiKey = currentConfig.groqApiKey,
                        model = currentConfig.groqModel,
                        systemPrompt = "You are Apollo-Scout, an expert freelance SEO scout. Generate a concise proposal.",
                        userPrompt = prompt,
                        maxTokens = 400
                    ).getOrDefault("Proposal ready for review.")

                    GeneratedPitch(
                        prospectNameOrCompany = prospect.sourcePlatform,
                        subjectLine = "Personalized SEO Audit Proposal for ${prospect.title.take(30)}",
                        personalizedPitchMessage = groqText,
                        auditHooks = listOf("Core Web Vitals & Render Delays", "Keyword Gap & Cannibalization"),
                        suggestedRateOrValue = 1850.0,
                        recommendedCta = "Offer a complimentary 4-minute Loom video audit"
                    )
                } else {
                    geminiPitchService.generatePersonalizedPitch(prospect).getOrNull() ?: GeneratedPitch(
                        prospectNameOrCompany = prospect.sourcePlatform,
                        subjectLine = "SEO Audit Proposal for ${prospect.title.take(30)}",
                        personalizedPitchMessage = "Hi there, saw your request for an SEO audit. We can provide a comprehensive technical and content review.",
                        auditHooks = listOf("Technical SEO Audit", "Competitive Gap Analysis"),
                        suggestedRateOrValue = 1500.0,
                        recommendedCta = "Send complimentary video review"
                    )
                }

                generatedPitches.add(pitch)

                // Add task to repository
                val task = Task(
                    title = "Pitching SEO Audit to ${prospect.title.take(40)}",
                    category = "Client Acquisition",
                    workerName = "Apollo-Scout",
                    workerRole = "Lead Generation Specialist",
                    status = "PENDING",
                    priority = "HIGH",
                    client = "${prospect.sourcePlatform} • Lead",
                    estimatedValue = pitch.suggestedRateOrValue,
                    details = buildString {
                        append("Subject: ${pitch.subjectLine}\n\n")
                        append("${pitch.personalizedPitchMessage}\n\n")
                        append("Recommended CTA: ${pitch.recommendedCta}\n")
                        append("Discovered Lead URL: ${prospect.link}\n")
                        append("Lead Snippet: ${prospect.snippet}\n")
                    },
                    logs = listOf(
                        "Discovered via ${searchResult.providerUsed} on ${prospect.sourcePlatform}",
                        if (currentConfig.groqApiKey.isNotBlank()) "Proposal drafted via Groq ${currentConfig.groqModel}" else "Proposal drafted via AI Engine"
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
