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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    // Form inputs initialized from the current cloud settings state
    var geminiApiKey by remember(currentSettings) { mutableStateOf(currentSettings.geminiApiKey) }
    var openRouterApiKey by remember(currentSettings) { mutableStateOf(currentSettings.openRouterApiKey) }
    var geminiModel by remember(currentSettings) { mutableStateOf(currentSettings.geminiModel) }
    var openRouterModel by remember(currentSettings) { mutableStateOf(currentSettings.openRouterModel) }
    var brevoApiKey by remember(currentSettings) { mutableStateOf(currentSettings.brevoApiKey) }
    var serperApiKey by remember(currentSettings) { mutableStateOf(currentSettings.serperApiKey) }
    var tavilyApiKey by remember(currentSettings) { mutableStateOf(currentSettings.tavilyApiKey) }
    var paymentLinkUrl by remember(currentSettings) { mutableStateOf(currentSettings.paymentLinkUrl) }
    var autoPilotEnabled by remember(currentSettings) { mutableStateOf(currentSettings.autoPilotEnabled) }
    var searchQuery by remember(currentSettings) { mutableStateOf(currentSettings.searchQuery) }
    var autoRunOnApproval by remember(currentSettings) { mutableStateOf(currentSettings.autoRunOnApproval) }
    var elevenLabsApiKey by remember(currentSettings) { mutableStateOf(currentSettings.elevenLabsApiKey) }
    var elevenLabsVoiceId by remember(currentSettings) { mutableStateOf(currentSettings.elevenLabsVoiceId) }
    var inboundEmailWebhookUrl by remember(currentSettings) { mutableStateOf(currentSettings.inboundEmailWebhookUrl) }

    // Dropdown toggles
    var geminiDropdownExpanded by remember { mutableStateOf(false) }
    var openRouterDropdownExpanded by remember { mutableStateOf(false) }

    // Password visibility toggles
    var showGeminiKey by remember { mutableStateOf(false) }
    var showOpenRouterKey by remember { mutableStateOf(false) }
    var showBrevoKey by remember { mutableStateOf(false) }
    var showSerperKey by remember { mutableStateOf(false) }
    var showTavilyKey by remember { mutableStateOf(false) }
    var showElevenLabsKey by remember { mutableStateOf(false) }

    // Test connection states
    var geminiTesting by remember { mutableStateOf(false) }
    var geminiTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var openRouterTesting by remember { mutableStateOf(false) }
    var openRouterTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var brevoTesting by remember { mutableStateOf(false) }
    var brevoTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var serperTesting by remember { mutableStateOf(false) }
    var serperTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var tavilyTesting by remember { mutableStateOf(false) }
    var tavilyTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    var elevenLabsTesting by remember { mutableStateOf(false) }
    var elevenLabsTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

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
                            text = "Firebase Cloud Storage",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "API keys are stored in your signed-in Firebase account and are accessible only to that account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Status Pills
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPill(
                    label = "Auto-Pilot",
                    isConfigured = autoPilotEnabled,
                    configuredText = "ACTIVE",
                    unconfiguredText = "OFF",
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "Payment Link",
                    isConfigured = paymentLinkUrl.isNotBlank(),
                    configuredText = "ATTACHED",
                    unconfiguredText = "NONE",
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "Gemini AI",
                    isConfigured = geminiApiKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
                StatusPill(
                    label = "Brevo Email",
                    isConfigured = brevoApiKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                )
            }

            // ==========================================
            // Auto-Pilot Mode (Background Automation)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (autoPilotEnabled) CyberTertiary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = if (autoPilotEnabled) Brush.horizontalGradient(listOf(CyberTertiary, CyberSecondary))
                    else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)))
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (autoPilotEnabled) CyberTertiary.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = if (autoPilotEnabled) CyberTertiary else Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Auto-Pilot Mode",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (autoPilotEnabled) "Autonomous Background Pipeline Active" else "Manual Approval Required",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (autoPilotEnabled) CyberTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = autoPilotEnabled,
                            onCheckedChange = { autoPilotEnabled = it },
                            modifier = Modifier.testTag("auto_pilot_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyberTertiary,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    Text(
                        text = "When Auto-Pilot is ON, the app automatically approves and executes any pending tasks without waiting for manual confirmation. Android WorkManager periodically discovers leads, synthesizes proposals, and dispatches deliverables in the background even if the app is closed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (autoPilotEnabled) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberTertiary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberTertiary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Auto-Pilot Active • Background sync & autonomous execution enabled",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CyberTertiary
                            )
                        }
                    }
                }
            }

            // ==========================================
            // Dynamic Payment Link (Gumroad / Stripe)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberSecondary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = CyberSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Dynamic Payment Link URL",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automated Client Checkout & CTA",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberSecondary
                            )
                        }
                    }

                    Text(
                        text = "The AI selects a $50 starter quote or a $350 corporate quote. Use {amount} in a checkout URL template, or ensure your payment provider honors the amount query parameter added to this link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = paymentLinkUrl,
                        onValueChange = { paymentLinkUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_link_input"),
                        label = { Text("Payment Link URL (Gumroad / Stripe)") },
                        placeholder = { Text("https://buy.stripe.com/... or https://gumroad.com/l/...") },
                        singleLine = true
                    )

                    if (paymentLinkUrl.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSuccess.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Checkout amount is selected per lead: $50 starter or $350 corporate.",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberSuccess
                            )
                        }
                    }
                }
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
            // 3. Brevo API Key (Email Dispatch)
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
                                .background(CyberPrimaryLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = CyberPrimaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "3. Brevo API Key",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EMAIL DISPATCH",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyberPrimaryLight
                                )
                            }
                            Text(
                                text = "Email delivery API (brevo.com)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "When tasks complete, the generated Gemini proposal is automatically emailed to the client address, or sent manually via 'Send Email' in task details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = brevoApiKey,
                        onValueChange = {
                            brevoApiKey = it
                            brevoTestResult = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brevo_api_key_input"),
                        label = { Text("Brevo API Key") },
                        placeholder = { Text("xkeysib-...") },
                        visualTransformation = if (showBrevoKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showBrevoKey = !showBrevoKey }) {
                                Icon(
                                    imageVector = if (showBrevoKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Brevo key visibility"
                                )
                            }
                        },
                        singleLine = true
                    )

                    Text(
                        text = "Verified sender: inamkhan744@gmail.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Test Brevo Connection Button & Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (brevoApiKey.isBlank()) {
                                    brevoTestResult = Pair(false, "Please enter a Brevo API key first")
                                    return@OutlinedButton
                                }
                                brevoTesting = true
                                brevoTestResult = null
                                viewModel.testBrevoKey(brevoApiKey) { success, msg ->
                                    brevoTesting = false
                                    brevoTestResult = Pair(success, msg)
                                }
                            },
                            enabled = !brevoTesting && brevoApiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("test_brevo_button")
                        ) {
                            if (brevoTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Brevo Connection", fontSize = 12.sp)
                            }
                        }

                        brevoTestResult?.let { (success, msg) ->
                            InlineStatusBadge(success = success, message = msg)
                        }
                    }
                }
            }

            // ==========================================
            // ElevenLabs API Key (AI Voice Notes)
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
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ElevenLabs API Key (AI Voice Notes)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Converts generated proposals into realistic MP3 audio notes attached to outreach emails.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = elevenLabsApiKey,
                        onValueChange = { elevenLabsApiKey = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("elevenlabs_api_key_input"),
                        label = { Text("ElevenLabs API Key") },
                        placeholder = { Text("xi-api-key...") },
                        visualTransformation = if (showElevenLabsKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showElevenLabsKey = !showElevenLabsKey }) {
                                Icon(
                                    imageVector = if (showElevenLabsKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showElevenLabsKey) "Hide key" else "Show key"
                                )
                            }
                        },
                        singleLine = true
                    )

                    Text(
                        text = "Voice Persona:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = elevenLabsVoiceId == "21m00Tcm4TlvDq8ikWAM",
                            onClick = { elevenLabsVoiceId = "21m00Tcm4TlvDq8ikWAM" },
                            label = { Text("Rachel (Natural)") }
                        )
                        FilterChip(
                            selected = elevenLabsVoiceId == "pNInz6obpgDQGcFmaJgB",
                            onClick = { elevenLabsVoiceId = "pNInz6obpgDQGcFmaJgB" },
                            label = { Text("Adam (Authoritative)") }
                        )
                        FilterChip(
                            selected = elevenLabsVoiceId == "29vD33N1CtxCmqQRPOHJ",
                            onClick = { elevenLabsVoiceId = "29vD33N1CtxCmqQRPOHJ" },
                            label = { Text("Drew (Conversational)") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (elevenLabsApiKey.isBlank()) {
                                    elevenLabsTestResult = Pair(false, "Please enter an ElevenLabs API key first")
                                    return@OutlinedButton
                                }
                                elevenLabsTesting = true
                                elevenLabsTestResult = null
                                viewModel.testElevenLabsKey(elevenLabsApiKey) { success, msg ->
                                    elevenLabsTesting = false
                                    elevenLabsTestResult = Pair(success, msg)
                                }
                            },
                            enabled = !elevenLabsTesting && elevenLabsApiKey.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("test_elevenlabs_button")
                        ) {
                            if (elevenLabsTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test ElevenLabs Connection", fontSize = 12.sp)
                            }
                        }

                        elevenLabsTestResult?.let { (success, msg) ->
                            InlineStatusBadge(success = success, message = msg)
                        }
                    }
                }
            }

            // ==========================================
            // The AI Closer: Incoming Email Webhook URL
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
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = CyberTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "The AI Closer: Inbound Webhook URL",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Receive client email replies from Zapier, Make, or Cloudflare Worker for autonomous objection handling.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inboundEmailWebhookUrl,
                        onValueChange = { inboundEmailWebhookUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inbound_webhook_url_input"),
                        label = { Text("Webhook Endpoint URL") },
                        placeholder = { Text("https://hooks.zapier.com/hooks/catch/...") },
                        singleLine = true
                    )
                }
            }

            // ==========================================
            // 4. Serper Search API Key
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
                                text = "4. Serper Search API Key",
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
            // 5. Tavily Search API Key (Backup search)
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
                                text = "5. Tavily Search API Key",
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
                        brevoApiKey = brevoApiKey.trim(),
                        serperApiKey = serperApiKey.trim(),
                        tavilyApiKey = tavilyApiKey.trim(),
                        paymentLinkUrl = paymentLinkUrl.trim(),
                        autoPilotEnabled = autoPilotEnabled,
                        searchQuery = searchQuery.trim(),
                        autoRunOnApproval = autoRunOnApproval,
                        elevenLabsApiKey = elevenLabsApiKey.trim(),
                        elevenLabsVoiceId = elevenLabsVoiceId.trim(),
                        inboundEmailWebhookUrl = inboundEmailWebhookUrl.trim()
                    )
                    viewModel.saveSettings(updated, context) { success, message ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
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
    configuredText: String = "Configured",
    unconfiguredText: String = "Not set",
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
                    text = if (isConfigured) configuredText else unconfiguredText,
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
