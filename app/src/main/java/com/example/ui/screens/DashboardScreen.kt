package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Task
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberPrimaryLight
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberTertiary
import com.example.ui.theme.CyberWarning
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.TasksUiState
import com.example.ui.viewmodel.WorkerNotification
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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

    val currentUser = Firebase.auth.currentUser
    val userEmail = currentUser?.email ?: "Freelancer Director"
    val userName = currentUser?.displayName ?: userEmail.substringBefore("@")

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
                            Text(
                                text = "Auto-Freelancer AI",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
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
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & API Keys",
                            tint = CyberTertiary
                        )
                    }
                    IconButton(
                        onClick = onSignOutClicked,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
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
                // Live Notification Banner when AI Worker starts
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

                if (isWideScreen) {
                    // Desktop / Tablet Two-Column Layout
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Column: Metrics + Filters + Task Feed
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
                                onComplete = { viewModel.completeTask(it.id) }
                            )
                        }

                        // Right Column: Detail Inspector / Console
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
                                onClose = { viewModel.selectTaskForDetail(null) }
                            )
                        }
                    }
                } else {
                    // Mobile Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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
                            onComplete = { viewModel.completeTask(it.id) }
                        )
                    }
                }
            }
        }
    }

    // Detail Modal for Mobile if a task is selected
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

    // Rejection Dialog with Reasons
    rejectTargetTask?.let { task ->
        RejectReasonDialog(
            task = task,
            onDismiss = { viewModel.closeRejectDialog() },
            onConfirmReject = { reason ->
                viewModel.rejectTask(task.id, reason)
            }
        )
    }

    // Create New Task Dialog
    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { viewModel.closeCreateTaskDialog() },
            onSubmit = { title, category, worker, role, client, value, details, priority ->
                viewModel.createCustomTask(title, category, worker, role, client, value, details, priority)
            }
        )
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
    onComplete: (Task) -> Unit
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No AI Worker Tasks Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap '+ New AI Task' or the ✨ button to simulate new worker opportunities.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            // Category, Client & Priority Header
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
                }

                StatusBadge(status = task.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Details preview
            Text(
                text = task.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Worker Assigned & Estimated Revenue
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

            // Running status animation
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

            // Rejection reason note if rejected
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

            // Expandable Logs Toggle if present
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

            // Action Buttons
            if (task.status == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Approve Button
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

                    // Reject Button
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
    onClose: () -> Unit
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
                    // Quick autocomplete with creative preset
                    title = "Delivering SEO technical audit & keyword clusters"
                    category = "Deliverable"
                    workerName = "ReportBot-Pro"
                    workerRole = "Technical SEO Analyst"
                    client = "NordicSaaS Inc."
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
                        onSubmit(title, category, workerName, workerRole, client, value, details, priority)
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
