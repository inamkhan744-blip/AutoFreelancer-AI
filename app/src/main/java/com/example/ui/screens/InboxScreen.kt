package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaPlayer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.DisposableEffect
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
import com.example.data.model.InboxEmail
import com.example.data.service.GeneratedVoiceNote
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberPrimaryLight
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTertiary
import com.example.ui.theme.CyberWarning
import com.example.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InboxScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val inboxEmails by viewModel.inboxEmails.collectAsState()
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, "Awaiting Review", "Approved & Sent", "Dismissed"
    var showScenarioDropdown by remember { mutableStateOf(false) }

    val awaitingCount = inboxEmails.count { it.status == "Awaiting Review" }
    val approvedCount = inboxEmails.count { it.status == "Approved & Sent" }

    val filteredEmails = remember(inboxEmails, statusFilter) {
        when (statusFilter) {
            "Awaiting Review" -> inboxEmails.filter { it.status == "Awaiting Review" }
            "Approved & Sent" -> inboxEmails.filter { it.status == "Approved & Sent" }
            "Dismissed" -> inboxEmails.filter { it.status == "Dismissed" }
            else -> inboxEmails
        }
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
                        modifier = Modifier.testTag("inbox_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Tasks"
                        )
                    }
                },
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "The AI Closer • Inbox",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (awaitingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberWarning.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$awaitingCount Action Required",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CyberWarning,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Auto-Drafted Objection Responses via Gemini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showScenarioDropdown = true },
                            modifier = Modifier.testTag("simulate_objection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Simulate Client Objection",
                                tint = CyberSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = showScenarioDropdown,
                            onDismissRequest = { showScenarioDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Simulate: Budget Objection (\"Too expensive\")") },
                                onClick = {
                                    viewModel.simulateIncomingClientObjection("Budget")
                                    showScenarioDropdown = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Simulated budget objection email received. Gemini drafting reply...")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simulate: Timing Objection (\"Next Quarter\")") },
                                onClick = {
                                    viewModel.simulateIncomingClientObjection("Timing")
                                    showScenarioDropdown = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Simulated timing objection email received.")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simulate: Proof & Trust (\"Burnt by past freelancer\")") },
                                onClick = {
                                    viewModel.simulateIncomingClientObjection("Trust")
                                    showScenarioDropdown = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Simulated trust objection email received.")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simulate: In-House Team (\"Have internal writer\")") },
                                onClick = {
                                    viewModel.simulateIncomingClientObjection("In-House")
                                    showScenarioDropdown = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Simulated in-house objection email received.")
                                    }
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.fetchWebhookInboundEmails { success, count, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        modifier = Modifier.testTag("sync_webhook_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Sync Webhook Emails",
                            tint = CyberPrimaryLight
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header summary banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Autonomous Objection Handler",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Review, edit, or attach ElevenLabs voice notes before Brevo dispatch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Status Filter Chips
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = statusFilter == "ALL",
                        onClick = { statusFilter = "ALL" },
                        label = { Text("All (${inboxEmails.size})") },
                        modifier = Modifier.testTag("inbox_filter_all"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyberPrimary
                        )
                    )
                    FilterChip(
                        selected = statusFilter == "Awaiting Review",
                        onClick = { statusFilter = "Awaiting Review" },
                        label = { Text("Awaiting Review ($awaitingCount)") },
                        modifier = Modifier.testTag("inbox_filter_awaiting"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberWarning.copy(alpha = 0.2f),
                            selectedLabelColor = CyberWarning
                        )
                    )
                    FilterChip(
                        selected = statusFilter == "Approved & Sent",
                        onClick = { statusFilter = "Approved & Sent" },
                        label = { Text("Approved & Sent ($approvedCount)") },
                        modifier = Modifier.testTag("inbox_filter_sent"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberSuccess.copy(alpha = 0.2f),
                            selectedLabelColor = CyberSuccess
                        )
                    )
                }
            }

            // Empty state
            if (filteredEmails.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "No emails in this inbox filter.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Simulate a realistic client objection to see Gemini draft a closing response.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Button(
                                onClick = {
                                    viewModel.simulateIncomingClientObjection("Budget")
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Simulated client objection added.")
                                    }
                                },
                                modifier = Modifier.testTag("empty_simulate_objection_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simulate Client Objection")
                            }
                        }
                    }
                }
            } else {
                items(filteredEmails, key = { it.id }) { email ->
                    InboxEmailCard(
                        email = email,
                        viewModel = viewModel,
                        onApproveAndSend = { voiceNote ->
                            viewModel.approveAndSendInboxReply(email, voiceNote) { success, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        onDismiss = {
                            viewModel.dismissInboxEmail(email.id)
                            scope.launch {
                                snackbarHostState.showSnackbar("Email dismissed.")
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InboxEmailCard(
    email: InboxEmail,
    viewModel: DashboardViewModel,
    onApproveAndSend: (GeneratedVoiceNote?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var draftText by remember(email.draftedReply) { mutableStateOf(email.draftedReply) }
    var isEditingDraft by remember { mutableStateOf(false) }
    var generatedVoiceNote by remember { mutableStateOf<GeneratedVoiceNote?>(null) }
    var isSynthesizingVoice by remember { mutableStateOf(false) }
    var isRegeneratingReply by remember { mutableStateOf(false) }

    // Media player for audio preview
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    val isPending = email.status == "Awaiting Review"
    val dateString = remember(email.receivedAt) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(email.receivedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inbox_email_card_${email.id}"),
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
            // Header: Sender, Date, Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = email.senderName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${email.senderEmail} • $dateString",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (email.status) {
                                "Approved & Sent" -> CyberSuccess.copy(alpha = 0.15f)
                                "Dismissed" -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                else -> CyberWarning.copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = email.status,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when (email.status) {
                            "Approved & Sent" -> CyberSuccess
                            "Dismissed" -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> CyberWarning
                        }
                    )
                }
            }

            // Objection Type Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyberSecondary.copy(alpha = 0.12f))
                    .border(1.dp, CyberSecondary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = CyberSecondary
                    )
                    Text(
                        text = email.objectionCategory,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyberSecondary
                    )
                }
            }

            // Inbound Client Email Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Subject: ${email.subject}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = email.incomingMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Gemini Drafted Closer Response
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyberPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "The AI Closer Drafted Response",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberPrimary
                        )
                    }

                    if (isPending) {
                        TextButton(onClick = { isEditingDraft = !isEditingDraft }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEditingDraft) "Done Editing" else "Edit Draft")
                        }
                    }
                }

                if (isEditingDraft) {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = {
                            draftText = it
                            viewModel.updateInboxDraft(email.id, it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 8,
                        shape = RoundedCornerShape(10.dp)
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = draftText.ifBlank { "No drafted reply available." },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Audio Player Bar if Voice Note Generated
            generatedVoiceNote?.let { voice ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CyberTertiary.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Headphones,
                                contentDescription = null,
                                tint = CyberTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "AI Voice Note Ready",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CyberTertiary
                                )
                                Text(
                                    text = "${voice.fileName} (${voice.fileSizeKb} KB • Will be attached to email)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (isPlayingAudio) {
                                    mediaPlayer?.stop()
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    isPlayingAudio = false
                                } else {
                                    try {
                                        mediaPlayer?.release()
                                        mediaPlayer = MediaPlayer().apply {
                                            setDataSource(voice.filePath)
                                            prepare()
                                            start()
                                            setOnCompletionListener {
                                                isPlayingAudio = false
                                            }
                                        }
                                        isPlayingAudio = true
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not play audio: ${e.message}", Toast.LENGTH_SHORT).show()
                                        isPlayingAudio = false
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlayingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlayingAudio) "Stop Voice Note" else "Play Voice Note",
                                tint = CyberTertiary
                            )
                        }
                    }
                }
            }

            // Action Buttons
            if (isPending) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onApproveAndSend(generatedVoiceNote) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("approve_send_reply_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve & Send via Brevo")
                    }

                    OutlinedButton(
                        onClick = {
                            isSynthesizingVoice = true
                            viewModel.generateVoiceNoteForText(
                                context = context,
                                text = draftText,
                                outputFileName = "closer_reply_${email.id.take(6)}.mp3"
                            ) { success, note, msg ->
                                isSynthesizingVoice = false
                                if (success && note != null) {
                                    generatedVoiceNote = note
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSynthesizingVoice,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("generate_voice_note_button")
                    ) {
                        if (isSynthesizingVoice) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Synthesizing MP3...")
                        } else {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Voice Note")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            isRegeneratingReply = true
                            viewModel.draftObjectionReply(
                                senderName = email.senderName,
                                senderEmail = email.senderEmail,
                                subject = email.subject,
                                incomingMessage = email.incomingMessage
                            ) { success, updatedEmail, msg ->
                                isRegeneratingReply = false
                                if (success && updatedEmail != null) {
                                    draftText = updatedEmail.draftedReply
                                    Toast.makeText(context, "Reply regenerated via Gemini", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isRegeneratingReply,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isRegeneratingReply) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Regenerate")
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Dismiss", color = CyberError)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (email.status == "Approved & Sent") "Dispatched to ${email.senderEmail} via Brevo" else "Dismissed by Director",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Reply", draftText))
                            Toast.makeText(context, "Copied reply to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Reply")
                    }
                }
            }
        }
    }
}
