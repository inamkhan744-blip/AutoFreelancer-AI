package com.example.ui.screens

import android.Manifest
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
import com.example.data.model.Task
import com.example.data.service.GeneratedPitch
import com.example.data.service.GeneratedVoiceNote
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
    val settings by viewModel.settings.collectAsState()

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
                            Spacer(modifier = Modifier.height(12.dp))
                            SearchAndFilterBar(
                                searchQuery = searchQuery,
                                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                statusFilter = statusFilter,
                                onFilterChanged = { viewModel.onStatusFilterChanged(it) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        MetricsHeader(metrics = metrics)
                        Spacer(modifier = Modifier.height(12.dp))
                        SearchAndFilterBar(
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                            statusFilter = statusFilter,
                            onFilterChanged = { viewModel.onStatusFilterChanged(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
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
}

@Composable
private fun LiveTerminalPanel(entries: List<WorkerLogEntry>) {
    val scrollState = rememberScrollState()
    LaunchedEffect(entries.lastOrNull()?.id) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(148.dp)
            .testTag("live_worker_terminal"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050907)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D65A).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE WORKER TERMINAL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF00F56A)
                )
                Text(
                    text = "● LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = Color(0xFF55FF91)
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                entries.takeLast(8).forEach { entry ->
                    Text(
                        text = "> [${entry.worker}] ${entry.message}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = Color(0xFF9BFFB8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
    onFilterChanged: (String) -> Unit
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
                "PENDING" to "Pending Approval",
                "ACTIVE" to "Active Workers",
                "COMPLETED" to "Completed",
                "REJECTED" to "Rejected"
            )

            filters.forEach { (key, label) ->
                FilterChip(
                    selected = statusFilter == key,
                    onClick = { onFilterChanged(key) },
                    label = { Text(label, fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CyberPrimaryLight
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
