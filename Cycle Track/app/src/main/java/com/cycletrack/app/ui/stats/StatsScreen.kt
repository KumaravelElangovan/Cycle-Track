package com.cycletrack.app.ui.stats

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycletrack.app.domain.model.CyclingIntensity
import com.cycletrack.app.domain.util.DistanceCalculator
import com.cycletrack.app.domain.util.SpeedCalculator
import com.cycletrack.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val chartData by viewModel.chartData.collectAsStateWithLifecycle()
    val prs by viewModel.personalRecords.collectAsStateWithLifecycle()
    val intensityCounts by viewModel.intensityCounts.collectAsStateWithLifecycle()
    val totals by viewModel.periodTotals.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val preferKm = userProfile.preferKilometers
    val preferKmh = userProfile.preferKmh

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Statistics & Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = 24.dp)
        ) {
            // ── Period Selector Tab ──────────────────────────────────────────
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                StatsPeriod.values().forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.setPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = StatsPeriod.values().size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = CycleGreen,
                            activeContentColor = CycleDarkBlue
                        )
                    ) {
                        Text(period.displayName, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Totals Overview Card ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(listOf(CycleDarkBlue, CycleMidBlue))
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "${selectedPeriod.displayName.uppercase()} TOTALS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        DistanceCalculator.formatDistance(totals.first, preferKm),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Timer, null, tint = CycleGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                SpeedCalculator.formatDuration(totals.second),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFFF6F00), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${totals.third} kcal*",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Distance Chart ───────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Text(
                "Distance Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatsDistanceChart(
                        data = chartData,
                        goalKm = if (selectedPeriod == StatsPeriod.WEEK) userProfile.dailyGoalKm.toDouble() else 0.0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            // ── Personal Records (PRs) ───────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            Text(
                "Personal Records 🏆",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

                RecordItem(
                    icon = Icons.Filled.Route,
                    title = "Longest Distance",
                    value = DistanceCalculator.formatDistance(prs.longestRideKm, preferKm),
                    subtitle = if (prs.longestRideDateMs > 0) dateFormat.format(Date(prs.longestRideDateMs)) else "No rides yet"
                )
                RecordItem(
                    icon = Icons.Filled.Speed,
                    title = "Fastest Average Speed",
                    value = SpeedCalculator.formatSpeed(prs.fastestAvgSpeedKmh, preferKmh),
                    subtitle = if (prs.fastestRideDateMs > 0) dateFormat.format(Date(prs.fastestRideDateMs)) else "No rides yet"
                )
                RecordItem(
                    icon = Icons.Filled.TrendingUp,
                    title = "Top Speed Reached",
                    value = SpeedCalculator.formatSpeed(prs.maxSpeedKmh, preferKmh),
                    subtitle = "All-time maximum recorded"
                )
                RecordItem(
                    icon = Icons.Filled.LocalFireDepartment,
                    title = "Most Calories Burned",
                    value = "${prs.highestCalories} kcal*",
                    subtitle = "Single session peak",
                    iconColor = Color(0xFFFF6F00)
                )
                RecordItem(
                    icon = Icons.Filled.Timer,
                    title = "Longest Duration",
                    value = SpeedCalculator.formatDuration(prs.longestDurationSeconds),
                    subtitle = "Longest active riding time"
                )
            }

            // ── Intensity Distribution ───────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            Text(
                "Intensity Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))

            val totalCount = intensityCounts.values.sum().coerceAtLeast(1)
            val easyCount = intensityCounts[CyclingIntensity.EASY] ?: 0
            val modCount = intensityCounts[CyclingIntensity.MODERATE] ?: 0
            val vigCount = intensityCounts[CyclingIntensity.VIGOROUS] ?: 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Segmented color bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        if (intensityCounts.values.sum() == 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        } else {
                            if (easyCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(easyCount.toFloat())
                                        .fillMaxHeight()
                                        .background(CycleGreen)
                                )
                            }
                            if (modCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(modCount.toFloat())
                                        .fillMaxHeight()
                                        .background(Color(0xFF1E88E5))
                                )
                            }
                            if (vigCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(vigCount.toFloat())
                                        .fillMaxHeight()
                                        .background(Color(0xFFFB8C00))
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IntensityLegendItem(color = CycleGreen, label = "Easy", count = easyCount, total = totalCount)
                        IntensityLegendItem(color = Color(0xFF1E88E5), label = "Moderate", count = modCount, total = totalCount)
                        IntensityLegendItem(color = Color(0xFFFB8C00), label = "Vigorous", count = vigCount, total = totalCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    iconColor: Color = CycleGreen
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun IntensityLegendItem(color: Color, label: String, count: Int, total: Int) {
    val pct = (count.toFloat() / total * 100).toInt()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Text("$count ($pct%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatsDistanceChart(
    data: List<DayStat>,
    goalKm: Double,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val barColor = CycleGreen
    val goalColor = CyclePause
    val labelColor = CycleTextSecondary

    Canvas(modifier = modifier) {
        val maxVal = maxOf(data.maxOfOrNull { it.distanceKm } ?: 1.0, goalKm, 1.0)
        val chartHeight = size.height * 0.75f
        val stepX = size.width / data.size
        val barWidth = stepX * 0.5f

        // Optional Goal line
        if (goalKm > 0) {
            val goalY = chartHeight - (goalKm / maxVal * chartHeight).toFloat()
            drawLine(
                color = goalColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        data.forEachIndexed { i, stat ->
            val barHeight = (stat.distanceKm / maxVal * chartHeight).toFloat()
            val left = i * stepX + (stepX - barWidth) / 2f
            val top = chartHeight - barHeight

            // Draw Bar
            drawRoundRect(
                color = if (stat.distanceKm > 0) barColor else barColor.copy(alpha = 0.15f),
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }

    // X-Axis Labels Row
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        data.forEach { stat ->
            Text(
                stat.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
