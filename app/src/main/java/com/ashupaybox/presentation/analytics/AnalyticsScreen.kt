package com.ashupaybox.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashupaybox.core.model.PaymentMethod
import com.ashupaybox.core.util.IndianCurrencyFormatter
import com.ashupaybox.presentation.home.MetricSubItem
import com.ashupaybox.presentation.home.StatCard
import com.ashupaybox.presentation.theme.AccentBlue
import com.ashupaybox.presentation.theme.AmberGold
import com.ashupaybox.presentation.theme.EmeraldGreen
import com.ashupaybox.presentation.theme.EmeraldGreenDark

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP HEADER
        item {
            Column {
                Text(
                    text = "Revenue Analytics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Real-time earnings and channel breakdown",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. SUMMARY KPI CARDS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Today's Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricSubItem(label = "Today Revenue", value = IndianCurrencyFormatter.formatRupees(uiState.todayTotalPaise))
                        MetricSubItem(label = "Payments Today", value = "${uiState.todayTxCount}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricSubItem(label = "Average Transaction", value = IndianCurrencyFormatter.formatRupees(uiState.todayAvgPaise))
                        MetricSubItem(label = "Highest Payment", value = IndianCurrencyFormatter.formatRupees(uiState.todayMaxPaise))
                    }
                }
            }
        }

        // 3. HOURLY REVENUE CHART
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hourly Revenue Today",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Peak: ${IndianCurrencyFormatter.formatRupees(uiState.maxHourlyPaise)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen
                        )
                    }

                    // Native Compose Canvas Bar Chart
                    HourlyCanvasChart(
                        hourlyPoints = uiState.hourlyPoints,
                        maxAmountPaise = uiState.maxHourlyPaise
                    )
                }
            }
        }

        // 4. LAST 7 DAYS REVENUE CHART
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Last 7 Days Revenue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    DailyCanvasChart(
                        dailyPoints = uiState.dailyPoints,
                        maxAmountPaise = uiState.maxDailyPaise
                    )
                }
            }
        }

        // 5. PAYMENT METHOD DISTRIBUTION
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Payment Method Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (uiState.methodBreakdowns.isEmpty()) {
                        Text(
                            text = "No method data available yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.methodBreakdowns.forEach { breakdown ->
                            MethodProgressRow(breakdown = breakdown)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyCanvasChart(
    hourlyPoints: List<HourlyPoint>,
    maxAmountPaise: Long
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val width = size.width
            val height = size.height
            val barCount = 24
            val spacing = 3.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = (width - totalSpacing) / barCount

            hourlyPoints.forEachIndexed { index, point ->
                val fraction = (point.amountPaise.toFloat() / maxAmountPaise.toFloat()).coerceIn(0.04f, 1.0f)
                val barHeight = height * fraction
                val left = index * (barWidth + spacing)
                val top = height - barHeight

                val isPeak = point.amountPaise == maxAmountPaise && point.amountPaise > 0
                val color = if (isPeak) EmeraldGreen else if (point.amountPaise > 0) EmeraldGreen.copy(alpha = 0.6f) else Color.DarkGray.copy(alpha = 0.3f)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        // Hour labels (00, 06, 12, 18, 23)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("00:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("06:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("12:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("18:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("23:59", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DailyCanvasChart(
    dailyPoints: List<DailyPoint>,
    maxAmountPaise: Long
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val width = size.width
            val height = size.height
            val barCount = dailyPoints.size
            if (barCount == 0) return@Canvas

            val spacing = 12.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = (width - totalSpacing) / barCount

            dailyPoints.forEachIndexed { index, point ->
                val fraction = (point.amountPaise.toFloat() / maxAmountPaise.toFloat()).coerceIn(0.05f, 1.0f)
                val barHeight = height * fraction
                val left = index * (barWidth + spacing)
                val top = height - barHeight

                val isToday = point.dayLabel == "Today"
                val color = if (isToday) EmeraldGreen else AccentBlue.copy(alpha = 0.7f)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        // Daily labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailyPoints.forEach { point ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = point.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (point.dayLabel == "Today") FontWeight.Bold else FontWeight.Normal,
                        color = if (point.dayLabel == "Today") EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MethodProgressRow(breakdown: MethodBreakdown) {
    val methodColor = when (breakdown.method) {
        PaymentMethod.UPI -> EmeraldGreen
        PaymentMethod.CARD -> AmberGold
        PaymentMethod.WALLET -> AccentBlue
        PaymentMethod.NETBANKING -> Color(0xFFAB47BC)
        PaymentMethod.OTHER -> Color(0xFF78909C)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(methodColor)
                )
                Text(
                    text = breakdown.method.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = IndianCurrencyFormatter.formatRupees(breakdown.amountPaise),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(${String.format(java.util.Locale.US, "%.1f", breakdown.percentage)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LinearProgressIndicator(
            progress = { (breakdown.percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = methodColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
