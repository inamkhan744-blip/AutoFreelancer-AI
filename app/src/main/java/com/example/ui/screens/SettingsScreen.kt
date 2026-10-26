package com.example.ui.screens

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SearchProvider
import com.example.data.repository.AppSettings
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberPrimaryLight
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberTertiary
import com.example.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val currentSettings by viewModel.settings.collectAsState()
    val discoveryProgress by viewModel.discoveryProgress.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var searchApiKey by remember(currentSettings) { mutableStateOf(currentSettings.searchApiKey) }
    var emailApiKey by remember(currentSettings) { mutableStateOf(currentSettings.emailApiKey) }
    var searchProvider by remember(currentSettings) { mutableStateOf(currentSettings.searchProvider) }
    var googleCseId by remember(currentSettings) { mutableStateOf(currentSettings.googleCseId) }
    var searchQuery by remember(currentSettings) { mutableStateOf(currentSettings.searchQuery) }
    var senderEmail by remember(currentSettings) { mutableStateOf(currentSettings.senderEmail) }
    var autoCreateTasks by remember(currentSettings) { mutableStateOf(currentSettings.autoCreateTasks) }

    var showSearchKey by remember { mutableStateOf(false) }
    var showEmailKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Automated AI Worker Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Dashboard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CyberPrimary.copy(alpha = 0.12f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CyberPrimary, CyberSecondary))
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = CyberPrimaryLight,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Autonomous Scout & Outreach Config",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Configure your Search & Email API keys to discover leads on LinkedIn, Reddit & X, and let Gemini craft personalized audit pitches.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 1: API Keys
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = CyberSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "API Keys & Integrations",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Search Provider Picker
                    Text(text = "Search Engine Provider:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { searchProvider = SearchProvider.SERPER }
                                .padding(end = 8.dp)
                        ) {
                            RadioButton(
                                selected = searchProvider == SearchProvider.SERPER,
                                onClick = { searchProvider = SearchProvider.SERPER }
                            )
                            Text("Serper.dev", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { searchProvider = SearchProvider.GOOGLE_CSE }
                                .padding(end = 8.dp)
                        ) {
                            RadioButton(
                                selected = searchProvider == SearchProvider.GOOGLE_CSE,
                                onClick = { searchProvider = SearchProvider.GOOGLE_CSE }
                            )
                            Text("Google Custom Search", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Search API Key
                    OutlinedTextField(
                        value = searchApiKey,
                        onValueChange = { searchApiKey = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_api_key_input"),
                        label = { Text("Search API Key (${searchProvider.displayName})") },
                        placeholder = { Text(if (searchProvider == SearchProvider.SERPER) "e.g. 1a2b3c... (from serper.dev)" else "Google API Key") },
                        visualTransformation = if (showSearchKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showSearchKey = !showSearchKey }) {
                                Icon(
                                    imageVector = if (showSearchKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle key visibility"
                                )
                            }
                        },
                        singleLine = true
                    )

                    if (searchProvider == SearchProvider.GOOGLE_CSE) {
                        OutlinedTextField(
                            value = googleCseId,
                            onValueChange = { googleCseId = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Google Search Engine ID (cx)") },
                            placeholder = { Text("e.g. 0123456789:abcdefgh") },
                            singleLine = true
                        )
                    }

                    // Email API Key
                    OutlinedTextField(
                        value = emailApiKey,
                        onValueChange = { emailApiKey = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_api_key_input"),
                        label = { Text("Email Delivery API Key (Resend / SendGrid)") },
                        placeholder = { Text("e.g. re_1234567890...") },
                        visualTransformation = if (showEmailKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showEmailKey = !showEmailKey }) {
                                Icon(
                                    imageVector = if (showEmailKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle email key visibility"
                                )
                            }
                        },
                        singleLine = true
                    )

                    // Sender Email
                    OutlinedTextField(
                        value = senderEmail,
                        onValueChange = { senderEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Sender Email Address") },
                        placeholder = { Text("e.g. director@youragency.com") },
                        singleLine = true
                    )
                }
            }

            // Section 2: Automated Search Query & Presets
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CyberTertiary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Target Search Query for SEO Audits",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search Query Formula") },
                        minLines = 2
                    )

                    Text(text = "Quick Query Presets:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            "looking for SEO audit site:linkedin.com",
                            "\"need someone to do an SEO audit\" site:reddit.com",
                            "recommend SEO consultant site:x.com",
                            "hiring freelance SEO audit"
                        )
                        presets.forEach { preset ->
                            FilterChip(
                                selected = searchQuery == preset,
                                onClick = { searchQuery = preset },
                                label = { Text(preset, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Queue to Task Feed", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Automatically create Pending tasks for approval when prospects are found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoCreateTasks,
                            onCheckedChange = { autoCreateTasks = it }
                        )
                    }
                }
            }

            // Save Settings Button
            Button(
                onClick = {
                    val updated = AppSettings(
                        searchApiKey = searchApiKey,
                        emailApiKey = emailApiKey,
                        searchProvider = searchProvider,
                        googleCseId = googleCseId,
                        searchQuery = searchQuery,
                        senderEmail = senderEmail,
                        autoCreateTasks = autoCreateTasks
                    )
                    viewModel.saveSettings(updated)
                    scope.launch {
                        snackbarHostState.showSnackbar("Settings & API keys saved successfully!")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_settings_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Configuration", fontWeight = FontWeight.Bold)
            }

            // Section 3: Live Lead Discovery & Gemini Pitch Test
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CyberSecondary, CyberTertiary))
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Run AI Lead Discovery Pipeline",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = "Triggers Search API to discover active SEO audit requests on the web, passes context to Gemini 3.8-Flash to draft personalized pitches, and stages tasks for your Approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    Button(
                        onClick = {
                            viewModel.runLeadDiscovery(searchQuery)
                        },
                        enabled = !discoveryProgress.isRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("run_discovery_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (discoveryProgress.isRunning) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Running Pipeline...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Lead Discovery & Pitch Generation", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Progress & Results Status
                    AnimatedVisibility(visible = discoveryProgress.stepMessage.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            if (discoveryProgress.isRunning) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = CyberSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = discoveryProgress.stepMessage,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (discoveryProgress.error != null) CyberError else CyberTertiary
                                )
                            )

                            if (discoveryProgress.generatedPitches.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Generated Pitches (${discoveryProgress.generatedPitches.size}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                                discoveryProgress.generatedPitches.forEach { pitch ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Prospect: ${pitch.prospectNameOrCompany}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = CyberSecondary
                                            )
                                            Text(
                                                text = "Subject: ${pitch.subjectLine}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Est. Value: $${pitch.suggestedRateOrValue.toInt()}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CyberTertiary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View & Approve in Dashboard")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
