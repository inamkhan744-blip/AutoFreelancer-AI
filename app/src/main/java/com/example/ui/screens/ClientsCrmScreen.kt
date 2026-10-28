package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CrmClient
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberPrimaryLight
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTertiary
import com.example.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClientsCrmScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val clients by viewModel.clients.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, Pitched, Paid, Completed

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedClientForEmail by remember { mutableStateOf<CrmClient?>(null) }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }
    var recipientEmail by remember { mutableStateOf("") }
    var isSendingEmail by remember { mutableStateOf(false) }

    // Client filtering
    val filteredClients = remember(clients, searchQuery, statusFilter) {
        clients.filter { client ->
            val matchesSearch = searchQuery.isBlank() ||
                client.name.contains(searchQuery, ignoreCase = true) ||
                client.company.contains(searchQuery, ignoreCase = true) ||
                client.email.contains(searchQuery, ignoreCase = true) ||
                client.websiteUrl.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (statusFilter) {
                "Pitched" -> client.status.equals("Pitched", ignoreCase = true)
                "Paid" -> client.status.equals("Paid", ignoreCase = true)
                "Completed" -> client.status.equals("Completed", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesStatus
        }
    }

    // Counts for CRM summary
    val pitchedCount = clients.count { it.status.equals("Pitched", ignoreCase = true) }
    val paidCount = clients.count { it.status.equals("Paid", ignoreCase = true) }
    val completedCount = clients.count { it.status.equals("Completed", ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("clients_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Tasks"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Client CRM Database",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${clients.size} total leads synced with Firebase Firestore",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_client_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Client",
                            tint = CyberPrimaryLight
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CyberPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_client_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Client Lead"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // CRM Pipeline Metric Counters
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        CrmMetricItem(label = "Total Leads", count = clients.size, color = CyberPrimary)
                        CrmMetricItem(label = "Pitched", count = pitchedCount, color = CyberSecondary)
                        CrmMetricItem(label = "Paid", count = paidCount, color = CyberSuccess)
                        CrmMetricItem(label = "Completed", count = completedCount, color = CyberTertiary)
                    }
                }
            }

            // Search and Status Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crm_search_input"),
                        placeholder = { Text("Search clients by name, company, email...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search Leads")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = statusFilter == "ALL",
                            onClick = { statusFilter = "ALL" },
                            label = { Text("All (${clients.size})") },
                            modifier = Modifier.testTag("client_status_filter_all"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyberPrimary
                            )
                        )
                        FilterChip(
                            selected = statusFilter == "Pitched",
                            onClick = { statusFilter = "Pitched" },
                            label = { Text("Pitched ($pitchedCount)") },
                            modifier = Modifier.testTag("client_status_filter_pitched"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberSecondary.copy(alpha = 0.2f),
                                selectedLabelColor = CyberSecondary
                            )
                        )
                        FilterChip(
                            selected = statusFilter == "Paid",
                            onClick = { statusFilter = "Paid" },
                            label = { Text("Paid ($paidCount)") },
                            modifier = Modifier.testTag("client_status_filter_paid"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberSuccess.copy(alpha = 0.2f),
                                selectedLabelColor = CyberSuccess
                            )
                        )
                        FilterChip(
                            selected = statusFilter == "Completed",
                            onClick = { statusFilter = "Completed" },
                            label = { Text("Completed ($completedCount)") },
                            modifier = Modifier.testTag("client_status_filter_completed"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberTertiary.copy(alpha = 0.2f),
                                selectedLabelColor = CyberTertiary
                            )
                        )
                    }
                }
            }

            // Client Cards List
            if (filteredClients.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank() || statusFilter != "ALL")
                                    "No clients match the current filter."
                                else
                                    "No client leads in CRM yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Run Lead Scout on Tasks screen or add a client manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            OutlinedButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.testTag("empty_state_add_client_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Client")
                            }
                        }
                    }
                }
            } else {
                items(filteredClients, key = { it.id }) { client ->
                    ClientCardItem(
                        client = client,
                        onStatusChange = { newStatus ->
                            viewModel.updateClientStatus(client.id, newStatus)
                            scope.launch {
                                snackbarHostState.showSnackbar("Client status updated to $newStatus")
                            }
                        },
                        onGenerateReport = {
                            viewModel.generateSeoReportForClient(client) { success, result, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        onSendEmailClicked = {
                            selectedClientForEmail = client
                            recipientEmail = client.email
                            emailSubject = "SEO Audit & Growth Deliverable for ${client.name}"
                            emailBody = if (client.seoReport.isNotBlank()) client.seoReport else client.pitchMessage
                        },
                        onCopyReport = { report ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SEO Audit Report", report))
                            Toast.makeText(context, "SEO Audit Report copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Dialog: Add Manual Client
    if (showAddDialog) {
        var clientName by remember { mutableStateOf("") }
        var clientCompany by remember { mutableStateOf("") }
        var clientWebsite by remember { mutableStateOf("") }
        var clientEmail by remember { mutableStateOf("") }
        var clientStatus by remember { mutableStateOf("Pitched") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Client to CRM") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Name / Contact") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crm_add_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientCompany,
                        onValueChange = { clientCompany = it },
                        label = { Text("Company / Brand") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crm_add_company_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientWebsite,
                        onValueChange = { clientWebsite = it },
                        label = { Text("Website URL (e.g. https://target.com)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crm_add_website_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientEmail,
                        onValueChange = { clientEmail = it },
                        label = { Text("Client Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crm_add_email_input"),
                        singleLine = true
                    )

                    Text(
                        text = "Initial Status:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Pitched", "Paid", "Completed").forEach { status ->
                            FilterChip(
                                selected = clientStatus == status,
                                onClick = { clientStatus = status },
                                label = { Text(status) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.isNotBlank() || clientWebsite.isNotBlank()) {
                            val newClient = CrmClient(
                                name = clientName.ifBlank { "Client Prospect" },
                                company = clientCompany,
                                websiteUrl = clientWebsite,
                                email = clientEmail.trim(),
                                status = clientStatus,
                                leadSource = "Direct Manual Lead"
                            )
                            viewModel.upsertClient(newClient)
                            showAddDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Client added to CRM & Firestore")
                            }
                        }
                    },
                    modifier = Modifier.testTag("crm_submit_add_client")
                ) {
                    Text("Save Client")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Send Email to Client via Brevo
    selectedClientForEmail?.let { client ->
        AlertDialog(
            onDismissRequest = { selectedClientForEmail = null },
            title = { Text("Send Email via Brevo") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = recipientEmail,
                        onValueChange = { recipientEmail = it },
                        label = { Text("Recipient Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crm_email_recipient_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = emailSubject,
                        onValueChange = { emailSubject = it },
                        label = { Text("Email Subject") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = emailBody,
                        onValueChange = { emailBody = it },
                        label = { Text("Proposal / Deliverable Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (recipientEmail.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Error: Client email is missing. Please provide client email to send proposal.")
                            }
                            return@Button
                        }
                        isSendingEmail = true
                        viewModel.sendEmailToClient(
                            toEmail = recipientEmail,
                            subject = emailSubject,
                            bodyText = emailBody
                        ) { success, msg ->
                            isSendingEmail = false
                            selectedClientForEmail = null
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    },
                    enabled = !isSendingEmail,
                    modifier = Modifier.testTag("crm_confirm_send_email_button")
                ) {
                    if (isSendingEmail) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sending...")
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                         Text("Send via Brevo")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedClientForEmail = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CrmMetricItem(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientCardItem(
    client: CrmClient,
    onStatusChange: (String) -> Unit,
    onGenerateReport: () -> Unit,
    onSendEmailClicked: () -> Unit,
    onCopyReport: (String) -> Unit
) {
    var statusDropdownOpen by remember { mutableStateOf(false) }
    var expandedPitch by remember { mutableStateOf(false) }
    var expandedReport by remember { mutableStateOf(false) }

    val statusColor = when (client.status.lowercase()) {
        "paid" -> CyberSuccess
        "completed" -> CyberTertiary
        else -> CyberSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("client_card_${client.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Name & Status Badge with Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (client.company.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = client.company,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { statusDropdownOpen = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("client_status_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = client.status,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Change Status",
                                modifier = Modifier.size(14.dp),
                                tint = statusColor
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = statusDropdownOpen,
                        onDismissRequest = { statusDropdownOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Pitched") },
                            onClick = {
                                onStatusChange("Pitched")
                                statusDropdownOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Paid") },
                            onClick = {
                                onStatusChange("Paid")
                                statusDropdownOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Completed") },
                            onClick = {
                                onStatusChange("Completed")
                                statusDropdownOpen = false
                            }
                        )
                    }
                }
            }

            // Contact info rows: Website & Email
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (client.websiteUrl.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = "Website",
                            modifier = Modifier.size(14.dp),
                            tint = CyberPrimaryLight
                        )
                        Text(
                            text = client.websiteUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberPrimaryLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (client.email.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Email",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = client.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Buttons: Generate SEO Report & Send Pitch Email
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (client.websiteUrl.isNotBlank()) {
                    Button(
                        onClick = onGenerateReport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberPrimary.copy(alpha = 0.15f),
                            contentColor = CyberPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("generate_report_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (client.seoReport.isNotBlank()) "Regenerate Report" else "Generate Report",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Button(
                    onClick = onSendEmailClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberTertiary.copy(alpha = 0.15f),
                        contentColor = CyberTertiary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("email_report_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Email Pitch / Report",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Expandable Pitch Section
            if (client.pitchMessage.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expandedPitch = !expandedPitch }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Outreach Pitch Message",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (expandedPitch) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = expandedPitch) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = client.pitchMessage,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { onCopyReport(client.pitchMessage) }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Pitch")
                                }
                            }
                        }
                    }
                }
            }

            // Expandable SEO Report Section
            if (client.seoReport.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expandedReport = !expandedReport }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CyberSuccess
                        )
                        Text(
                            text = "5-Point Technical SEO Audit Report",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberSuccess
                        )
                    }
                    Icon(
                        imageVector = if (expandedReport) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CyberSuccess
                    )
                }

                AnimatedVisibility(visible = expandedReport) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = client.seoReport,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onCopyReport(client.seoReport) },
                                    modifier = Modifier.testTag("copy_report_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Report")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
