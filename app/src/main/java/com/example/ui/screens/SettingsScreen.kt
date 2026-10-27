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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.repository.AppSettings
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
fun SettingsScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val currentSettings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Form inputs initialized from EncryptedSharedPreferences
    var groqApiKey by remember(currentSettings) { mutableStateOf(currentSettings.groqApiKey) }
    var serperApiKey by remember(currentSettings) { mutableStateOf(currentSettings.serperApiKey) }
    var tavilyApiKey by remember(currentSettings) { mutableStateOf(currentSettings.tavilyApiKey) }
    var groqModel by remember(currentSettings) { mutableStateOf(currentSettings.groqModel) }
    var searchQuery by remember(currentSettings) { mutableStateOf(currentSettings.searchQuery) }
    var autoRunOnApproval by remember(currentSettings) { mutableStateOf(currentSettings.autoRunOnApproval) }

    // Password visibility toggles
    var showGroqKey by remember { mutableStateOf(false) }
    var showSerperKey by remember { mutableStateOf(false) }
    var showTavilyKey by remember { mutableStateOf(false) }

    // Test connection states
    var groqTesting by remember { mutableStateOf(false) }
    var groqTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var serperTesting by remember { mutableStateOf(false) }
    var serperTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var tavilyTesting by remember { mutableStateOf(false) }
    var tavilyTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Settings & API Keys",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security & Persistence Banner
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
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyberPrimaryLight,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Encrypted On-Device Storage",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All API keys are encrypted using EncryptedSharedPreferences (AES-256 GCM) with Android KeyStore and persist securely across restarts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Status Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(
                    label = "Groq AI",
                    isConfigured = groqApiKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "Serper Search",
                    isConfigured = serperApiKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "Tavily Backup",
                    isConfigured = tavilyApiKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
            }

            // ==========================================
            // 1. Groq API Key (for AI text generation)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyberSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CyberSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "1. Groq API Key",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "For AI text generation (openai/gpt-oss-120b)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Used when tasks are Approved to generate tailored SEO audit pitches, client proposals, and deliverable action plans in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = groqApiKey,
                        onValueChange = {
                            groqApiKey = it
                            groqTestResult = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("groq_api_key_input"),
                        label = { Text("Groq API Key") },
                        placeholder = { Text("e.g. gsk_...") },
                        visualTransformation = if (showGroqKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showGroqKey = !showGroqKey }) {
                                Icon(
                                    imageVector = if (showGroqKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Groq key visibility"
                                )
                            }
                        },
                        singleLine = true
                    )

                    // Model Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Groq Model:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val models = listOf(
                                "openai/gpt-oss-120b" to "GPT-OSS 120B (Recommended)",
                                "llama-3.1-8b-instant" to "LLaMA 3.1 8B (Ultra Fast)",
                                "mixtral-8x7b-32768" to "Mixtral 8x7B"
                            )
                            models.forEach { (id, label) ->
                                FilterChip(
                                    selected = groqModel == id,
                                    onClick = { groqModel = id },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberSecondary.copy(alpha = 0.25f)
                                    )
                                )
                            }
                        }
                    }

                    // Test Groq Connection Button & Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (groqApiKey.isBlank()) {
                                    groqTestResult = Pair(false, "Please enter a Groq API key first")
                                    return@OutlinedButton
                                }
                                groqTesting = true
                                groqTestResult = null
                                viewModel.testGroqKey(groqApiKey) { success, msg ->
                                    groqTesting = false
                                    groqTestResult = Pair(success, msg)
                                }
                            },
                            enabled = !groqTesting && groqApiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (groqTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Groq Connection", fontSize = 12.sp)
                            }
                        }

                        groqTestResult?.let { (success, msg) ->
                            InlineStatusBadge(success = success, message = msg)
                        }
                    }
                }
            }

            // ==========================================
            // 2. Serper Search API Key (for finding clients)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyberPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "2. Serper Search API Key",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "For finding clients on the internet (Google Search)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Queries Google organically via Serper.dev to locate real businesses and founders actively seeking SEO audits on LinkedIn, Reddit, and Twitter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = serperApiKey,
                        onValueChange = {
                            serperApiKey = it
                            serperTestResult = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("serper_api_key_input"),
                        label = { Text("Serper Search API Key") },
                        placeholder = { Text("e.g. key from serper.dev") },
                        visualTransformation = if (showSerperKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showSerperKey = !showSerperKey }) {
                                Icon(
                                    imageVector = if (showSerperKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Serper key visibility"
                                )
                            }
                        },
                        singleLine = true
                    )

                    // Test Serper Button & Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (serperApiKey.isBlank()) {
                                    serperTestResult = Pair(false, "Please enter a Serper API key first")
                                    return@OutlinedButton
                                }
                                serperTesting = true
                                serperTestResult = null
                                viewModel.testSerperKey(serperApiKey) { success, msg ->
                                    serperTesting = false
                                    serperTestResult = Pair(success, msg)
                                }
                            },
                            enabled = !serperTesting && serperApiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (serperTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Serper Search", fontSize = 12.sp)
                            }
                        }

                        serperTestResult?.let { (success, msg) ->
                            InlineStatusBadge(success = success, message = msg)
                        }
                    }
                }
            }

            // ==========================================
            // 3. Tavily Search API Key (as backup search)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyberTertiary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = null,
                                tint = CyberTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "3. Tavily Search API Key",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "As a backup search option (Automatic Fallback)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "High-accuracy search engine optimized for AI agents. Automatically kicks in as a seamless backup if Serper is rate-limited or fails.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tavilyApiKey,
                        onValueChange = {
                            tavilyApiKey = it
                            tavilyTestResult = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tavily_api_key_input"),
                        label = { Text("Tavily Search API Key") },
                        placeholder = { Text("e.g. tvly-...") },
                        visualTransformation = if (showTavilyKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showTavilyKey = !showTavilyKey }) {
                                Icon(
                                    imageVector = if (showTavilyKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Tavily key visibility"
                                )
                            }
                        },
                        singleLine = true
                    )

                    // Test Tavily Button & Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (tavilyApiKey.isBlank()) {
                                    tavilyTestResult = Pair(false, "Please enter a Tavily API key first")
                                    return@OutlinedButton
                                }
                                tavilyTesting = true
                                tavilyTestResult = null
                                viewModel.testTavilyKey(tavilyApiKey) { success, msg ->
                                    tavilyTesting = false
                                    tavilyTestResult = Pair(success, msg)
                                }
                            },
                            enabled = !tavilyTesting && tavilyApiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (tavilyTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Tavily (Backup)", fontSize = 12.sp)
                            }
                        }

                        tavilyTestResult?.let { (success, msg) ->
                            InlineStatusBadge(success = success, message = msg)
                        }
                    }
                }
            }

            // ==========================================
            // Search Query Configuration & Presets
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Lead Discovery Search Formula",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Default Query Formula") },
                        minLines = 2
                    )

                    Text(text = "Quick Presets:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            "looking for SEO audit site:linkedin.com",
                            "\"need someone to do an SEO audit\" site:reddit.com",
                            "Shopify SEO audit site:x.com",
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
                }
            }

            // ==========================================
            // Save Settings Button
            // ==========================================
            Button(
                onClick = {
                    val updated = AppSettings(
                        groqApiKey = groqApiKey.trim(),
                        serperApiKey = serperApiKey.trim(),
                        tavilyApiKey = tavilyApiKey.trim(),
                        groqModel = groqModel.trim(),
                        searchQuery = searchQuery.trim(),
                        autoRunOnApproval = autoRunOnApproval
                    )
                    viewModel.saveSettings(updated)
                    scope.launch {
                        snackbarHostState.showSnackbar("Keys saved securely in EncryptedSharedPreferences!")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_settings_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Keys to Encrypted Storage", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    isConfigured: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConfigured) CyberSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (isConfigured) CyberSuccess else Color.Gray.copy(alpha = 0.4f),
                    if (isConfigured) CyberSuccess.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Key,
                contentDescription = null,
                tint = if (isConfigured) CyberSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isConfigured) "Configured" else "Not set",
                    fontSize = 9.sp,
                    color = if (isConfigured) CyberSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InlineStatusBadge(
    success: Boolean,
    message: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (success) CyberSuccess.copy(alpha = 0.15f) else CyberError.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (success) CyberSuccess else CyberError,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = message.take(30),
            fontSize = 11.sp,
            color = if (success) CyberSuccess else CyberError,
            fontWeight = FontWeight.Medium
        )
    }
}
