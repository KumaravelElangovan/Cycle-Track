package com.cycletrack.app.ui.history

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycletrack.app.domain.model.CyclingIntensity
import com.cycletrack.app.domain.model.Workout
import com.cycletrack.app.domain.util.DistanceCalculator
import com.cycletrack.app.domain.util.SpeedCalculator
import com.cycletrack.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onRideClick: (Long) -> Unit
) {
    val workouts by viewModel.filteredWorkouts.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val totalDistance by viewModel.totalHistoryDistanceKm.collectAsStateWithLifecycle()
    val totalDuration by viewModel.totalHistoryDurationSeconds.collectAsStateWithLifecycle()
    val totalCalories by viewModel.totalHistoryCalories.collectAsStateWithLifecycle()

    var workoutToDelete by remember { mutableStateOf<Workout?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Deletion confirmation dialog
    if (workoutToDelete != null) {
        val ride = workoutToDelete!!
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            title = { Text("Delete Ride?") },
            text = {
                Text(
                    "Are you sure you want to delete this ride (%.2f km on %s)? This cannot be undone."
                        .format(
                            ride.distanceKm,
                            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ride.startTime))
                        )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkout(ride.id)
                        workoutToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CycleStop)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { workoutToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(CycleDarkBlue, CycleMidBlue)))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Ride History",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${workouts.size} ${if (workouts.size == 1) "ride" else "rides"} recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    // Sort menu button
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            HistorySort.values().forEach { sortOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            sortOption.displayName,
                                            fontWeight = if (sort == sortOption) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sort == sortOption) CycleGreenDark else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSort(sortOption)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (sort == sortOption) {
                                            Icon(Icons.Filled.Check, null, tint = CycleGreenDark)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Filter Chips ──────────────────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HistoryFilter.values()) { filterOption ->
                    FilterChip(
                        selected = filter == filterOption,
                        onClick = { viewModel.setFilter(filterOption) },
                        label = { Text(filterOption.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CycleGreen,
                            selectedLabelColor = CycleDarkBlue
                        )
                    )
                }
            }

            // ── Aggregate Summary Banner ──────────────────────────────────────
            if (workouts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        SummaryColumn(
                            value = DistanceCalculator.formatDistance(totalDistance, userProfile.preferKilometers),
                            label = "Total Dist"
                        )
                        Divider(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                        )
                        SummaryColumn(
                            value = SpeedCalculator.formatDuration(totalDuration),
                            label = "Total Time"
                        )
                        Divider(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                        )
                        SummaryColumn(
                            value = "$totalCalories kcal*",
                            label = "Total Cal"
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Ride List or Empty State ──────────────────────────────────────
            if (workouts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.DirectionsBike,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No rides found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when (filter) {
                                HistoryFilter.ALL -> "You haven't tracked any rides yet. Tap the track button to start your first journey!"
                                else -> "No rides recorded for this period. Try switching filters."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(workouts, key = { it.id }) { workout ->
                        HistoryRideCard(
                            workout = workout,
                            preferKm = userProfile.preferKilometers,
                            preferKmh = userProfile.preferKmh,
                            onClick = { onRideClick(workout.id) },
                            onDelete = { workoutToDelete = workout }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HistoryRideCard(
    workout: Workout,
    preferKm: Boolean,
    preferKmh: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy  h:mm a", Locale.getDefault())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Date + Intensity badge + Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CycleDarkBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.DirectionsBike,
                            contentDescription = null,
                            tint = CycleGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        dateFormat.format(Date(workout.startTime)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IntensityBadge(intensity = workout.intensity)
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Primary Stat: Distance + Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        DistanceCalculator.formatDistance(workout.distanceKm, preferKm),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = CycleGreenDark,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = CycleGreenDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(10.dp))

            // Secondary Stats: Duration, Avg Speed, Calories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill(
                    icon = Icons.Filled.Timer,
                    label = SpeedCalculator.formatDuration(workout.durationSeconds)
                )
                StatPill(
                    icon = Icons.Filled.Speed,
                    label = SpeedCalculator.formatSpeed(workout.avgSpeedKmh, preferKmh)
                )
                StatPill(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "${workout.caloriesBurned} kcal*"
                )
            }
        }
    }
}

@Composable
fun IntensityBadge(intensity: CyclingIntensity) {
    val (badgeBg, badgeText) = when (intensity) {
        CyclingIntensity.EASY -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        CyclingIntensity.MODERATE -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        CyclingIntensity.VIGOROUS -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeBg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            intensity.name.lowercase().capitalize(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = badgeText
        )
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
