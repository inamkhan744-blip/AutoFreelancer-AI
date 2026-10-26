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
import com.example.data.service.SearchProspectService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
        .map<List<Task>, TasksUiState> { filteredTasks ->
            TasksUiState.Success(filteredTasks)
        }
        .catch { error ->
            emit(TasksUiState.Error(error.localizedMessage ?: "Failed to observe tasks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TasksUiState.Loading
        )

    val metrics: StateFlow<DashboardMetrics> = repository.observeTasks()
        .combine(MutableStateFlow(Unit)) { tasks, _ ->
            DashboardMetrics(
                totalTasks = tasks.size,
                pendingCount = tasks.count { it.status == "PENDING" },
                activeWorkersCount = tasks.count { it.status == "APPROVED" || it.status == "RUNNING" },
                completedCount = tasks.count { it.status == "COMPLETED" },
                totalEstimatedValue = tasks.sumOf { it.estimatedValue }
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

    fun approveTask(task: Task) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            val result = repository.approveTask(task.id)
            _isOperationLoading.value = false

            if (result.isSuccess) {
                // Show notification that AI worker has started
                _activeNotification.value = WorkerNotification(
                    workerName = task.workerName,
                    taskTitle = task.title
                )

                // Simulate worker lifecycle progression
                delay(3000)
                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "RUNNING",
                    newLog = "${task.workerName} active: Initiated workflow telemetry and task routines."
                )

                delay(5000)
                repository.updateTaskStatus(
                    taskId = task.id,
                    newStatus = "RUNNING",
                    newLog = "Processing deliverable assets: 60% completion checkpoint reached."
                )
            }
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

    /**
     * Executes the end-to-end Automated Lead Scout:
     * 1. Uses Search API to find people asking for SEO audits
     * 2. Uses Gemini API to write personalized pitch messages
     * 3. Queues tasks in the dashboard for director Approval or Rejection
     */
    fun runLeadDiscovery(customQuery: String? = null) {
        viewModelScope.launch {
            val currentConfig = settings.value
            val queryToRun = customQuery ?: currentConfig.searchQuery

            _discoveryProgress.value = LeadDiscoveryProgress(
                isRunning = true,
                stepMessage = "Querying Search API for active SEO audit requests...",
                error = null
            )

            val searchResult = searchService.findSeoAuditProspects(
                query = queryToRun,
                apiKey = currentConfig.searchApiKey,
                provider = currentConfig.searchProvider,
                googleCseId = currentConfig.googleCseId
            )

            if (searchResult.isFailure) {
                _discoveryProgress.value = LeadDiscoveryProgress(
                    isRunning = false,
                    stepMessage = "Search API failed: ${searchResult.exceptionOrNull()?.message}",
                    error = searchResult.exceptionOrNull()?.message
                )
                return@launch
            }

            val prospects = searchResult.getOrDefault(emptyList())
            if (prospects.isEmpty()) {
                _discoveryProgress.value = LeadDiscoveryProgress(
                    isRunning = false,
                    stepMessage = "No new prospects found for this query.",
                    prospects = emptyList()
                )
                return@launch
            }

            _discoveryProgress.value = LeadDiscoveryProgress(
                isRunning = true,
                stepMessage = "Found ${prospects.size} prospects. Gemini is writing personalized pitch drafts...",
                prospects = prospects
            )

            val generatedPitches = mutableListOf<GeneratedPitch>()
            // Process up to 3 prospects to stay within responsive interaction bounds
            val targetProspects = prospects.take(3)
            for ((index, prospect) in targetProspects.withIndex()) {
                _discoveryProgress.value = _discoveryProgress.value.copy(
                    stepMessage = "Drafting pitch ${index + 1}/${targetProspects.size} with Gemini for ${prospect.sourcePlatform}..."
                )

                val pitchResult = geminiPitchService.generatePersonalizedPitch(prospect)
                if (pitchResult.isSuccess) {
                    val pitch = pitchResult.getOrThrow()
                    generatedPitches.add(pitch)

                    // Add task to Firestore
                    val task = Task(
                        title = "Pitching SEO Audit to ${pitch.prospectNameOrCompany}",
                        category = "Client Acquisition",
                        workerName = "Apollo-Scout",
                        workerRole = "Lead Generation Specialist",
                        status = "PENDING",
                        priority = "HIGH",
                        client = "${prospect.sourcePlatform} • ${pitch.prospectNameOrCompany}",
                        estimatedValue = pitch.suggestedRateOrValue,
                        details = buildString {
                            append("Subject: ${pitch.subjectLine}\n\n")
                            append("${pitch.personalizedPitchMessage}\n\n")
                            append("Recommended CTA: ${pitch.recommendedCta}\n")
                            append("Source URL: ${prospect.link}\n")
                            if (pitch.auditHooks.isNotEmpty()) {
                                append("Audit Hypotheses:\n")
                                pitch.auditHooks.forEach { append("• $it\n") }
                            }
                        },
                        logs = listOf(
                            "Discovered via Search API on ${prospect.sourcePlatform}",
                            "Gemini 3.8-Flash formulated custom personalized pitch draft"
                        )
                    )
                    repository.createTask(task)
                }
            }

            _discoveryProgress.value = LeadDiscoveryProgress(
                isRunning = false,
                stepMessage = "Completed! Generated ${generatedPitches.size} personalized pitches ready for Approval.",
                prospects = prospects,
                generatedPitches = generatedPitches
            )
        }
    }
}
