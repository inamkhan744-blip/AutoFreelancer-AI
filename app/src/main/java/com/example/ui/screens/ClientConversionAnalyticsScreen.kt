package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClosureTrendPoint
import com.example.data.model.ConversionAnalyticsSummary
import com.example.data.model.CrmClient
import com.example.data.model.PipelineFunnelStage
import com.example.data.model.ResponseTimeDistribution
import com.example.data.service.GeneratedTemplate
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberPrimaryLight
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTertiary
import com.example.ui.theme.CyberWarning
import com.example.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClientConversionAnalyticsScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val summary by viewModel.conversionAnalyticsSummary.collectAsState()
    val funnelStages by viewModel.pipelineFunnelStages.collectAsState()
    val responseTimes by viewModel.responseTimeDistribution.collectAsState()
    val closuresOverTime by viewModel.closureTrendPoints.collectAsState()
    val unresponsiveLeads by viewModel.unresponsiveLeads.collectAsState()
    val isOperationLoading by viewModel.isOperationLoading.collectAsState()

    var selectedTimeframe by remember { mutableStateOf("All Time") } // "Last 7 Days", "Last 30 Days", "All Time"

    // Check-in review dialog state
    var selectedLeadForCheckIn by remember { mutableStateOf<CrmClient?>(null) }
    var draftedCheckInTemplate by remember { mutableStateOf<GeneratedTemplate?>(null) }
    var editedSubject by remember { mutableStateOf("") }
    var editedBody by remember { mutableStateOf("") }
    var isDraftingCheckIn by remember { mutableStateOf(false) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 } }

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
                        modifier = Modifier.testTag("analytics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
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
                                text = "Conversion Analytics",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberPrimary.copy(alpha = 0.15f))
                                    .border(1.dp, CyberPrimary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "RECHARTS PIPELINE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CyberPrimary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        Text(
                            text = "Lead-to-Client Funnel • Response Times • Closures",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.scheduleAll48HourCheckIns { count, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        },
                        modifier = Modifier.testTag("auto_schedule_all_checkins_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Auto-Schedule 48h Check-Ins",
                            tint = CyberSecondary
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
            // Timeframe selector chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Last 7 Days", "Last 30 Days", "All Time").forEach { tf ->
                        FilterChip(
                            selected = selectedTimeframe == tf,
                            onClick = { selectedTimeframe = tf },
                            label = { Text(tf, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyberPrimary
                            ),
                            modifier = Modifier.testTag("timeframe_chip_$tf")
                        )
                    }
                }
            }

            // Executive KPI Summary Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalyticsKpiCard(
                            title = "Win Rate",
                            value = "${"%.1f".format(summary.winRatePercent)}%",
                            subtitle = "${summary.wonLeads} won / ${summary.totalLeads} total",
                            accentColor = CyberSuccess,
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsKpiCard(
                            title = "Closed Revenue",
                            value = currencyFormat.format(summary.totalWonRevenue),
                            subtitle = "Avg: ${currencyFormat.format(summary.avgDealSize)}",
                            accentColor = CyberTertiary,
                            icon = Icons.Default.MonetizationOn,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalyticsKpiCard(
                            title = "Avg Latency",
                            value = "${"%.1f".format(summary.avgResponseTimeHours)} hrs",
                            subtitle = "Speed to engagement",
                            accentColor = CyberPrimaryLight,
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsKpiCard(
                            title = "48h Inactive",
                            value = "${summary.unresponsiveCount} leads",
                            subtitle = "Need check-in pitch",
                            accentColor = if (summary.unresponsiveCount > 0) CyberWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                            icon = Icons.Default.HourglassBottom,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 1. Lead-to-Client Pipeline Funnel Visualization
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pipeline_funnel_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LEAD-TO-CLIENT PIPELINE FUNNEL",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CyberPrimary
                                )
                                Text(
                                    text = "Stage conversion % and drop-off analysis",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Pipeline: ${currencyFormat.format(summary.totalPipelineValue)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyberTertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (funnelStages.isEmpty() || summary.totalLeads == 0) {
                            Text(
                                text = "No pipeline deals recorded yet. Launch Lead Scout to begin populating conversion metrics.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                funnelStages.forEachIndexed { index, stage ->
                                    FunnelStageBar(
                                        stage = stage,
                                        currencyFormat = currencyFormat,
                                        index = index
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Response Times Tracking Chart
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("response_times_chart_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "RESPONSE TIMES TRACKING",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CyberSecondary
                                )
                                Text(
                                    text = "Latency distribution and lead engagement velocity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = CyberSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            responseTimes.forEach { bucket ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = bucket.bucketLabel,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                        )
                                        Text(
                                            text = "${bucket.count} leads (${"%.1f".format(bucket.percentage)}%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (bucket.percentage / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = Color(bucket.colorHex),
                                        trackColor = Color(bucket.colorHex).copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Successful Lead Closures Over Time Chart
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("closures_over_time_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LEAD CLOSURES OVER TIME",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CyberTertiary
                                )
                                Text(
                                    text = "7-Day closed deals and won revenue trajectory",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = CyberTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual bar chart for closures over time
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val maxCount = closuresOverTime.maxOfOrNull { it.wonCount }?.coerceAtLeast(1) ?: 1
                            closuresOverTime.forEach { point ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val heightFraction = (point.wonCount.toFloat() / maxCount.toFloat()).coerceIn(0.1f, 1f)
                                    Text(
                                        text = "${point.wonCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (point.wonCount > 0) CyberTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .fillMaxHeight(heightFraction)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(CyberTertiary, CyberSecondary.copy(alpha = 0.5f))
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = point.dateLabel.takeLast(2),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. 48-Hour Inactivity Follow-Up Action Center
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unresponsive_leads_section"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (unresponsiveLeads.isNotEmpty()) CyberWarning.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CyberWarning.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = CyberWarning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "UNRESPONSIVE LEADS (48H+)",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CyberWarning
                                    )
                                    Text(
                                        text = "${unresponsiveLeads.size} leads inactive for 48+ hours",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (unresponsiveLeads.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        viewModel.scheduleAll48HourCheckIns { count, msg ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberWarning, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("schedule_all_checkins_button")
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto-CheckIn All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (unresponsiveLeads.isEmpty()) {
                            Text(
                                text = "Awesome! No leads have been inactive for 48+ hours. Your outreach pipeline is responsive and up to date.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                unresponsiveLeads.forEachIndexed { index, client ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("unresponsive_lead_card_$index"),
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
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = client.name,
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    Text(
                                                        text = client.websiteUrl.ifBlank { client.email },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(CyberWarning.copy(alpha = 0.15f))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "${client.hoursInactive}h inactive",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = CyberWarning
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedLeadForCheckIn = client
                                                        isDraftingCheckIn = true
                                                        viewModel.draft48HourCheckIn(client) { success, template, msg ->
                                                            isDraftingCheckIn = false
                                                            if (success && template != null) {
                                                                draftedCheckInTemplate = template
                                                                editedSubject = template.subject
                                                                editedBody = template.body
                                                            } else {
                                                                scope.launch {
                                                                    snackbarHostState.showSnackbar(msg)
                                                                }
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f).testTag("draft_checkin_button_$index"),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberSecondary, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Draft with Gemini", fontSize = 11.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        viewModel.draft48HourCheckIn(client) { success, template, msg ->
                                                            if (success && template != null) {
                                                                viewModel.send48HourCheckInEmail(client, template) { sendSuccess, sendMsg ->
                                                                    scope.launch {
                                                                        snackbarHostState.showSnackbar(sendMsg)
                                                                    }
                                                                }
                                                            } else {
                                                                scope.launch {
                                                                    snackbarHostState.showSnackbar(msg)
                                                                }
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f).testTag("direct_send_checkin_$index"),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary, contentColor = Color.Black)
                                                ) {
                                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Quick Check-In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        }
    }

    // Review & Dispatch Dialog for drafted 48-hour check-in
    if (selectedLeadForCheckIn != null && draftedCheckInTemplate != null) {
        AlertDialog(
            onDismissRequest = {
                selectedLeadForCheckIn = null
                draftedCheckInTemplate = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberSecondary)
                    Text("Gemini 48-Hour Check-In")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Review and edit the personalized check-in message before dispatching via Brevo to ${selectedLeadForCheckIn?.email.orEmpty().ifBlank { selectedLeadForCheckIn?.name.orEmpty() }}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editedSubject,
                        onValueChange = { editedSubject = it },
                        label = { Text("Subject Line") },
                        modifier = Modifier.fillMaxWidth().testTag("checkin_subject_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editedBody,
                        onValueChange = { editedBody = it },
                        label = { Text("Personalized Email Body") },
                        modifier = Modifier.fillMaxWidth().height(180.dp).testTag("checkin_body_input"),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val client = selectedLeadForCheckIn
                        if (client != null) {
                            val finalTemplate = GeneratedTemplate(subject = editedSubject, body = editedBody)
                            viewModel.send48HourCheckInEmail(client, finalTemplate) { success, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        }
                        selectedLeadForCheckIn = null
                        draftedCheckInTemplate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_send_checkin_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send Check-In Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedLeadForCheckIn = null
                        draftedCheckInTemplate = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isDraftingCheckIn) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Gemini Working...") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = CyberSecondary, modifier = Modifier.size(24.dp))
                    Text("Drafting personalized 48-hour check-in...")
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun AnalyticsKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FunnelStageBar(
    stage: PipelineFunnelStage,
    currencyFormat: NumberFormat,
    index: Int
) {
    val barColor = when (index) {
        0 -> Color(0xFF6366F1) // Indigo (Discovered)
        1 -> Color(0xFF3B82F6) // Blue (Pitched)
        2 -> Color(0xFF06B6D4) // Cyan (Follow-Up)
        3 -> Color(0xFF10B981) // Emerald (Interested)
        4 -> Color(0xFFF59E0B) // Amber (Call Booked)
        5 -> Color(0xFF8B5CF6) // Violet (Proposal Sent)
        else -> Color(0xFF10B981) // Green (Won)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
                Text(
                    text = stage.displayName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (stage.totalValue > 0) {
                    Text(
                        text = currencyFormat.format(stage.totalValue),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${stage.count} deals (${"%.1f".format(stage.conversionRate)}%)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = barColor
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (stage.conversionRate / 100f).coerceIn(0.04f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.15f)
        )
    }
}
