package com.cycletrack.app.ui.ridedetail

import android.content.Intent
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycletrack.app.domain.util.DistanceCalculator
import com.cycletrack.app.domain.util.SpeedCalculator
import com.cycletrack.app.ui.history.IntensityBadge
import com.cycletrack.app.ui.theme.*
import org.osmdroid.config.Configuration as OsmConfig
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    workoutId: Long,
    onBack: () -> Unit,
    viewModel: RideDetailViewModel = hiltViewModel()
) {
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Ride?") },
            text = { Text("This will permanently remove this ride from your history.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkout(onDeleted = onBack)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CycleStop)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ride Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        workout?.let {
                            val df = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
                            Text(
                                df.format(Date(it.startTime)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    workout?.let { ride ->
                        IconButton(onClick = {
                            val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ride.startTime))
                            val shareText = "🚴 Just completed a cycling workout on $dateStr!\n\n" +
                                    "📏 Distance: %.2f km\n".format(ride.distanceKm) +
                                    "⏱ Duration: ${SpeedCalculator.formatDuration(ride.durationSeconds)}\n" +
                                    "⚡ Avg Speed: %.1f km/h\n".format(ride.avgSpeedKmh) +
                                    "🔥 Calories: ${ride.caloriesBurned} kcal\n\n" +
                                    "Tracked with CycleTrack 🚴‍♂️"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Ride Details"))
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }

                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CycleGreen)
            }
        } else if (workout == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Ride not found", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            val ride = workout!!
            val preferKm = userProfile.preferKilometers
            val preferKmh = userProfile.preferKmh

            val paceMinPerKm = if (ride.distanceKm > 0) (ride.durationSeconds / 60.0) / ride.distanceKm else 0.0
            val paceMinutes = paceMinPerKm.toInt()
            val paceSeconds = ((paceMinPerKm - paceMinutes) * 60).toInt()
            val paceFormatted = "%d:%02d min/km".format(paceMinutes, paceSeconds)

            val elevationGain = remember(ride.routePoints) {
                var gain = 0.0
                for (i in 1 until ride.routePoints.size) {
                    val diff = ride.routePoints[i].altitudeM - ride.routePoints[i - 1].altitudeM
                    if (diff > 0) gain += diff
                }
                gain
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // ── Map View ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    if (ride.routePoints.size >= 2) {
                        AndroidView(
                            factory = { ctx ->
                                OsmConfig.getInstance().apply {
                                    userAgentValue = "CycleTrack/1.0"
                                    load(ctx, ctx.getSharedPreferences("osm_prefs", 0))
                                }
                                MapView(ctx).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true)

                                    val geoPoints = ride.routePoints.map { GeoPoint(it.latitude, it.longitude) }

                                    val polyline = Polyline().apply {
                                        outlinePaint.color = android.graphics.Color.parseColor("#00C853")
                                        outlinePaint.strokeWidth = 12f
                                        setPoints(geoPoints)
                                    }
                                    overlays.add(polyline)

                                    // Start marker
                                    val startMarker = Marker(this).apply {
                                        position = geoPoints.first()
                                        title = "Start"
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    }
                                    overlays.add(startMarker)

                                    // Finish marker
                                    val endMarker = Marker(this).apply {
                                        position = geoPoints.last()
                                        title = "Finish"
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    }
                                    overlays.add(endMarker)

                                    // Fit bounding box
                                    post {
                                        try {
                                            val box = BoundingBox.fromGeoPoints(geoPoints)
                                            zoomToBoundingBox(box, true, 80)
                                        } catch (e: Exception) {
                                            controller.setZoom(15.0)
                                            controller.setCenter(geoPoints.first())
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.LocationOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No GPS route points recorded for this ride",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Ride Stats ───────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Big Primary Distance Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(listOf(CycleDarkBlue, CycleMidBlue))
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                DistanceCalculator.formatDistance(ride.distanceKm, preferKm),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "TOTAL DISTANCE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                IntensityBadge(intensity = ride.intensity)
                            }
                        }
                    }

                    // Key Metrics Grid
                    Text(
                        "Performance Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Timer,
                            value = SpeedCalculator.formatDuration(ride.durationSeconds),
                            label = "Moving Time"
                        )
                        DetailMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.LocalFireDepartment,
                            value = "${ride.caloriesBurned} kcal*",
                            label = "Calories Burned",
                            iconTint = Color(0xFFFF6F00)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Speed,
                            value = SpeedCalculator.formatSpeed(ride.avgSpeedKmh, preferKmh),
                            label = "Average Speed"
                        )
                        DetailMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.TrendingUp,
                            value = SpeedCalculator.formatSpeed(ride.maxSpeedKmh, preferKmh),
                            label = "Max Speed"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Pace,
                            value = paceFormatted,
                            label = "Average Pace"
                        )
                        DetailMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Landscape,
                            value = if (elevationGain > 0) "+%.0f m".format(elevationGain) else "--",
                            label = "Elevation Gain"
                        )
                    }

                    // Timeline & Notes Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Timing Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Started", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(timeFormat.format(Date(ride.startTime)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ended", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(timeFormat.format(Date(ride.endTime)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GPS Route Points", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${ride.routePoints.size} points", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Text(
                        "* Calories are calculated using standard MET formula based on moving time and selected intensity.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailMetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    iconTint: Color = CycleGreen
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
