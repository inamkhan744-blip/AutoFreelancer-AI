package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
    val availableGeminiModels by viewModel.availableGeminiModels.collectAsState()
    val isFetchingGeminiModels by viewModel.isFetchingGeminiModels.collectAsState()
    val geminiModelsMessage by viewModel.geminiModelsMessage.collectAsState()

    val availableOpenRouterModels by viewModel.availableOpenRouterModels.collectAsState()
    val isFetchingOpenRouterModels by viewModel.isFetchingOpenRouterModels.collectAsState()
    val openRouterModelsMessage by viewModel.openRouterModelsMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Form inputs initialized from EncryptedSharedPreferences
    var geminiApiKey by remember(currentSettings) { mutableStateOf(currentSettings.geminiApiKey) }
    var openRouterApiKey by remember(currentSettings) { mutableStateOf(currentSettings.openRouterApiKey) }
    var geminiModel by remember(currentSettings) { mutableStateOf(currentSettings.geminiModel) }
    var openRouterModel by remember(currentSettings) { mutableStateOf(currentSettings.openRouterModel) }
    var serperApiKey by remember(currentSettings) { mutableStateOf(currentSettings.serperApiKey) }
    var tavilyApiKey by remember(currentSettings) { mutableStateOf(currentSettings.tavilyApiKey) }
    var searchQuery by remember(currentSettings) { mutableStateOf(currentSettings.searchQuery) }
    var autoRunOnApproval by remember(currentSettings) { mutableStateOf(currentSettings.autoRunOnApproval) }

    // Dropdown toggles
    var geminiDropdownExpanded by remember { mutableStateOf(false) }
    var openRouterDropdownExpanded by remember { mutableStateOf(false) }

    // Password visibility toggles
    var showGeminiKey by remember { mutableStateOf(false) }
    var showOpenRouterKey by remember { mutableStateOf(false) }
    var showSerperKey by remember { mutableStateOf(false) }
    var showTavilyKey by remember { mutableStateOf(false) }

    // Test connection states
    var geminiTesting by remember { mutableStateOf(false) }
    var geminiTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var openRouterTesting by remember { mutableStateOf(false) }
    var openRouterTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

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
                            text = "Settings & AI Configuration",
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
                            text = "All API keys and active model preferences are encrypted using EncryptedSharedPreferences (AES-256 GCM) with Android KeyStore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Status Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPill(
                    label = "Gemini",
                    isConfigured = geminiApiKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "OpenRouter",
                    isConfigured = openRouterApiKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "Serper",
                    isConfigured = serperApiKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "Tavily",
                    isConfigured = tavilyApiKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
            }

            // ==========================================
            // 1. Google Gemini API Key (Primary AI)
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
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "1. Google Gemini API Key",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PRIMARY AI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyberSuccess
                                )
                            }
                            Text(
                                text = "Active Model: $geminiModel (gemini-3.8-flash)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Primary engine for generating SEO audit pitches and deliverables. Select or refresh available models directly from Google's endpoint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = geminiApiKey,
                        onValueChange = {
                            geminiApiKey = it
                            geminiTestResult = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input"),
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(
                                    imageVector = if (showGeminiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Gemini key visibility"
                                )
                            }
                        },
                        singleLine = true
                    )

                    // Gemini Model Fetch & Live Dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gemini Model Selection:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            OutlinedButton(
                                onClick = {
                                    if (geminiApiKey.isBlank()) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Enter a Gemini API Key first to fetch available models.")
                                        }
                                        return@OutlinedButton
                                    }
                                    viewModel.fetchAvailableGeminiModels(geminiApiKey)
                                },
                                enabled = !isFetchingGeminiModels,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("fetch_gemini_models_button")
                            ) {
                                if (isFetchingGeminiModels) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fetching...", fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fetch Gemini Models", fontSize = 11.sp)
                                }
                            }
                        }

                        geminiModelsMessage?.let { (success, msg) ->
                            InlineStatusBadge(success = success, message = msg)
                        }

                        // Gemini Model Dropdown
                        val allDisplayGeminiModels = (listOf(geminiModel) + availableGeminiModels)
                            .filter { it.isNotBlank() }
                            .distinct()

                        ExposedDropdownMenuBox(
                            expanded = geminiDropdownExpanded,
                            onExpandedChange = { geminiDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = geminiModel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Selected Gemini Model") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = geminiDropdownExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("gemini_model_dropdown_field")
                            )

                            ExposedDropdownMenu(
                                expanded = geminiDropdownExpanded,
                                onDismissRequest = { geminiDropdownExpanded = false }
                            ) {
                                allDisplayGeminiModels.forEach { modelOption ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = modelOption,
                                                    fontWeight = if (modelOption == geminiModel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (modelOption == "gemini-3.8-flash") CyberPrimaryLight else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (modelOption == "gemini-3.8-flash" || modelOption == "gemini-3.6-flash") {
                                                    Text(
                                                        text = "Active",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CyberSuccess
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            geminiModel = modelOption
                                            geminiDropdownExpanded = false
                                        },
                                        leadingIcon = if (modelOption == geminiModel) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = CyberSuccess,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }

                        // Quick Select Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "gemini-3.8-flash" to "gemini-3.8-flash (Recommended)",
                                "gemini-3.6-flash" to "gemini-3.6-flash (Fast)",
                                "gemini-2.5-flash" to "gemini-2.5-flash"
                            ).forEach { (id, label) ->
                                FilterChip(
                                    selected = geminiModel == id,
                                    onClick = { geminiModel = id },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberPrimary.copy(alpha = 0.25f)
                                    )
                                )
                            }
                        }
                    }

                    // Test Gemini Connection Button & Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (geminiApiKey.isBlank()) {
                                    geminiTestResult = Pair(false, "Please enter a Gemini API key first")
                                    return@OutlinedButton
                                }
                                geminiTesting = true
                                geminiTestResult = null
                                viewModel.testGeminiKey(geminiApiKey, geminiModel) { success, msg ->
                                    geminiTesting = false
                                    geminiTestResult = Pair(success, msg)
                                }
                            },
                            enabled = !geminiTesting && geminiApiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("test_gemini_button")
                        ) {
                            if (geminiTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Gemini Connection", fontSize = 12.sp)
                            }
                        }

                        geminiTestResult?.let { (success, msg) ->
                            InlineStatusBadge(success = success, message = msg)
                        }
                    }
                }
            }

            // ==========================================
            // 2. OpenRouter API Key (Fallback AI)
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
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = CyberSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "2. OpenRouter API Key",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FALLBACK AI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyberTertiary
                                )
                            }
                            Text(
                                text = "Active Model: $openRouterModel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Automatic fallback engine. If Gemini rate-limits or fails, the worker immediately tries OpenRouter and logs clear diagnostic output.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = openRouterApiKey,
                        onValueChange = {
                            openRouterApiKey = it
                            openRouterTestResult = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("openrouter_api_key_input"),
                        label = { Text("OpenRouter API Key") },
                        placeholder = { Text("sk-or-v1-...") },
                        visualTransformation = if (showOpenRouterKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showOpenRouterKey = !showOpenRouterKey }) {
                                Icon(
                                    imageVector = if (showOpenRouterKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle OpenRouter key visibility"
                                )
                            }
                        },
                        singleLine = true
                    )

                    // OpenRouter Model Fetch & Live Dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OpenRouter Model Selection:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            OutlinedButton(
                                onClick = {
                                    if (openRouterApiKey.isBlank()) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Enter an OpenRouter API Key first to fetch available models.")
                                        }
                                        return@OutlinedButton
                                    }
                                    viewModel.fetchAvailableOpenRouterModels(openRouterApiKey)
                                },
                                enabled = !isFetchingOpenRouterModels,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("fetch_openrouter_models_button")
                            ) {
                                if (isFetchingOpenRouterModels) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fetching...", fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fetch OpenRouter Models", fontSize = 11.sp)
                                }
                            }
                        }

                        openRouterModelsMessage?.let { (success, msg) ->
                            InlineStatusBadge(success = success, message = msg)
                        }

                        // OpenRouter Model Dropdown
                        val allDisplayOpenRouterModels = (listOf(openRouterModel) + availableOpenRouterModels)
                            .filter { it.isNotBlank() }
                            .distinct()

                        ExposedDropdownMenuBox(
                            expanded = openRouterDropdownExpanded,
                            onExpandedChange = { openRouterDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = openRouterModel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Selected OpenRouter Model") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = openRouterDropdownExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("openrouter_model_dropdown_field")
                            )

                            ExposedDropdownMenu(
                                expanded = openRouterDropdownExpanded,
                                onDismissRequest = { openRouterDropdownExpanded = false }
                            ) {
                                allDisplayOpenRouterModels.forEach { modelOption ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = modelOption,
                                                    fontWeight = if (modelOption == openRouterModel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (modelOption.endsWith(":free")) CyberSecondary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (modelOption.endsWith(":free")) {
                                                    Text(
                                                        text = "FREE",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CyberSuccess
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            openRouterModel = modelOption
                                            openRouterDropdownExpanded = false
                                        },
                                        leadingIcon = if (modelOption == openRouterModel) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = CyberSuccess,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }

                        // Quick Select Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "meta-llama/llama-3.3-70b-instruct:free" to "Llama 3.3 70B (Free)",
                                "google/gemma-4-26b-a4b-it:free" to "Gemma 4 26B (Free)"
                            ).forEach { (id, label) ->
                                FilterChip(
                                    selected = openRouterModel == id,
                                    onClick = { openRouterModel = id },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberSecondary.copy(alpha = 0.25f)
                                    )
                                )
                            }
                        }
                    }

                    // Test OpenRouter Connection Button & Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (openRouterApiKey.isBlank()) {
                                    openRouterTestResult = Pair(false, "Please enter an OpenRouter API key first")
                                    return@OutlinedButton
                                }
                                openRouterTesting = true
                                openRouterTestResult = null
                                viewModel.testOpenRouterKey(openRouterApiKey, openRouterModel) { success, msg ->
                                    openRouterTesting = false
                                    openRouterTestResult = Pair(success, msg)
                                }
                            },
                            enabled = !openRouterTesting && openRouterApiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("test_openrouter_button")
                        ) {
                            if (openRouterTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test OpenRouter Connection", fontSize = 12.sp)
                            }
                        }

                        openRouterTestResult?.let { (success, msg) ->
                            InlineStatusBadge(success = success, message = msg)
                        }
                    }
                }
            }

            // ==========================================
            // 3. Serper Search API Key
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
                                text = "3. Serper Search API Key",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Google Search via Serper.dev for live prospects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Queries Google organically to locate businesses and founders actively seeking SEO audits on LinkedIn, Reddit, and Twitter.",
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
            // 4. Tavily Search API Key (Backup search)
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
                                text = "4. Tavily Search API Key",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Backup search option (Automatic Fallback)",
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
                        placeholder = { Text("tvly-...") },
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
                        geminiApiKey = geminiApiKey.trim(),
                        openRouterApiKey = openRouterApiKey.trim(),
                        geminiModel = geminiModel.trim(),
                        openRouterModel = openRouterModel.trim(),
                        serperApiKey = serperApiKey.trim(),
                        tavilyApiKey = tavilyApiKey.trim(),
                        searchQuery = searchQuery.trim(),
                        autoRunOnApproval = autoRunOnApproval
                    )
                    viewModel.saveSettings(updated)
                    scope.launch {
                        snackbarHostState.showSnackbar("Settings and models saved securely in EncryptedSharedPreferences!")
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
                Text("Save Keys & Models to Encrypted Storage", fontWeight = FontWeight.Bold)
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
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Key,
                contentDescription = null,
                tint = if (isConfigured) CyberSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isConfigured) "Configured" else "Not set",
                    fontSize = 8.sp,
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
            text = message.take(40),
            fontSize = 11.sp,
            color = if (success) CyberSuccess else CyberError,
            fontWeight = FontWeight.Medium
        )
    }
}
