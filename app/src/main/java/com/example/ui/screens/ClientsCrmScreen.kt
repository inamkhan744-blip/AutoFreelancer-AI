package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
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
import com.example.data.model.DealPipelineStage
import com.example.data.service.FollowUpSequenceEngine
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberPrimaryLight
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTertiary
import com.example.ui.theme.CyberWarning
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
    var selectedStageFilter by remember { mutableStateOf("ALL") }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedClientForEmail by remember { mutableStateOf<CrmClient?>(null) }
    var selectedClientForProposal by remember { mutableStateOf<CrmClient?>(null) }
    var selectedClientForPayment by remember { mutableStateOf<CrmClient?>(null) }
    var selectedClientForTwilio by remember { mutableStateOf<CrmClient?>(null) }

    // Email dispatch state
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }
    var recipientEmail by remember { mutableStateOf("") }
    var isSendingEmail by remember { mutableStateOf(false) }

    // Client filtering across 8 stages
    val filteredClients = remember(clients, searchQuery, selectedStageFilter) {
        clients.filter { client ->
            val matchesSearch = searchQuery.isBlank() ||
                client.name.contains(searchQuery, ignoreCase = true) ||
                client.company.contains(searchQuery, ignoreCase = true) ||
                client.email.contains(searchQuery, ignoreCase = true) ||
                client.websiteUrl.contains(searchQuery, ignoreCase = true)

            val clientNormalizedStage = DealPipelineStage.normalize(client.pipelineStage)
            val matchesStage = when (selectedStageFilter) {
                "ALL" -> true
                else -> clientNormalizedStage.equals(selectedStageFilter, ignoreCase = true)
            }

            matchesSearch && matchesStage
        }
    }

    // Pipeline counts
    val pitchedCount = clients.count { DealPipelineStage.normalize(it.pipelineStage) == DealPipelineStage.PITCHED }
    val followUpCount = clients.count { DealPipelineStage.normalize(it.pipelineStage) == DealPipelineStage.FOLLOW_UP }
    val interestedCount = clients.count { DealPipelineStage.normalize(it.pipelineStage) == DealPipelineStage.INTERESTED }
    val wonCount = clients.count { DealPipelineStage.normalize(it.pipelineStage) == DealPipelineStage.WON }

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
                            text = "Deal Pipeline CRM",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${clients.size} deals in pipeline • 8 stages active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.exportDataToJson(context) { success, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        modifier = Modifier.testTag("export_crm_data_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Export CRM Data (JSON)",
                            tint = CyberTertiary
                        )
                    }

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
                        CrmMetricItem(label = "Total Deals", count = clients.size, color = CyberPrimary)
                        CrmMetricItem(label = "Pitched", count = pitchedCount, color = CyberSecondary)
                        CrmMetricItem(label = "Follow-Ups", count = followUpCount, color = CyberWarning)
                        CrmMetricItem(label = "Interested", count = interestedCount, color = CyberPrimaryLight)
                        CrmMetricItem(label = "Closed / Won", count = wonCount, color = CyberSuccess)
                    }
                }
            }

            // Search and 8-Stage Pipeline Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crm_search_input"),
                        placeholder = { Text("Search clients by name, company, email, site...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search Leads")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // 8-Stage Horizontal Pipeline Filter Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedStageFilter == "ALL",
                            onClick = { selectedStageFilter = "ALL" },
                            label = { Text("All (${clients.size})", fontSize = 11.sp) },
                            modifier = Modifier.testTag("client_stage_filter_all"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyberPrimary
                            )
                        )
                        DealPipelineStage.ALL.forEach { stage ->
                            val stageCount = clients.count { DealPipelineStage.normalize(it.pipelineStage) == stage }
                            FilterChip(
                                selected = selectedStageFilter == stage,
                                onClick = { selectedStageFilter = stage },
                                label = { Text("$stage ($stageCount)", fontSize = 11.sp) },
                                modifier = Modifier.testTag("client_stage_filter_${stage.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = stageColor(stage).copy(alpha = 0.2f),
                                    selectedLabelColor = stageColor(stage)
                                )
                            )
                        }
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
                                text = if (searchQuery.isNotBlank() || selectedStageFilter != "ALL")
                                    "No clients match the current stage filter."
                                else
                                    "No client leads in pipeline yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Run Apollo-Scout on Tasks screen or add a prospect manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            OutlinedButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.testTag("empty_state_add_client_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Lead")
                            }
                        }
                    }
                }
            } else {
                items(filteredClients, key = { it.id }) { client ->
                    ClientPipelineCard(
                        client = client,
                        onStageChange = { newStage ->
                            viewModel.updateClientPipelineStage(client.id, newStage)
                            scope.launch {
                                snackbarHostState.showSnackbar("Moved ${client.name} to $newStage")
                            }
                        },
                        onGenerateProposalClicked = {
                            selectedClientForProposal = client
                        },
                        onGenerateReport = {
                            viewModel.generateSeoReportForClient(client) { success, result, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        onRequestPaymentClicked = {
                            selectedClientForPayment = client
                        },
                        onSendTwilioClicked = {
                            selectedClientForTwilio = client
                        },
                        onOpenPortalClicked = {
                            viewModel.openClientPortal(client)
                        },
                        onSendEmailClicked = {
                            selectedClientForEmail = client
                            recipientEmail = client.email
                            emailSubject = "SEO Audit & Growth Deliverable for ${client.name}"
                            emailBody = if (client.seoReport.isNotBlank()) client.seoReport else client.pitchMessage
                        },
                        onTriggerSequence = { stage ->
                            viewModel.sendFollowUpSequenceNow(client, stage) { success, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        onCopyContent = { text, label ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
                            Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Dialog: Add Manual Prospect
    if (showAddDialog) {
        var clientName by remember { mutableStateOf("") }
        var clientCompany by remember { mutableStateOf("") }
        var clientWebsite by remember { mutableStateOf("") }
        var clientEmail by remember { mutableStateOf("") }
        var clientPhone by remember { mutableStateOf("") }
        var clientStage by remember { mutableStateOf(DealPipelineStage.NEW) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Prospect to Pipeline") },
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
                        label = { Text("Contact Name") },
                        modifier = Modifier.fillMaxWidth().testTag("crm_add_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientCompany,
                        onValueChange = { clientCompany = it },
                        label = { Text("Company / Business") },
                        modifier = Modifier.fillMaxWidth().testTag("crm_add_company_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientWebsite,
                        onValueChange = { clientWebsite = it },
                        label = { Text("Website URL (e.g. https://domain.com)") },
                        modifier = Modifier.fillMaxWidth().testTag("crm_add_website_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientEmail,
                        onValueChange = { clientEmail = it },
                        label = { Text("Client Email") },
                        modifier = Modifier.fillMaxWidth().testTag("crm_add_email_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientPhone,
                        onValueChange = { clientPhone = it },
                        label = { Text("Phone / WhatsApp (e.g. +1234567890)") },
                        modifier = Modifier.fillMaxWidth().testTag("crm_add_phone_input"),
                        singleLine = true
                    )

                    Text(text = "Initial Pipeline Stage:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(DealPipelineStage.NEW, DealPipelineStage.PITCHED, DealPipelineStage.INTERESTED).forEach { stage ->
                            FilterChip(
                                selected = clientStage == stage,
                                onClick = { clientStage = stage },
                                label = { Text(stage, fontSize = 11.sp) }
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
                                phone = clientPhone.trim(),
                                status = clientStage,
                                pipelineStage = clientStage,
                                leadSource = "Direct Manual Lead"
                            )
                            viewModel.upsertClient(newClient)
                            showAddDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Prospect added to Pipeline CRM")
                            }
                        }
                    },
                    modifier = Modifier.testTag("crm_submit_add_client")
                ) {
                    Text("Save Prospect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Generate Formal Proposal & Quote
    selectedClientForProposal?.let { client ->
        var quoteAmountText by remember { mutableStateOf(if (client.proposalPrice > 0) client.proposalPrice.toString() else "850") }
        var isGeneratingProposal by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedClientForProposal = null },
            title = { Text("Generate Proposal & Quote") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "The AI Architect will synthesize a formal growth proposal deliverable for ${client.name} (${client.company.ifBlank { client.websiteUrl }}).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = quoteAmountText,
                        onValueChange = { quoteAmountText = it.filter { char -> char.isDigit() } },
                        label = { Text("Quoted Investment ($ USD)") },
                        modifier = Modifier.fillMaxWidth().testTag("proposal_quote_amount_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = quoteAmountText.toIntOrNull() ?: 850
                        isGeneratingProposal = true
                        viewModel.generateClientProposalDeliverable(client, amount) { success, proposal, msg ->
                            isGeneratingProposal = false
                            selectedClientForProposal = null
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    },
                    enabled = !isGeneratingProposal,
                    modifier = Modifier.testTag("confirm_generate_proposal_button")
                ) {
                    if (isGeneratingProposal) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Synthesizing...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Proposal")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedClientForProposal = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Request Payment
    selectedClientForPayment?.let { client ->
        var paymentAmountText by remember { mutableStateOf(if (client.proposalPrice > 0) client.proposalPrice.toString() else "500") }
        var isRequesting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedClientForPayment = null },
            title = { Text("Request Client Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Send an invoice and secure Stripe/Gumroad checkout link to ${client.email}.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it.filter { char -> char.isDigit() } },
                        label = { Text("Invoice Amount ($ USD)") },
                        modifier = Modifier.fillMaxWidth().testTag("payment_request_amount_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountText.toIntOrNull() ?: 500
                        isRequesting = true
                        viewModel.requestPaymentFromClient(client, amount) { success, msg ->
                            isRequesting = false
                            selectedClientForPayment = null
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    },
                    enabled = !isRequesting,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSuccess),
                    modifier = Modifier.testTag("confirm_request_payment_button")
                ) {
                    if (isRequesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Payment Request")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedClientForPayment = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Twilio WhatsApp / SMS Outreach
    selectedClientForTwilio?.let { client ->
        var messageText by remember {
            mutableStateOf("Hi ${client.name}, quick follow-up from our team regarding your website speed and organic traffic audit.")
        }
        var isWhatsApp by remember { mutableStateOf(false) }
        var isSendingTwilio by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedClientForTwilio = null },
            title = { Text("Multi-Channel Outreach (Twilio)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Send an SMS or WhatsApp message when email outreach has been ignored.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isWhatsApp,
                            onClick = { isWhatsApp = false },
                            label = { Text("SMS Text") }
                        )
                        FilterChip(
                            selected = isWhatsApp,
                            onClick = { isWhatsApp = true },
                            label = { Text("WhatsApp") }
                        )
                    }
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("Message Body") },
                        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("twilio_message_input"),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSendingTwilio = true
                        viewModel.sendTwilioMessage(client, messageText, isWhatsApp) { success, msg ->
                            isSendingTwilio = false
                            selectedClientForTwilio = null
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    },
                    enabled = !isSendingTwilio,
                    modifier = Modifier.testTag("confirm_send_twilio_button")
                ) {
                    if (isSendingTwilio) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send via Twilio")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedClientForTwilio = null }) {
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
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = recipientEmail,
                        onValueChange = { recipientEmail = it },
                        label = { Text("Recipient Email") },
                        modifier = Modifier.fillMaxWidth().testTag("crm_email_recipient_input"),
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
                        modifier = Modifier.fillMaxWidth().height(180.dp),
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

private fun stageColor(stage: String): Color {
    return when (DealPipelineStage.normalize(stage)) {
        DealPipelineStage.NEW -> CyberPrimary
        DealPipelineStage.PITCHED -> CyberSecondary
        DealPipelineStage.FOLLOW_UP -> CyberWarning
        DealPipelineStage.INTERESTED -> CyberPrimaryLight
        DealPipelineStage.CALL_BOOKED -> CyberTertiary
        DealPipelineStage.PROPOSAL_SENT -> CyberSecondary
        DealPipelineStage.WON -> CyberSuccess
        DealPipelineStage.LOST -> CyberError
        else -> CyberPrimary
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientPipelineCard(
    client: CrmClient,
    onStageChange: (String) -> Unit,
    onGenerateProposalClicked: () -> Unit,
    onGenerateReport: () -> Unit,
    onRequestPaymentClicked: () -> Unit,
    onSendTwilioClicked: () -> Unit,
    onOpenPortalClicked: () -> Unit,
    onSendEmailClicked: () -> Unit,
    onTriggerSequence: (FollowUpSequenceEngine.FollowUpStage) -> Unit,
    onCopyContent: (String, String) -> Unit
) {
    var stageDropdownOpen by remember { mutableStateOf(false) }
    var sequenceMenuOpen by remember { mutableStateOf(false) }
    var expandedPitch by remember { mutableStateOf(false) }
    var expandedProposal by remember { mutableStateOf(false) }
    var expandedReport by remember { mutableStateOf(false) }

    val normalizedStage = DealPipelineStage.normalize(client.pipelineStage)
    val color = stageColor(normalizedStage)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("client_card_${client.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Name & Interactive Stage Badge
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
                            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = client.company, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.15f))
                            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { stageDropdownOpen = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("client_status_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = normalizedStage,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = color
                            )
                            Icon(Icons.Default.MoreVert, contentDescription = "Move Stage", modifier = Modifier.size(14.dp), tint = color)
                        }
                    }

                    DropdownMenu(
                        expanded = stageDropdownOpen,
                        onDismissRequest = { stageDropdownOpen = false }
                    ) {
                        DealPipelineStage.ALL.forEach { stageOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stageOption,
                                        fontWeight = if (stageOption == normalizedStage) FontWeight.Bold else FontWeight.Normal,
                                        color = stageColor(stageOption)
                                    )
                                },
                                onClick = {
                                    onStageChange(stageOption)
                                    stageDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            // Contact & Meta details: Website, Email, Sequence Cadence & A/B Variant
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (client.websiteUrl.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Language, contentDescription = "Website", modifier = Modifier.size(14.dp), tint = CyberPrimaryLight)
                        Text(text = client.websiteUrl, style = MaterialTheme.typography.bodySmall, color = CyberPrimaryLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (client.email.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Email, contentDescription = "Email", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = client.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Sequence & A/B Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sequence: ${client.sequenceStage.ifBlank { "DAY_1" }} • A/B: Variant ${client.abVariant.ifBlank { "A" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (client.proposalPrice > 0) {
                        Text(
                            text = "Quote: $${client.proposalPrice}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyberSuccess
                        )
                    }
                }
            }

            // Core Action Buttons
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Generate Proposal & Quote Button
                Button(
                    onClick = onGenerateProposalClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberPrimary.copy(alpha = 0.15f),
                        contentColor = CyberPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("generate_proposal_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (client.proposalHtml.isNotBlank()) "Update Proposal" else "Generate Proposal & Quote", fontSize = 12.sp)
                }

                // Request Payment Button
                Button(
                    onClick = onRequestPaymentClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSuccess.copy(alpha = 0.15f),
                        contentColor = CyberSuccess
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("request_payment_button")
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Request Payment", fontSize = 12.sp)
                }

                // View Client Portal Button
                Button(
                    onClick = onOpenPortalClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberTertiary.copy(alpha = 0.15f),
                        contentColor = CyberTertiary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("client_portal_button")
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Client Portal", fontSize = 12.sp)
                }

                // Twilio SMS / WhatsApp Outreach Button
                Button(
                    onClick = onSendTwilioClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSecondary.copy(alpha = 0.15f),
                        contentColor = CyberSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("twilio_sms_button")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp / SMS", fontSize = 12.sp)
                }

                // Multi-Stage Sequence Menu
                Box {
                    OutlinedButton(
                        onClick = { sequenceMenuOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("sequence_menu_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Follow-Up Cadence", fontSize = 12.sp)
                    }

                    DropdownMenu(
                        expanded = sequenceMenuOpen,
                        onDismissRequest = { sequenceMenuOpen = false }
                    ) {
                        FollowUpSequenceEngine.FollowUpStage.values().forEach { stage ->
                            DropdownMenuItem(
                                text = { Text("${stage.name} (${stage.delayDays}d): ${stage.purpose}") },
                                onClick = {
                                    onTriggerSequence(stage)
                                    sequenceMenuOpen = false
                                }
                            )
                        }
                    }
                }

                // Email Pitch / Report Button
                OutlinedButton(
                    onClick = onSendEmailClicked,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("email_report_button")
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Email Deliverable", fontSize = 12.sp)
                }

                // Generate SEO Report
                if (client.websiteUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = onGenerateReport,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("generate_report_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("5-Point Audit", fontSize = 12.sp)
                    }
                }
            }

            // Expandable Proposal Section
            if (client.proposalHtml.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expandedProposal = !expandedProposal }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Formal Proposal ($${client.proposalPrice} USD)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = CyberPrimary
                    )
                    Icon(
                        imageVector = if (expandedProposal) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CyberPrimary
                    )
                }

                AnimatedVisibility(visible = expandedProposal) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = client.proposalHtml, style = MaterialTheme.typography.bodySmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onCopyContent(client.proposalHtml, "Proposal") }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Proposal")
                                }
                            }
                        }
                    }
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
                        text = "Initial Outreach Pitch",
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = client.pitchMessage, style = MaterialTheme.typography.bodySmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onCopyContent(client.pitchMessage, "Pitch") }) {
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
                    Text(
                        text = "5-Point Technical SEO Audit Report",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyberSuccess
                    )
                    Icon(
                        imageVector = if (expandedReport) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CyberSuccess
                    )
                }

                AnimatedVisibility(visible = expandedReport) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = client.seoReport, style = MaterialTheme.typography.bodySmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    onClick = { onCopyContent(client.seoReport, "SEO Report") },
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
