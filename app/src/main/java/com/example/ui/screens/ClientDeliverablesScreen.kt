package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.data.service.RawSeoData
import com.example.data.service.SeoReportResult
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
fun ClientDeliverablesScreen(
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
    val savedDeliverables = remember(clients) {
        clients.filter { it.seoReport.isNotBlank() }
    }

    // Input States
    var websiteUrlInput by remember { mutableStateOf("") }
    var clientNameInput by remember { mutableStateOf("") }
    var clientEmailInput by remember { mutableStateOf("") }

    // Generation State
    var isGenerating by remember { mutableStateOf(false) }
    var lastReportResult by remember { mutableStateOf<SeoReportResult?>(null) }
    var generationErrorMessage by remember { mutableStateOf<String?>(null) }

    // Email Dispatch Modal State
    var emailDialogOpen by remember { mutableStateOf(false) }
    var targetEmailRecipient by remember { mutableStateOf("") }
    var targetEmailSubject by remember { mutableStateOf("") }
    var targetEmailBody by remember { mutableStateOf("") }
    var isDispatchingEmail by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String, label: String = "SEO Audit Report") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

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
                        modifier = Modifier.testTag("deliverables_back_button")
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
                            text = "Client Deliverables",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "5-Point Technical SEO Audit & AI Synthesizer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // URL & Client Input Generator Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = null,
                                    tint = CyberPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Generate 5-Point SEO Audit",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Live metadata inspection + Gemini AI synthesis",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = websiteUrlInput,
                            onValueChange = { websiteUrlInput = it },
                            label = { Text("Client Website URL") },
                            placeholder = { Text("e.g. https://example.com") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("website_url_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = clientNameInput,
                                onValueChange = { clientNameInput = it },
                                label = { Text("Client Name") },
                                placeholder = { Text("e.g. Acme Corp") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("client_name_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = clientEmailInput,
                                onValueChange = { clientEmailInput = it },
                                label = { Text("Email (Optional)") },
                                placeholder = { Text("contact@site.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("client_email_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val url = websiteUrlInput.trim()
                                if (url.isBlank()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please enter a target website URL.")
                                    }
                                    return@Button
                                }
                                isGenerating = true
                                generationErrorMessage = null
                                viewModel.generateSeoReportForUrl(
                                    url = url,
                                    clientName = clientNameInput.ifBlank { "Client" },
                                    clientEmail = clientEmailInput.trim()
                                ) { success, result, msg ->
                                    isGenerating = false
                                    if (success && result != null) {
                                        lastReportResult = result
                                        scope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    } else {
                                        generationErrorMessage = msg
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Generation error: $msg")
                                        }
                                    }
                                }
                            },
                            enabled = !isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_report_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scraping Meta Tags & Generating AI Audit...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generate Report",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        if (generationErrorMessage != null) {
                            Text(
                                text = "⚠️ $generationErrorMessage",
                                color = CyberError,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Live Generated Report View
            lastReportResult?.let { reportResult ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Raw Inspection Summary Card
                        RawMetaCard(reportResult.rawData)

                        // 5-Point Synthesized Report Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = CyberSuccess,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "5-Point Technical SEO Audit",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CyberPrimary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = reportResult.engineUsed,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = CyberPrimary
                                        )
                                    }
                                }

                                Text(
                                    text = reportResult.generatedReport,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            copyToClipboard(reportResult.generatedReport, "SEO Audit Report")
                                        },
                                        modifier = Modifier.testTag("copy_report_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Report")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            targetEmailRecipient = clientEmailInput.trim()
                                            targetEmailSubject = "SEO Audit & 5-Point Growth Plan for ${reportResult.targetUrl}"
                                            targetEmailBody = reportResult.generatedReport
                                            emailDialogOpen = true
                                        },
                                        modifier = Modifier.testTag("email_report_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Email to Client")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Saved Deliverables History
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Client Deliverables (${savedDeliverables.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (savedDeliverables.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved client deliverables yet. Generate your first audit above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(savedDeliverables, key = { it.id }) { client ->
                    DeliverableHistoryItem(
                        client = client,
                        onCopy = { copyToClipboard(client.seoReport, "SEO Audit Report") },
                        onEmail = {
                            targetEmailRecipient = client.email
                            targetEmailSubject = "SEO Audit Report for ${client.name}"
                            targetEmailBody = client.seoReport
                            emailDialogOpen = true
                        }
                    )
                }
            }
        }
    }

    // Modal: Send Deliverable Email via Brevo
    if (emailDialogOpen) {
        AlertDialog(
            onDismissRequest = { emailDialogOpen = false },
            title = { Text("Email Deliverable via Brevo") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetEmailRecipient,
                        onValueChange = { targetEmailRecipient = it },
                        label = { Text("Recipient Client Email") },
                        placeholder = { Text("client@company.com") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deliverable_recipient_email_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetEmailSubject,
                        onValueChange = { targetEmailSubject = it },
                        label = { Text("Email Subject") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetEmailBody,
                        onValueChange = { targetEmailBody = it },
                        label = { Text("Report Deliverable") },
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
                        if (targetEmailRecipient.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Error: Client email is missing. Please provide client email to send proposal.")
                            }
                            return@Button
                        }
                        isDispatchingEmail = true
                        viewModel.sendEmailToClient(
                            toEmail = targetEmailRecipient,
                            subject = targetEmailSubject,
                            bodyText = targetEmailBody
                        ) { success, msg ->
                            isDispatchingEmail = false
                            emailDialogOpen = false
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    },
                    enabled = !isDispatchingEmail,
                    modifier = Modifier.testTag("confirm_deliverable_email_button")
                ) {
                    if (isDispatchingEmail) {
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
                TextButton(onClick = { emailDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RawMetaCard(rawData: RawSeoData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Scraped Technical Metrics",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Status: ${rawData.httpStatus} • ${rawData.latencyMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberSuccess
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Title: ${rawData.title ?: "MISSING"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rawData.title.isNullOrBlank()) CyberError else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Meta Description: ${rawData.metaDescription ?: "MISSING"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rawData.metaDescription.isNullOrBlank()) CyberWarning else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "H1: ${rawData.h1Heading ?: "MISSING"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rawData.h1Heading.isNullOrBlank()) CyberWarning else MaterialTheme.colorScheme.onSurface
                )
                if (rawData.issuesIdentified.isNotEmpty()) {
                    Text(
                        text = "Identified Issues: ${rawData.issuesIdentified.joinToString("; ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberError
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeliverableHistoryItem(
    client: CrmClient,
    onCopy: () -> Unit,
    onEmail: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (client.websiteUrl.isNotBlank()) {
                        Text(
                            text = client.websiteUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberPrimaryLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSuccess.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Report Ready",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyberSuccess
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = client.seoReport,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide Report" else "View Report")
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onCopy,
                        modifier = Modifier.testTag("copy_report_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = onEmail,
                        modifier = Modifier.testTag("email_report_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Email")
                    }
                }
            }
        }
    }
}
