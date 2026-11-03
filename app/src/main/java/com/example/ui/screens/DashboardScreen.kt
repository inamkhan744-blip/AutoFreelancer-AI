package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.ClipData
import android.content.ClipboardManager
import com.example.data.model.CrmClient
import com.example.data.model.DealPipelineStage
import com.example.data.model.InboxEmail
import com.example.data.model.Task
import com.example.data.service.PaidDealEngine
import com.example.data.service.ServiceTier
import com.example.data.service.GeneratedPitch
import com.example.data.service.GeneratedVoiceNote
import com.example.ui.components.MonthlyIncomeTrendsCard
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberPrimaryLight
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberTertiary
import com.example.ui.theme.CyberWarning
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.LeadDiscoveryProgress
import com.example.ui.viewmodel.TasksUiState
import com.example.ui.viewmodel.WorkerNotification
import com.example.util.JarvisVoiceManager
import com.example.util.VoiceCommandState
import com.example.util.WorkerLogEntry
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    onSignOutClicked: () -> Unit
) {
    val tasksState by viewModel.tasksUiState.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val activeNotification by viewModel.activeNotification.collectAsState()
    val selectedTask by viewModel.selectedTaskForDetail.collectAsState()
    val rejectTargetTask by viewModel.rejectTargetTask.collectAsState()
    val showCreateDialog by viewModel.showCreateTaskDialog.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val isOperationLoading by viewModel.isOperationLoading.collectAsState()
    val isAutoPilotActive by viewModel.isAutoPilotActive.collectAsState()
    val liveTerminalEntries by viewModel.liveTerminalEntries.collectAsState()
    val discoveryProgress by viewModel.discoveryProgress.collectAsState()
    val todayLeadCount by viewModel.todayLeadCount.collectAsState()
    val pipelineTotalValue by viewModel.pipelineTotalValue.collectAsState()
    val totalEarnedRevenue by viewModel.totalEarnedRevenue.collectAsState()
    val pendingInvoicesValue by viewModel.pendingInvoicesValue.collectAsState()
    val paidClientsCount by viewModel.paidClientsCount.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val monthlyIncomeTrends by viewModel.monthlyIncomeTrends.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val hotLeads by viewModel.hotLeads.collectAsState()
    val hotLeadsCount by viewModel.hotLeadsCount.collectAsState()

    var showDealCloserDialog by remember { mutableStateOf(false) }
    var selectedClientForOffer by remember { mutableStateOf<CrmClient?>(null) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var selectedClientForPayment by remember { mutableStateOf<CrmClient?>(null) }

    val userName = "Freelancer Director"

    val context = LocalContext.current
    val jarvisManager = remember {
        JarvisVoiceManager(
            context = context,
            onApproveAllTasks = {
                viewModel.approveAllPendingTasks { count ->
                    Toast.makeText(context, "Jarvis approved $count tasks!", Toast.LENGTH_SHORT).show()
                }
            },
            onFindLeads = {
                viewModel.runLeadDiscovery()
                Toast.makeText(context, "Jarvis launched Lead Discovery!", Toast.LENGTH_SHORT).show()
            },
            onNavigateTo = { screen ->
                when (screen) {
                    "SETTINGS" -> onNavigateToSettings()
                    else -> viewModel.navigateTo(screen)
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            jarvisManager.stopListening()
        }
    }

    val voiceState by jarvisManager.voiceState.collectAsState()
    val lastRecognizedText by jarvisManager.lastRecognizedText.collectAsState()
    var showJarvisDialog by remember { mutableStateOf(false) }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showJarvisDialog = true
            jarvisManager.startListening()
        } else {
            Toast.makeText(context, "Microphone permission required for Jarvis voice commands", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Auto-Freelancer AI",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (isAutoPilotActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CyberTertiary.copy(alpha = 0.2f))
                                            .border(1.dp, CyberTertiary, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .testTag("auto_pilot_active_badge")
                                    ) {
                                        Text(
                                            text = "Auto-Pilot Active",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = CyberTertiary,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Director: $userName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.runLeadDiscovery() },
                        modifier = Modifier.testTag("lead_scout_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Run Search API & Gemini Lead Scout",
                            tint = CyberSecondary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openCreateTaskDialog() },
                        modifier = Modifier.testTag("add_task_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Task",
                            tint = CyberPrimaryLight
                        )
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo("ANALYTICS") },
                        modifier = Modifier.testTag("analytics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Client Conversion Analytics",
                            tint = CyberTertiary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & API Keys",
                            tint = CyberPrimaryLight
                        )
                    }
                    IconButton(
                        onClick = onSignOutClicked,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync / Refresh Tasks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        showJarvisDialog = true
                        jarvisManager.startListening()
                    } else {
                        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.testTag("jarvis_mic_button"),
                containerColor = if (voiceState is VoiceCommandState.Listening) CyberSecondary else CyberPrimary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (voiceState is VoiceCommandState.Listening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Jarvis Voice Commands"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 840.dp

            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = activeNotification != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    activeNotification?.let { notif ->
                        WorkerNotificationBanner(
                            notification = notif,
                            onDismiss = { viewModel.dismissNotification() }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isAutoPilotActive,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("auto_pilot_banner"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberTertiary.copy(alpha = 0.12f)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(CyberTertiary, CyberSecondary))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(CyberTertiary.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = CyberTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Auto-Pilot Mode Active",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CyberTertiary
                                    )
                                    Text(
                                        text = "Pending tasks auto-approved • Background periodic sync running via WorkManager",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            AssistChip(
                                onClick = onNavigateToSettings,
                                label = { Text("Settings", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                LiveTerminalPanel(entries = liveTerminalEntries)

                LeadDiscoveryShowcaseCard(
                    progress = discoveryProgress,
                    onDismiss = { viewModel.dismissDiscoveryProgress() },
                    onSendPitch = { pitch ->
                        val targetProspect = discoveryProgress.prospects.find {
                            it.title.contains(pitch.prospectNameOrCompany, ignoreCase = true) ||
                            pitch.personalizedPitchMessage.contains(it.link)
                        }
                        val email = targetProspect?.enrichment?.url?.let { "" } ?: ""
                        viewModel.sendEmailToClient(
                            toEmail = email.ifBlank { "client@${pitch.prospectNameOrCompany.lowercase().replace(" ", "")}.com" },
                            subject = pitch.subjectLine,
                            bodyText = pitch.personalizedPitchMessage
                        ) { success, msg ->
                            Toast.makeText(context, if (success) "Pitch dispatched via Brevo!" else msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSaveToCrm = { pitch ->
                        viewModel.createCustomTask(
                            title = "Pitch: ${pitch.prospectNameOrCompany}",
                            category = "OUTREACH",
                            workerName = "Sophia AI",
                            workerRole = "Lead Closer",
                            client = pitch.prospectNameOrCompany,
                            clientEmail = "client@${pitch.prospectNameOrCompany.lowercase().replace(" ", "")}.com",
                            estimatedValue = pitch.suggestedRateOrValue,
                            details = pitch.personalizedPitchMessage,
                            priority = "HIGH"
                        )
                        Toast.makeText(context, "Saved ${pitch.prospectNameOrCompany} to CRM & Tasks!", Toast.LENGTH_SHORT).show()
                    }
                )

                ExecutiveAgencyGoalsHeader(
                    todayLeadCount = todayLeadCount,
                    dailyLeadTarget = settings.dailyLeadTarget,
                    pipelineTotalValue = pipelineTotalValue,
                    dailyRevenueTarget = settings.dailyRevenueTarget,
                    isAutoPilotActive = isAutoPilotActive,
                    onLeadScoutClicked = { viewModel.runLeadDiscovery() },
                    onSyncImapClicked = {
                        viewModel.syncInboxReplies { count ->
                            Toast.makeText(context, "IMAP Sync completed: $count replies", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onToggleAutoPilot = onNavigateToSettings,
                    onNewTaskClicked = { viewModel.openCreateTaskDialog() }
                )

                if (isWideScreen) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            MetricsHeader(metrics = metrics)
                            Spacer(modifier = Modifier.height(8.dp))
                            RealDollarRevenueCard(
                                totalEarnedRevenue = totalEarnedRevenue,
                                pendingInvoicesValue = pendingInvoicesValue,
                                paidClientsCount = paidClientsCount,
                                onOpenDealCloser = {
                                    selectedClientForOffer = clients.firstOrNull()
                                    showDealCloserDialog = true
                                },
                                onOpenRecordPayment = {
                                    selectedClientForPayment = clients.firstOrNull()
                                    showRecordPaymentDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SearchAndFilterBar(
                                searchQuery = searchQuery,
                                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                statusFilter = statusFilter,
                                onFilterChanged = { viewModel.onStatusFilterChanged(it) },
                                hotLeadsCount = hotLeadsCount
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (statusFilter == "HOT_LEADS") {
                                HotLeadsDashboardView(
                                    hotLeads = hotLeads,
                                    isLoading = isOperationLoading,
                                    onScanWithGemini = { viewModel.scanAndClassifyAllHotLeads() },
                                    onSyncImap = { viewModel.syncInboxReplies() },
                                    onConvertToTask = { viewModel.convertHotLeadToTask(it) },
                                    onViewInInbox = { viewModel.navigateTo("INBOX") },
                                    onSendPaidOffer = { email ->
                                        val match = clients.find { c -> c.email.equals(email.senderEmail, ignoreCase = true) }
                                            ?: CrmClient(
                                                name = email.senderName.ifBlank { email.senderEmail.substringBefore("@") },
                                                email = email.senderEmail,
                                                company = email.senderEmail.substringAfter("@").substringBefore("."),
                                                pipelineStage = DealPipelineStage.INTERESTED
                                            )
                                        selectedClientForOffer = match
                                        showDealCloserDialog = true
                                    }
                                )
                            } else {
                                TaskListContent(
                                    tasksState = tasksState,
                                    selectedTaskId = selectedTask?.id,
                                    onTaskClicked = { viewModel.selectTaskForDetail(it) },
                                    onApprove = { viewModel.approveTask(it) },
                                    onReject = { viewModel.openRejectDialog(it) },
                                    onComplete = { viewModel.completeTask(it.id) },
                                    onScoutClicked = { viewModel.runLeadDiscovery() },
                                    onSettingsClicked = onNavigateToSettings
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(0.8f)
                                .fillMaxHeight()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(topStart = 16.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            TaskDetailPane(
                                task = selectedTask,
                                onApprove = { selectedTask?.let { viewModel.approveTask(it) } },
                                onReject = { selectedTask?.let { viewModel.openRejectDialog(it) } },
                                onComplete = { selectedTask?.let { viewModel.completeTask(it.id) } },
                                onClose = { viewModel.selectTaskForDetail(null) },
                                viewModel = viewModel
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("dashboard_scroll_list"),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            MetricsHeader(metrics = metrics)
                        }
                        item {
                            RealDollarRevenueCard(
                                totalEarnedRevenue = totalEarnedRevenue,
                                pendingInvoicesValue = pendingInvoicesValue,
                                paidClientsCount = paidClientsCount,
                                onOpenDealCloser = {
                                    selectedClientForOffer = clients.firstOrNull()
                                    showDealCloserDialog = true
                                },
                                onOpenRecordPayment = {
                                    selectedClientForPayment = clients.firstOrNull()
                                    showRecordPaymentDialog = true
                                }
                            )
                        }
                        item {
                            MonthlyIncomeTrendsCard(monthlyPoints = monthlyIncomeTrends)
                        }
                        item {
                            SearchAndFilterBar(
                                searchQuery = searchQuery,
                                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                statusFilter = statusFilter,
                                onFilterChanged = { viewModel.onStatusFilterChanged(it) },
                                hotLeadsCount = hotLeadsCount
                            )
                        }
                        if (statusFilter == "HOT_LEADS") {
                            item {
                                HotLeadsDashboardView(
                                    hotLeads = hotLeads,
                                    isLoading = isOperationLoading,
                                    onScanWithGemini = { viewModel.scanAndClassifyAllHotLeads() },
                                    onSyncImap = { viewModel.syncInboxReplies() },
                                    onConvertToTask = { viewModel.convertHotLeadToTask(it) },
                                    onViewInInbox = { viewModel.navigateTo("INBOX") },
                                    onSendPaidOffer = { email ->
                                        val match = clients.find { c -> c.email.equals(email.senderEmail, ignoreCase = true) }
                                            ?: CrmClient(
                                                name = email.senderName.ifBlank { email.senderEmail.substringBefore("@") },
                                                email = email.senderEmail,
                                                company = email.senderEmail.substringAfter("@").substringBefore("."),
                                                pipelineStage = DealPipelineStage.INTERESTED
                                            )
                                        selectedClientForOffer = match
                                        showDealCloserDialog = true
                                    }
                                )
                            }
                        } else {
                            when (val state = tasksState) {
                                is TasksUiState.Loading -> {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = CyberPrimary)
                                        }
                                    }
                                }
                                is TasksUiState.Error -> {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = CyberError.copy(alpha = 0.1f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(text = "Unable to load tasks", style = MaterialTheme.typography.titleMedium, color = CyberError)
                                                Text(text = state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                                is TasksUiState.Success -> {
                                    if (state.tasks.isEmpty()) {
                                        item {
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(Icons.Default.Work, contentDescription = null, tint = CyberPrimaryLight, modifier = Modifier.size(36.dp))
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Text("No tasks currently in pipeline", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text("Run 24/7 Lead Scout above to find verified clients.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.height(14.dp))
                                                    Button(
                                                        onClick = { viewModel.runLeadDiscovery() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                                                    ) {
                                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Run 24/7 Lead Hunter")
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        items(state.tasks, key = { it.id }) { task ->
                                            TaskItemCard(
                                                task = task,
                                                isSelected = task.id == selectedTask?.id,
                                                onClick = { viewModel.selectTaskForDetail(task) },
                                                onApprove = { viewModel.approveTask(task) },
                                                onReject = { viewModel.openRejectDialog(task) },
                                                onComplete = { viewModel.completeTask(task.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedTask != null) {
        BoxWithConstraints {
            if (maxWidth < 840.dp) {
                AlertDialog(
                    onDismissRequest = { viewModel.selectTaskForDetail(null) },
                    confirmButton = {
                        if (selectedTask?.status == "PENDING") {
                            Button(
                                onClick = {
                                    selectedTask?.let { viewModel.approveTask(it) }
                                    viewModel.selectTaskForDetail(null)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                            ) {
                                Text("Approve Task")
                            }
                        } else {
                            TextButton(onClick = { viewModel.selectTaskForDetail(null) }) {
                                Text("Close")
                            }
                        }
                    },
                    dismissButton = {
                        if (selectedTask?.status == "PENDING") {
                            OutlinedButton(
                                onClick = {
                                    selectedTask?.let { viewModel.openRejectDialog(it) }
                                }
                            ) {
                                Text("Reject")
                            }
                        }
                    },
                    text = {
                        selectedTask?.let { task ->
                            var mobileRecipientEmail by remember(task.id, task.clientEmail) { mutableStateOf(task.clientEmail) }
                            var mobileSendingEmail by remember { mutableStateOf(false) }
                            var mobileEmailStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
                            var mobileVoiceNote by remember(task.id) { mutableStateOf<GeneratedVoiceNote?>(null) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Client: ${task.client} • Est. Value: $${task.estimatedValue.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyberTertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (task.clientEmail.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Client Email: ${task.clientEmail}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CyberSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Assigned Worker: ${task.workerName} (${task.workerRole})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Brief Details:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = task.details,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                VoiceNoteSection(
                                    task = task,
                                    viewModel = viewModel,
                                    voiceNote = mobileVoiceNote,
                                    onVoiceNoteGenerated = { mobileVoiceNote = it }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = CyberTertiary, modifier = Modifier.size(16.dp))
                                            Text("Send Proposal via Email (Brevo)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = mobileRecipientEmail,
                                            onValueChange = { mobileRecipientEmail = it },
                                            label = { Text("Recipient Email") },
                                            placeholder = { Text("client@example.com") },
                                            modifier = Modifier.fillMaxWidth().testTag("mobile_recipient_email_input"),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                if (mobileRecipientEmail.isNotBlank()) {
                                                    mobileSendingEmail = true
                                                    mobileEmailStatus = null
                                                    viewModel.sendTaskEmail(task, mobileRecipientEmail, mobileVoiceNote) { success, msg ->
                                                        mobileSendingEmail = false
                                                        mobileEmailStatus = Pair(success, msg)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("send_email_button"),
                                            enabled = !mobileSendingEmail && mobileRecipientEmail.isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberTertiary)
                                        ) {
                                            if (mobileSendingEmail) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Sending Email...")
                                            } else {
                                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Send Email" + if (mobileVoiceNote != null) " + Voice Note" else "")
                                            }
                                        }

                                        mobileEmailStatus?.let { (success, msg) ->
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = msg,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (success) CyberTertiary else CyberError
                                            )
                                        }
                                    }
                                }

                                if (task.logs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Live Execution Logs:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = CyberSecondary
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        task.logs.forEach { log ->
                                            Text(
                                                text = "▸ $log",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    rejectTargetTask?.let { task ->
        RejectReasonDialog(
            task = task,
            onDismiss = { viewModel.closeRejectDialog() },
            onConfirmReject = { reason ->
                viewModel.rejectTask(task.id, reason)
            }
        )
    }

    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { viewModel.closeCreateTaskDialog() },
            onSubmit = { title, category, worker, role, client, clientEmail, value, details, priority ->
                viewModel.createCustomTask(title, category, worker, role, client, clientEmail, value, details, priority)
            }
        )
    }

    if (showJarvisDialog || voiceState is VoiceCommandState.Listening || voiceState is VoiceCommandState.Processing) {
        JarvisVisualIndicatorDialog(
            voiceState = voiceState,
            lastRecognizedText = lastRecognizedText,
            onDismiss = {
                showJarvisDialog = false
                jarvisManager.stopListening()
            },
            onManualCommand = { cmd ->
                jarvisManager.executeManualCommand(cmd)
            }
        )
    }

    if (showDealCloserDialog) {
        InstantDealCloserDialog(
            initialClient = selectedClientForOffer,
            allClients = clients,
            agencyName = settings.agencyName,
            paymentLinkUrl = settings.paymentLinkUrl,
            onDismiss = { showDealCloserDialog = false },
            onSendOffer = { client, tier, customNote ->
                viewModel.sendInstantPaidOffer(client, tier, customNote) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            onRecordPaid = { client, amount ->
                viewModel.recordCashPaymentCollected(client, amount) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showRecordPaymentDialog) {
        RecordPaymentDialog(
            initialClient = selectedClientForPayment,
            allClients = clients,
            onDismiss = { showRecordPaymentDialog = false },
            onConfirmPayment = { client, amount, method ->
                viewModel.recordCashPaymentCollected(client, amount, method) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
private fun LiveTerminalPanel(entries: List<WorkerLogEntry>) {
    val scrollState = rememberScrollState()
    LaunchedEffect(entries.lastOrNull()?.id) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("live_worker_terminal"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050907)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D65A).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LIVE WORKER TERMINAL",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF00F56A)
                    )
                    Text(
                        text = "(${entries.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF55FF91),
                        fontSize = 10.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "● LIVE",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = Color(0xFF55FF91)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Terminal Logs",
                        tint = Color(0xFF00F56A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (entries.isEmpty()) {
                        Text(
                            text = "> Ready. Autonomous AI workers waiting for tasks...",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = Color(0xFF9BFFB8).copy(alpha = 0.7f)
                        )
                    } else {
                        entries.takeLast(10).forEach { entry ->
                            Text(
                                text = "> [${entry.worker}] ${entry.message}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = Color(0xFF9BFFB8),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeadDiscoveryShowcaseCard(
    progress: LeadDiscoveryProgress,
    onDismiss: () -> Unit,
    onSendPitch: (GeneratedPitch) -> Unit,
    onSaveToCrm: (GeneratedPitch) -> Unit
) {
    if (progress.isRunning) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("lead_scout_running_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF071510)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberSecondary.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = CyberSecondary,
                        strokeWidth = 2.5.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Lead Scout & Hunter Active",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyberSecondary
                        )
                        Text(
                            text = progress.stepMessage.ifBlank { "Querying Serper, Brave, and Google CSE with Gemini..." },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = CyberSecondary,
                    trackColor = CyberSecondary.copy(alpha = 0.2f)
                )
            }
        }
    } else if (progress.generatedPitches.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("lead_scout_showcase_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberSecondary)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CyberSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyberSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Discovered Leads & Live Pitches",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = CyberSecondary
                            )
                            Text(
                                text = "${progress.generatedPitches.size} verified prospects found & enriched",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("dismiss_discovery_showcase")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Showcase",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    progress.generatedPitches.forEachIndexed { index, pitch ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("discovered_pitch_card_$index"),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pitch.prospectNameOrCompany,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CyberTertiary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$${pitch.suggestedRateOrValue.toInt()} Quote",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = CyberTertiary
                                        )
                                    }
                                }

                                if (pitch.auditHooks.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        pitch.auditHooks.take(2).forEach { hook ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(CyberPrimary.copy(alpha = 0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = hook.take(35),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = CyberPrimary,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pitch.personalizedPitchMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onSendPitch(pitch) },
                                        modifier = Modifier.weight(1f).testTag("pitch_send_brevo_$index"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary, contentColor = Color.Black)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Send Pitch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onSaveToCrm(pitch) },
                                        modifier = Modifier.weight(1f).testTag("pitch_save_crm_$index"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberTertiary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save CRM", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutiveAgencyGoalsHeader(
    todayLeadCount: Int,
    dailyLeadTarget: Int,
    pipelineTotalValue: Double,
    dailyRevenueTarget: Double,
    isAutoPilotActive: Boolean,
    onLeadScoutClicked: () -> Unit,
    onSyncImapClicked: () -> Unit,
    onToggleAutoPilot: () -> Unit,
    onNewTaskClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("executive_agency_goals_header"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AGENCY DAILY TARGETS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CyberPrimary
                )
                Text(
                    text = "Pipeline: $${pipelineTotalValue.toInt()}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyberTertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Lead Goal
                Column(modifier = Modifier.weight(1f)) {
                    val safeLeadTarget = dailyLeadTarget.coerceAtLeast(1)
                    val leadRatio = (todayLeadCount.toFloat() / safeLeadTarget).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Leads Today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$todayLeadCount / $safeLeadTarget",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { leadRatio },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = CyberPrimary,
                        trackColor = CyberPrimary.copy(alpha = 0.15f)
                    )
                }

                // Revenue Goal
                Column(modifier = Modifier.weight(1f)) {
                    val safeRevTarget = if (dailyRevenueTarget <= 0.0) 100.0 else dailyRevenueTarget
                    val revRatio = (pipelineTotalValue.toFloat() / safeRevTarget.toFloat()).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Revenue Goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$${pipelineTotalValue.toInt()} / $${safeRevTarget.toInt()}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { revRatio },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = CyberTertiary,
                        trackColor = CyberTertiary.copy(alpha = 0.15f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Executive Quick Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onLeadScoutClicked,
                    modifier = Modifier.weight(1f).testTag("quick_action_lead_scout"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Scout", fontSize = 11.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onSyncImapClicked,
                    modifier = Modifier.weight(1f).testTag("quick_action_sync_imap"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = CyberPrimaryLight, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("IMAP", fontSize = 11.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onToggleAutoPilot,
                    modifier = Modifier.weight(1f).testTag("quick_action_autopilot"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = if (isAutoPilotActive) CyberTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (isAutoPilotActive) "Auto: ON" else "Auto: OFF", fontSize = 11.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onNewTaskClicked,
                    modifier = Modifier.weight(1f).testTag("quick_action_new_task"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CyberTertiary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Task", fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun WorkerNotificationBanner(
    notification: WorkerNotification,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("worker_started_notification"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF064E3B)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(CyberTertiary, CyberSecondary)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyberTertiary.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = CyberTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AI Worker Dispatched",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(CyberTertiary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${notification.workerName} is now executing '${notification.taskTitle}'",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD1FAE5),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Notification",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun MetricsHeader(metrics: DashboardMetrics) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 } }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(
            title = "Pending",
            value = metrics.pendingCount.toString(),
            icon = Icons.Default.HourglassEmpty,
            color = CyberWarning,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Active AI",
            value = metrics.activeWorkersCount.toString(),
            icon = Icons.Default.RocketLaunch,
            color = CyberSecondary,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Completed",
            value = metrics.completedCount.toString(),
            icon = Icons.Default.CheckCircle,
            color = CyberTertiary,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Pipeline",
            value = currencyFormat.format(metrics.totalEstimatedValue),
            icon = Icons.Default.TrendingUp,
            color = CyberPrimaryLight,
            modifier = Modifier.weight(1.3f)
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    statusFilter: String,
    onFilterChanged: (String) -> Unit,
    hotLeadsCount: Int = 0
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_search_bar"),
            placeholder = { Text("Search AI tasks, clients, or workers...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf(
                "ALL" to "All Tasks",
                "HOT_LEADS" to if (hotLeadsCount > 0) "🔥 Hot Leads ($hotLeadsCount)" else "🔥 Hot Leads",
                "PENDING" to "Pending Approval",
                "ACTIVE" to "Active Workers",
                "COMPLETED" to "Completed",
                "REJECTED" to "Rejected"
            )

            filters.forEach { (key, label) ->
                val isHotChip = key == "HOT_LEADS"
                FilterChip(
                    selected = statusFilter == key,
                    onClick = { onFilterChanged(key) },
                    label = { Text(label, fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_chip_$key"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isHotChip) CyberWarning.copy(alpha = 0.25f) else CyberPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = if (isHotChip) CyberWarning else CyberPrimaryLight
                    )
                )
            }
        }
    }
}

@Composable
fun TaskListContent(
    tasksState: TasksUiState,
    selectedTaskId: String?,
    onTaskClicked: (Task) -> Unit,
    onApprove: (Task) -> Unit,
    onReject: (Task) -> Unit,
    onComplete: (Task) -> Unit,
    onScoutClicked: (() -> Unit)? = null,
    onSettingsClicked: (() -> Unit)? = null
) {
    when (tasksState) {
        is TasksUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberPrimary)
            }
        }
        is TasksUiState.Error -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberError.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unable to load tasks",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberError
                    )
                    Text(
                        text = tasksState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is TasksUiState.Success -> {
            if (tasksState.tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = CyberPrimaryLight,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tasks yet",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Configure your Gemini / AI and Search API keys in Settings, then run Lead Scout to discover live leads and generate AI audit pitches.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onScoutClicked != null) {
                                Button(
                                    onClick = onScoutClicked,
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run Lead Scout")
                                }
                            }
                            if (onSettingsClicked != null) {
                                OutlinedButton(
                                    onClick = onSettingsClicked
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Settings")
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(tasksState.tasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            isSelected = task.id == selectedTaskId,
                            onClick = { onTaskClicked(task) },
                            onApprove = { onApprove(task) },
                            onReject = { onReject(task) },
                            onComplete = { onComplete(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    isSelected: Boolean,
    onClick: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onComplete: () -> Unit
) {
    var expandedLogs by remember { mutableStateOf(false) }

    val statusBorderColor = when (task.status) {
        "PENDING" -> CyberWarning
        "APPROVED", "RUNNING" -> CyberSecondary
        "COMPLETED" -> CyberTertiary
        "REJECTED" -> CyberError
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("task_card_${task.id}")
            .then(
                if (isSelected) Modifier.border(2.dp, CyberPrimary, RoundedCornerShape(14.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(task.category, fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.height(26.dp)
                    )
                    Text(
                        text = "•  ${task.client}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.clientEmail.isNotBlank()) {
                        Text(
                            text = "• ✉ ${task.clientEmail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                StatusBadge(status = task.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = task.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(CyberSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = CyberSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${task.workerName} • ${task.workerRole}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$${task.estimatedValue.toInt()}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyberTertiary
                )
            }

            if (task.status == "RUNNING" || task.status == "APPROVED") {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = CyberSecondary
                )
            }

            if (task.status == "REJECTED" && !task.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberError.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Reason: ${task.rejectionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberError
                    )
                }
            }

            if (task.logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { expandedLogs = !expandedLogs }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Worker Logs (${task.logs.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberSecondary
                    )
                    Icon(
                        imageVector = if (expandedLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CyberSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (expandedLogs) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        task.logs.forEach { log ->
                            Text(
                                text = "▸ $log",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (task.status == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("approve_button_${task.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("reject_button_${task.id}"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CyberError
                        ),
                        border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.horizontalGradient(listOf(CyberError, CyberError))),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (task.status == "APPROVED" || task.status == "RUNNING") {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberTertiary.copy(alpha = 0.2f),
                        contentColor = CyberTertiary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Complete Deliverable", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "PENDING" -> Triple(CyberWarning.copy(alpha = 0.2f), CyberWarning, "PENDING")
        "APPROVED" -> Triple(CyberSecondary.copy(alpha = 0.2f), CyberSecondary, "APPROVED")
        "RUNNING" -> Triple(CyberPrimary.copy(alpha = 0.2f), CyberPrimaryLight, "RUNNING")
        "COMPLETED" -> Triple(CyberTertiary.copy(alpha = 0.2f), CyberTertiary, "COMPLETED")
        "REJECTED" -> Triple(CyberError.copy(alpha = 0.2f), CyberError, "REJECTED")
        else -> Triple(Color.Gray.copy(alpha = 0.2f), Color.LightGray, status)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            fontSize = 10.sp
        )
    }
}

@Composable
fun TaskDetailPane(
    task: Task?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onComplete: () -> Unit,
    onClose: () -> Unit,
    viewModel: DashboardViewModel
) {
    if (task == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Select a task to inspect",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "View worker telemetry, execution logs, and full client proposal brief.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        var generatedVoiceNote by remember(task.id) { mutableStateOf<GeneratedVoiceNote?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = task.status)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close pane")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Client: ${task.client} • Category: ${task.category}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (task.clientEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Client Email: ${task.clientEmail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Assigned AI Agent", style = MaterialTheme.typography.labelSmall)
                        Text(task.workerName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(task.workerRole, style = MaterialTheme.typography.bodySmall, color = CyberSecondary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Estimated Value", style = MaterialTheme.typography.labelSmall)
                        Text("$${task.estimatedValue.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = CyberTertiary)
                        Text("Priority: ${task.priority}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Task Specifications & Brief", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = task.details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            VoiceNoteSection(
                task = task,
                viewModel = viewModel,
                voiceNote = generatedVoiceNote,
                onVoiceNoteGenerated = { generatedVoiceNote = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                var recipientEmail by remember(task.id, task.clientEmail) { mutableStateOf(task.clientEmail) }
                var isSendingEmail by remember { mutableStateOf(false) }
                var emailStatusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = CyberTertiary, modifier = Modifier.size(18.dp))
                        Text(
                             text = "Client Proposal Email (Brevo API)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = recipientEmail,
                        onValueChange = { recipientEmail = it },
                        label = { Text("Recipient Email") },
                         placeholder = { Text("client@company.com") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recipient_email_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (recipientEmail.isNotBlank()) {
                                    isSendingEmail = true
                                    emailStatusMessage = null
                                    viewModel.sendTaskEmail(task, recipientEmail, generatedVoiceNote) { success, msg ->
                                        isSendingEmail = false
                                        emailStatusMessage = Pair(success, msg)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("send_email_button"),
                            enabled = !isSendingEmail && recipientEmail.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTertiary)
                        ) {
                            if (isSendingEmail) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sending...")
                            } else {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Email" + if (generatedVoiceNote != null) " + Voice Note" else "")
                            }
                        }
                    }

                    emailStatusMessage?.let { (success, msg) ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (success) CyberTertiary else CyberError
                        )
                    }
                }
            }

            if (task.logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Worker Console & Execution Telemetry", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CyberSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0F1D), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    task.logs.forEach { log ->
                        Text(
                            text = "⚡ $log",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = Color(0xFF67E8F9),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (task.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve Task")
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberError)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject Task")
                    }
                }
            } else if (task.status == "APPROVED" || task.status == "RUNNING") {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTertiary)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark Completed & Archive")
                }
            }
        }
    }
}

@Composable
fun RejectReasonDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirmReject: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("Scope out of freelance boundary") }
    var customReason by remember { mutableStateOf("") }

    val presetReasons = listOf(
        "Scope out of freelance boundary",
        "Client budget under threshold ($1000 min)",
        "Unverified client reputation / spam risk",
        "Overloaded worker queue / timeline conflict"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Reject AI Worker Task", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select or enter a reason for rejecting '${task.title}':",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))

                presetReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedReason = reason }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = reason, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customReason,
                    onValueChange = {
                        customReason = it
                        if (it.isNotBlank()) selectedReason = "custom"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Or custom reason") },
                    placeholder = { Text("Describe reason...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReason == "custom" || customReason.isNotBlank()) {
                        customReason.ifBlank { "Rejected by director" }
                    } else {
                        selectedReason
                    }
                    onConfirmReject(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberError)
            ) {
                Text("Confirm Reject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        category: String,
        worker: String,
        role: String,
        client: String,
        clientEmail: String,
        value: Double,
        details: String,
        priority: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Client Acquisition") }
    var workerName by remember { mutableStateOf("Apollo-Scout") }
    var workerRole by remember { mutableStateOf("Lead Generation Specialist") }
    var client by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var estimatedValueStr by remember { mutableStateOf("1500") }
    var details by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("HIGH") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("New AI Worker Task", fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    title = "Delivering SEO technical audit & keyword clusters"
                    category = "Deliverable"
                    workerName = "ReportBot-Pro"
                    workerRole = "Technical SEO Analyst"
                    client = "NordicSaaS Inc."
                    clientEmail = "contact@nordicsaas.example"
                    estimatedValueStr = "2800"
                    details = "Audit sitemap indexation, crawl errors, high-intent backlink opportunities and site speed."
                    priority = "HIGH"
                }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Autocomplete", tint = CyberSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Scouting for SEO clients on LinkedIn") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = client,
                    onValueChange = { client = it },
                    label = { Text("Client / Target *") },
                    placeholder = { Text("e.g. Series A Tech Startups") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = clientEmail,
                    onValueChange = { clientEmail = it },
                    label = { Text("Client Email (Optional)") },
                    placeholder = { Text("e.g. contact@clientcompany.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_email_input"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = estimatedValueStr,
                        onValueChange = { estimatedValueStr = it },
                        label = { Text("Est. Value ($)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = workerName,
                        onValueChange = { workerName = it },
                        label = { Text("AI Worker") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = workerRole,
                        onValueChange = { workerRole = it },
                        label = { Text("Worker Role") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Execution Details") },
                    placeholder = { Text("Provide prompt instructions or deliverable scope...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val value = estimatedValueStr.toDoubleOrNull() ?: 1000.0
                        onSubmit(title, category, workerName, workerRole, client, clientEmail, value, details, priority)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
            ) {
                Text("Queue Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun VoiceNoteSection(
    task: Task,
    viewModel: DashboardViewModel,
    voiceNote: GeneratedVoiceNote?,
    onVoiceNoteGenerated: (GeneratedVoiceNote?) -> Unit
) {
    val context = LocalContext.current
    var isGenerating by remember { mutableStateOf(false) }
    var voiceErrorMsg by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(voiceNote) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1B4B).copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(CyberPrimary, CyberSecondary))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = CyberPrimaryLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "AI Voice Note for Outreach",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "ElevenLabs Neural Audio Pitch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                if (voiceNote != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberTertiary.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Ready (${voiceNote.fileSizeKb} KB)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyberTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (voiceNote == null) {
                Button(
                    onClick = {
                        val textToConvert = task.details.ifBlank {
                            "Hello ${task.client}, this is an automated agency proposal regarding ${task.title}. We have identified key growth opportunities for your brand."
                        }
                        isGenerating = true
                        voiceErrorMsg = null
                        viewModel.generateVoiceNoteForText(
                            context = context,
                            text = textToConvert,
                            outputFileName = "pitch_${task.id.take(8)}"
                        ) { success, generated, msg ->
                            isGenerating = false
                            if (success && generated != null) {
                                onVoiceNoteGenerated(generated)
                            } else {
                                voiceErrorMsg = msg
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_voice_note_button"),
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synthesizing MP3 Voice Note...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Voice Note (ElevenLabs)")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (isPlaying) {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                isPlaying = false
                            } else {
                                try {
                                    mediaPlayer?.release()
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(voiceNote.file.absolutePath)
                                        prepare()
                                        setOnCompletionListener { isPlaying = false }
                                        start()
                                    }
                                    isPlaying = true
                                } catch (e: Exception) {
                                    voiceErrorMsg = "Playback error: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) CyberError else CyberTertiary
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPlaying) "Stop Preview" else "Play Voice Note")
                    }

                    OutlinedButton(
                        onClick = {
                            mediaPlayer?.release()
                            mediaPlayer = null
                            isPlaying = false
                            onVoiceNoteGenerated(null)
                        }
                    ) {
                        Text("Re-record")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "✓ Attached as MP3 audio in proposal email dispatch",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberSecondary,
                    fontSize = 11.sp
                )
            }

            voiceErrorMsg?.let { err ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberError
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JarvisVisualIndicatorDialog(
    voiceState: VoiceCommandState,
    lastRecognizedText: String,
    onDismiss: () -> Unit,
    onManualCommand: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CyberSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = CyberSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text("Jarvis Voice Commands", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            when (voiceState) {
                                is VoiceCommandState.Listening -> CyberSecondary.copy(alpha = 0.15f)
                                is VoiceCommandState.Processing -> CyberPrimary.copy(alpha = 0.15f)
                                is VoiceCommandState.ActionExecuted -> CyberTertiary.copy(alpha = 0.15f)
                                is VoiceCommandState.Error -> CyberError.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .border(
                            width = 2.dp,
                            color = when (voiceState) {
                                is VoiceCommandState.Listening -> CyberSecondary
                                is VoiceCommandState.Processing -> CyberPrimary
                                is VoiceCommandState.ActionExecuted -> CyberTertiary
                                is VoiceCommandState.Error -> CyberError
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (voiceState) {
                            is VoiceCommandState.Listening -> Icons.Default.Mic
                            is VoiceCommandState.Processing -> Icons.Default.Sync
                            is VoiceCommandState.ActionExecuted -> Icons.Default.CheckCircle
                            is VoiceCommandState.Error -> Icons.Default.Cancel
                            else -> Icons.Default.Mic
                        },
                        contentDescription = null,
                        tint = when (voiceState) {
                            is VoiceCommandState.Listening -> CyberSecondary
                            is VoiceCommandState.Processing -> CyberPrimary
                            is VoiceCommandState.ActionExecuted -> CyberTertiary
                            is VoiceCommandState.Error -> CyberError
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when (voiceState) {
                        is VoiceCommandState.Listening -> "Listening... Speak clearly into the microphone"
                        is VoiceCommandState.Processing -> "Interpreting voice command..."
                        is VoiceCommandState.ActionExecuted -> voiceState.feedback
                        is VoiceCommandState.Error -> voiceState.message
                        else -> "Jarvis Standby"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center,
                    color = when (voiceState) {
                        is VoiceCommandState.Listening -> CyberSecondary
                        is VoiceCommandState.ActionExecuted -> CyberTertiary
                        is VoiceCommandState.Error -> CyberError
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                if (lastRecognizedText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"$lastRecognizedText\"",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Or tap a quick command:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { onManualCommand("Approve all tasks") },
                        label = { Text("Approve all tasks") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = { onManualCommand("Find more leads") },
                        label = { Text("Find more leads") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = { onManualCommand("Open Inbox") },
                        label = { Text("Open Inbox") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
            ) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HotLeadsDashboardView(
    hotLeads: List<InboxEmail>,
    isLoading: Boolean,
    onScanWithGemini: () -> Unit,
    onSyncImap: () -> Unit,
    onConvertToTask: (InboxEmail) -> Unit,
    onViewInInbox: () -> Unit,
    onSendPaidOffer: ((InboxEmail) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hot_leads_banner"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberWarning.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CyberWarning.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔥", fontSize = 18.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Verified Hot Leads",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Gemini-Parsed Client Intent • Spam & Bots Filtered",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CyberWarning.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberWarning.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${hotLeads.size} Active",
                            color = CyberWarning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Incoming emails are parsed by Gemini for real hiring intent (Shopify fixes, traffic drop rescue, audits, budget quotes). Automated SEO tools, UptimeRobot, self-replies & newsletters are suppressed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onScanWithGemini,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hot_leads_scan_gemini_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan with Gemini", fontSize = 12.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onSyncImap,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hot_leads_sync_imap_button"),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync IMAP Inbox", fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }

        if (hotLeads.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("hot_leads_empty_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🛡️", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Spam Shield Active: 0 Hot Leads Found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Generic automated audit spam, bounce notices, and robot alerts are automatically blocked. When a prospective client emails seeking SEO help or asking for a quote, it will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onScanWithGemini,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyze Inbox for Hot Leads")
                    }
                }
            }
        } else {
            hotLeads.forEach { email ->
                HotLeadItemCard(
                    email = email,
                    onConvertToTask = { onConvertToTask(email) },
                    onViewInInbox = onViewInInbox,
                    onSendPaidOffer = onSendPaidOffer?.let { callback -> { callback(email) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HotLeadItemCard(
    email: InboxEmail,
    onConvertToTask: () -> Unit,
    onViewInInbox: () -> Unit,
    onSendPaidOffer: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hot_lead_card_${email.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberWarning.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = email.senderName.ifBlank { "Prospective Client" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = email.senderEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberPrimaryLight
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CyberWarning.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberWarning)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🔥", fontSize = 12.sp)
                        Text(
                            text = "${email.conversionProbability}/10 Hot",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberWarning
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (email.clientIntentCategory.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberPrimary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberPrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = email.clientIntentCategory,
                            fontSize = 11.sp,
                            color = CyberPrimaryLight,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                email.jobKeywords.forEach { keyword ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "#$keyword",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = email.subject.ifBlank { "(No Subject)" },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = email.incomingMessage.take(280) + if (email.incomingMessage.length > 280) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 18.sp
                )
            }

            if (email.intentSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberPrimaryLight, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = email.intentSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberPrimaryLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onSendPaidOffer != null) {
                    Button(
                        onClick = onSendPaidOffer,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("hot_lead_send_offer_button_${email.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberTertiary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("💰 Send $797 Offer", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }

                Button(
                    onClick = onConvertToTask,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("hot_lead_convert_button_${email.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("To Deal Task", fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onViewInInbox,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("hot_lead_inbox_button_${email.id}"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Inbox", fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun RealDollarRevenueCard(
    totalEarnedRevenue: Double,
    pendingInvoicesValue: Double,
    paidClientsCount: Int,
    onOpenDealCloser: () -> Unit,
    onOpenRecordPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 } }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("real_dollar_revenue_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberTertiary.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CyberTertiary.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("💵", fontSize = 18.sp)
                        }
                    }
                    Column {
                        Text(
                            text = "Real Dollar Revenue & Cash Invoicing",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Direct Stripe / PayPal / USDT Checkout Invoicing",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberTertiary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CyberTertiary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberTertiary)
                ) {
                    Text(
                        text = "$paidClientsCount Paid Clients",
                        color = CyberTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CyberTertiary.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberTertiary.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Collected Cash ($)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberTertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormat.format(totalEarnedRevenue),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = CyberTertiary
                        )
                        Text(
                            text = "Real Bank / Wallet Deposits",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CyberWarning.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberWarning.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Invoiced Out ($)",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberWarning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormat.format(pendingInvoicesValue),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = CyberWarning
                        )
                        Text(
                            text = "Awaiting Client Payment",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenDealCloser,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("btn_quick_deal_closer"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTertiary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Paid Invoice ($)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpenRecordPayment,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_record_payment"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberTertiary)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberTertiary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Cash ($)", color = CyberTertiary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InstantDealCloserDialog(
    initialClient: CrmClient?,
    allClients: List<CrmClient>,
    agencyName: String,
    paymentLinkUrl: String,
    onDismiss: () -> Unit,
    onSendOffer: (CrmClient, ServiceTier, String) -> Unit,
    onRecordPaid: (CrmClient, Double) -> Unit
) {
    var selectedClient by remember { mutableStateOf(initialClient ?: allClients.firstOrNull()) }
    var selectedTier by remember { mutableStateOf(PaidDealEngine.TIERS[1]) }
    var customPriceText by remember { mutableStateOf("") }
    var customNote by remember { mutableStateOf("") }
    val context = LocalContext.current

    val activeTier = remember(selectedTier, customPriceText) {
        val customVal = customPriceText.toDoubleOrNull()
        if (customVal != null && customVal > 0) {
            selectedTier.copy(priceUsd = customVal, title = "${selectedTier.title} ($${customVal.toInt()})")
        } else {
            selectedTier
        }
    }

    val previewProposal = remember(selectedClient, activeTier, customNote, agencyName, paymentLinkUrl) {
        PaidDealEngine.buildClosingProposal(
            clientName = selectedClient?.name ?: "Valued Client",
            websiteUrl = selectedClient?.websiteUrl ?: "your website",
            agencyName = agencyName,
            tier = activeTier,
            paymentLinkUrl = paymentLinkUrl,
            customNote = customNote
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("💰", fontSize = 20.sp)
                Text("Close Deal & Dispatch Paid Proposal ($)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Client / Lead:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                if (allClients.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        allClients.take(5).forEach { c ->
                            FilterChip(
                                selected = selectedClient?.id == c.id,
                                onClick = { selectedClient = c },
                                label = { Text(c.name.ifBlank { c.email }.take(18), fontSize = 11.sp) }
                            )
                        }
                    }
                } else {
                    Text("No CRM clients found yet. Run Lead Scout or sync inbox to add prospects.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Choose High-Ticket Service Package:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaidDealEngine.TIERS.forEach { tier ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTier = tier },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTier.id == tier.id) CyberTertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedTier.id == tier.id) CyberTertiary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tier.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(tier.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CyberTertiary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        "$${tier.priceUsd.toInt()}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = CyberTertiary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customPriceText,
                    onValueChange = { customPriceText = it },
                    label = { Text("Custom Price ($ USD, Optional)") },
                    placeholder = { Text("e.g. 500") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = customNote,
                    onValueChange = { customNote = it },
                    label = { Text("Custom Deliverable / Note (Optional)") },
                    placeholder = { Text("e.g. Including free Shopify schema audit...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 2
                )

                Text("Live Proposal & Payment Checkout Preview:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = previewProposal,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = selectedClient ?: allClients.firstOrNull() ?: CrmClient(name = "Client Prospect", email = "prospect@example.com")
                    onSendOffer(target, activeTier, customNote)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberTertiary),
                modifier = Modifier.testTag("btn_confirm_send_paid_offer")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dispatch Offer ($${activeTier.priceUsd.toInt()})", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Deal Offer", previewProposal)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied proposal & payment link to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy Link")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordPaymentDialog(
    initialClient: CrmClient?,
    allClients: List<CrmClient>,
    onDismiss: () -> Unit,
    onConfirmPayment: (CrmClient, Double, String) -> Unit
) {
    var selectedClient by remember { mutableStateOf(initialClient ?: allClients.firstOrNull()) }
    var amountText by remember { mutableStateOf(selectedClient?.let { if (it.proposalPrice > 0) it.proposalPrice.toInt().toString() else "797" } ?: "797") }
    var selectedPaymentMethod by remember { mutableStateOf("Stripe Checkout") }

    val methods = listOf("Stripe Checkout", "PayPal", "Wise", "Payoneer", "USDT Crypto", "Direct Bank Wire")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎉", fontSize = 22.sp)
                Text("Record Real Dollar Payment ($)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Confirm client payment deposit. This will mark the client as Closed Won, log the transaction, and add the dollars to your Real Earned Cash balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Select Paying Client:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                if (allClients.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        allClients.take(5).forEach { c ->
                            FilterChip(
                                selected = selectedClient?.id == c.id,
                                onClick = {
                                    selectedClient = c
                                    if (c.proposalPrice > 0) {
                                        amountText = c.proposalPrice.toInt().toString()
                                    }
                                },
                                label = { Text(c.name.ifBlank { c.email }.take(18), fontSize = 11.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount ($ USD)") },
                    leadingIcon = { Text("$", fontWeight = FontWeight.Bold, color = CyberTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                Text("Payment Method / Channel:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    methods.forEach { m ->
                        FilterChip(
                            selected = selectedPaymentMethod == m,
                            onClick = { selectedPaymentMethod = m },
                            label = { Text(m, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = selectedClient ?: allClients.firstOrNull() ?: CrmClient(name = "Client Prospect")
                    val amt = amountText.toDoubleOrNull() ?: 797.0
                    onConfirmPayment(target, amt, selectedPaymentMethod)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberTertiary),
                modifier = Modifier.testTag("btn_confirm_record_payment")
            ) {
                Text("Confirm Deposit ($$amountText)", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
