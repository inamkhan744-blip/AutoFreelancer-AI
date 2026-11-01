package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonthlyIncomeTrendPoint
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberPrimaryLight
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTertiary
import java.text.NumberFormat
import java.util.Locale

/**
 * Recharts-inspired Interactive Monthly Income Trends Component.
 * Visualizes trailing monthly revenue and deal win rates from converted leads in Firestore.
 */
@Composable
fun MonthlyIncomeTrendsCard(
    monthlyPoints: List<MonthlyIncomeTrendPoint>,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }
    }

    val totalRevenue = remember(monthlyPoints) { monthlyPoints.sumOf { it.revenue } }
    val totalWonDeals = remember(monthlyPoints) { monthlyPoints.sumOf { it.wonDealsCount } }
    val latestGrowth = remember(monthlyPoints) { monthlyPoints.lastOrNull()?.growthRatePercent ?: 0f }
    val maxRevenue = remember(monthlyPoints) {
        (monthlyPoints.maxOfOrNull { it.revenue } ?: 1000.0).coerceAtLeast(500.0)
    }

    var selectedPoint by remember { mutableStateOf<MonthlyIncomeTrendPoint?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_income_trends_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF07141A).copy(alpha = 0.95f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    CyberTertiary.copy(alpha = 0.7f),
                    CyberSecondary.copy(alpha = 0.4f),
                    CyberPrimary.copy(alpha = 0.6f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title & Recharts Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "MONTHLY REVENUE TRENDS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = CyberTertiary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberTertiary.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RECHARTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = CyberTertiary
                            )
                        }
                    }
                    Text(
                        text = "Trailing 6-Month Closed Won Lead Volume & Growth",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CyberTertiary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = CyberTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Converted
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F232D))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "Total Won",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = currencyFormat.format(totalRevenue),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberTertiary
                        )
                    }
                }

                // Won Deals Count
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F232D))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "Closed Deals",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$totalWonDeals Deals",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberSecondary
                        )
                    }
                }

                // MoM Growth
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F232D))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "MoM Velocity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (latestGrowth >= 0f) CyberSuccess else CyberTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (latestGrowth >= 0f) "+${"%.1f".format(latestGrowth)}%" else "${"%.1f".format(latestGrowth)}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (latestGrowth >= 0f) CyberSuccess else CyberTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Tooltip Box
            AnimatedVisibility(
                visible = selectedPoint != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedPoint?.let { point ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF132F3C))
                            .border(1.dp, CyberTertiary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("recharts_tooltip")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${point.monthLabel}: ${currencyFormat.format(point.revenue)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${point.wonDealsCount} Won Deals • ${if (point.growthRatePercent >= 0f) "+${"%.0f".format(point.growthRatePercent)}%" else "${"%.0f".format(point.growthRatePercent)}%"} MoM",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Chart Canvas / Grid Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                // Grid Background Lines
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(1.0f, 0.66f, 0.33f, 0.0f).forEach { fraction ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currencyFormat.format(maxRevenue * fraction),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 8.sp,
                                modifier = Modifier.width(42.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.07f),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }

                // Vertical Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp, end = 8.dp)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyPoints.forEach { point ->
                        val heightFraction = if (maxRevenue > 0) {
                            (point.revenue / maxRevenue).toFloat().coerceIn(0.06f, 1f)
                        } else {
                            0.06f
                        }

                        val isSelected = selectedPoint?.monthKey == point.monthKey

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedPoint = if (isSelected) null else point
                                }
                                .testTag("monthly_trend_bar_${point.monthKey}")
                        ) {
                            if (point.wonDealsCount > 0) {
                                Text(
                                    text = "${point.wonDealsCount}w",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) CyberSecondary else CyberTertiary,
                                    fontSize = 9.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.verticalGradient(
                                                listOf(CyberSecondary, CyberTertiary)
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(
                                                    CyberTertiary,
                                                    CyberSecondary.copy(alpha = 0.6f),
                                                    CyberPrimary.copy(alpha = 0.3f)
                                                )
                                            )
                                        }
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                1.5.dp,
                                                Color.White,
                                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // X-Axis Month Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                monthlyPoints.forEach { point ->
                    val isSelected = selectedPoint?.monthKey == point.monthKey
                    Text(
                        text = point.monthLabel.replace(" '", "'"),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) CyberSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CyberTertiary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Converted Revenue ($)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CyberSecondary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Won Deals Count (w)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}
