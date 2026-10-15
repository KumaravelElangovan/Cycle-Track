package com.cycletrack.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycletrack.app.domain.model.Workout
import com.cycletrack.app.domain.util.SpeedCalculator
import com.cycletrack.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onStartCycling: () -> Unit,
    onViewHistory: () -> Unit,
    onViewStats: () -> Unit,
    onSettings: () -> Unit,
    onRideClick: (Long) -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val totalDistance by viewModel.totalDistanceKm.collectAsStateWithLifecycle()
    val totalCalories by viewModel.totalCalories.collectAsStateWithLifecycle()
    val totalDuration by viewModel.totalDurationSeconds.collectAsStateWithLifecycle()
    val rideCount by viewModel.rideCount.collectAsStateWithLifecycle()
    val maxSpeed by viewModel.allTimeMaxSpeed.collectAsStateWithLifecycle()
    val avgSpeed by viewModel.avgSpeedKmh.collectAsStateWithLifecycle()
    val recentRides by viewModel.recentWorkouts.collectAsStateWithLifecycle()
    val todayDistance by viewModel.todayDistanceKm.collectAsStateWithLifecycle()
    val weeklyDistances by viewModel.weeklyDistances.collectAsStateWithLifecycle()

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else      -> "Good evening"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(CycleDarkBlue, CycleMidBlue)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CycleGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.DirectionsBike,
                                    contentDescription = null,
                                    tint = CycleDarkBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "CycleTrack",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Filled.Settings, null, tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "$greeting${if (userProfile.name.isNotBlank()) ", ${userProfile.name.split(" ").first()}!" else "!"}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Ready for your ride today?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // ── Start Button ────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            StartCyclingButton(
                onClick = onStartCycling,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // ── Today Progress ──────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            val goalKm = userProfile.dailyGoalKm.toDouble()
            val progress = if (goalKm > 0) (todayDistance / goalKm).toFloat().coerceIn(0f, 1f) else 0f
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Today's Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("%.2f km".format(todayDistance), fontWeight = FontWeight.SemiBold, color = CycleGreen)
                        Text("Goal: ${userProfile.dailyGoalKm} km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = CycleGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${(progress * 100).toInt()}% of daily goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ── Stats Grid ──────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(20.dp))
            Text(
                "All-Time Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            // 2-column grid
            val statItems = listOf(
                Triple(Icons.Filled.Route, "%.1f km".format(totalDistance), "Total Distance"),
                Triple(Icons.Filled.LocalFireDepartment, "$totalCalories kcal*", "Total Calories"),
                Triple(Icons.Filled.Timer, SpeedCalculator.formatDuration(totalDuration), "Total Time"),
                Triple(Icons.Filled.Speed, "%.1f km/h".format(avgSpeed), "Avg Speed"),
                Triple(Icons.Filled.TrendingUp, "%.1f km/h".format(maxSpeed), "Max Speed"),
                Triple(Icons.Filled.DirectionsBike, "$rideCount", "Total Rides")
            )
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                statItems.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { (icon, value, label) ->
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = icon,
                                value = value,
                                label = label
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Weekly Chart ────────────────────────────────────────────────────
        if (weeklyDistances.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "This Week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    WeeklyBarChart(
                        data = weeklyDistances,
                        goalKm = userProfile.dailyGoalKm.toDouble(),
                        modifier = Modifier.fillMaxWidth().height(160.dp).padding(16.dp)
                    )
                }
            }
        }

        // ── Recent Rides ────────────────────────────────────────────────────
        if (recentRides.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Rides",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onViewHistory) {
                        Text("See All", color = CycleGreen)
                    }
                }
            }
            items(recentRides.take(3)) { workout ->
                WorkoutListItem(
                    workout = workout,
                    onClick = { onRideClick(workout.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        } else {
            item {
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.DirectionsBike, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No rides yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Start cycling to see your history here!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StartCyclingButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "scale"
    )
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(64.dp).scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CycleGreen),
        elevation = ButtonDefaults.buttonElevation(8.dp)
    ) {
        Icon(Icons.Filled.DirectionsBike, null, modifier = Modifier.size(28.dp), tint = CycleDarkBlue)
        Spacer(Modifier.width(12.dp))
        Text(
            "Start Cycling",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = CycleDarkBlue
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CycleGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = CycleGreen, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun WorkoutListItem(
    workout: Workout,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault())
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CycleDarkBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.DirectionsBike, null, tint = CycleGreen, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(dateFormat.format(Date(workout.startTime)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("%.2f km".format(workout.distanceKm), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(SpeedCalculator.formatDuration(workout.durationSeconds), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${workout.caloriesBurned} kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("%.1f".format(workout.avgSpeedKmh), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CycleGreen)
                Text("km/h avg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(
    data: List<Pair<String, Double>>,
    goalKm: Double,
    modifier: Modifier = Modifier
) {
    val greenColor = CycleGreen
    val goalColor = CyclePause
    val labelColor = CycleTextSecondary

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2)
        val maxVal = maxOf(data.maxOfOrNull { it.second } ?: 1.0, goalKm, 1.0)
        val chartHeight = size.height * 0.75f
        val labelHeight = size.height * 0.25f

        // Goal line
        val goalY = chartHeight - (goalKm / maxVal * chartHeight).toFloat()
        drawLine(
            color = goalColor,
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        data.forEachIndexed { index, (label, distance) ->
            val barHeight = (distance / maxVal * chartHeight).toFloat()
            val x = index * (size.width / data.size) + barWidth / 2
            val barTop = chartHeight - barHeight

            // Bar
            drawRoundRect(
                color = if (distance > 0) greenColor else greenColor.copy(alpha = 0.15f),
                topLeft = Offset(x, barTop),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight.coerceAtLeast(4f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
        }
    }
}
