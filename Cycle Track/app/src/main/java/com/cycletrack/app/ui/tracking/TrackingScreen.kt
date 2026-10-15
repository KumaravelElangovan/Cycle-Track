package com.cycletrack.app.ui.tracking

import android.Manifest
import android.content.res.Configuration
import android.view.View
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cycletrack.app.domain.util.SpeedCalculator
import com.cycletrack.app.service.TrackingState
import com.cycletrack.app.ui.theme.*
import com.google.accompanist.permissions.*
import org.osmdroid.config.Configuration as OsmConfig
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TrackingScreen(
    viewModel: TrackingViewModel = hiltViewModel(),
    onNavigateToRideDetail: (Long) -> Unit = {}
) {
    val trackingState by viewModel.trackingState.collectAsStateWithLifecycle()
    val liveStats by viewModel.liveStats.collectAsStateWithLifecycle()
    val routePoints by viewModel.routePoints.collectAsStateWithLifecycle()
    val calories by viewModel.estimatedCalories.collectAsStateWithLifecycle()
    val showStopDialog by viewModel.showStopConfirmation.collectAsStateWithLifecycle()
    val savedWorkoutId by viewModel.savedWorkoutId.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Location permission
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Map reference for polyline updates
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var polyline by remember { mutableStateOf<Polyline?>(null) }

    // Update polyline when route points change
    LaunchedEffect(routePoints) {
        val map = mapView ?: return@LaunchedEffect
        if (routePoints.size >= 2) {
            if (polyline == null) {
                polyline = Polyline().apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#00C853")
                    outlinePaint.strokeWidth = 10f
                }
                map.overlays.add(polyline)
            }
            polyline?.setPoints(routePoints.map { GeoPoint(it.latitude, it.longitude) })
            map.invalidate()
        }
    }

    // Stop confirmation dialog
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissStopConfirmation() },
            title = { Text("End Ride?") },
            text = {
                Text(
                    "Your ride will be saved. Current distance: " +
                            "%.2f km".format(liveStats.distanceKm)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmStop() },
                    colors = ButtonDefaults.buttonColors(containerColor = CycleStop)
                ) { Text("Stop & Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissStopConfirmation() }) {
                    Text("Keep Going")
                }
            }
        )
    }

    // Navigate to ride detail when saved
    LaunchedEffect(savedWorkoutId) {
        savedWorkoutId?.let { id ->
            onNavigateToRideDetail(id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(CycleDarkBlue, CycleMidBlue))
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    "Live Tracking",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when (trackingState) {
                        TrackingState.Running -> "● Recording"
                        TrackingState.Paused  -> "⏸ Paused"
                        TrackingState.Stopped -> "✓ Completed"
                        else -> "Ready to start"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (trackingState) {
                        TrackingState.Running -> CycleGreen
                        TrackingState.Paused  -> CycleWarning
                        else -> Color.White.copy(alpha = 0.7f)
                    }
                )
            }

            // GPS Signal indicator
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (liveStats.hasGpsSignal) Icons.Filled.GpsFixed else Icons.Filled.GpsOff,
                    contentDescription = "GPS",
                    tint = if (liveStats.hasGpsSignal) CycleGreen else Color.Red,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    if (liveStats.hasGpsSignal) "GPS" else "No GPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // ── Map ──────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
        ) {
            if (locationPermission.status.isGranted) {
                AndroidView(
                    factory = { ctx ->
                        OsmConfig.getInstance().apply {
                            userAgentValue = "CycleTrack/1.0"
                            load(ctx, ctx.getSharedPreferences("osm_prefs", 0))
                        }
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(17.0)
                            controller.setCenter(GeoPoint(28.6139, 77.2090)) // Default: New Delhi

                            val locationOverlay = MyLocationNewOverlay(
                                GpsMyLocationProvider(ctx), this
                            ).apply {
                                enableMyLocation()
                                enableFollowLocation()
                            }
                            overlays.add(locationOverlay)
                            mapView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Permission request UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.LocationOff, contentDescription = null,
                        tint = CycleTextSecondary, modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Location permission required for GPS tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { locationPermission.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = CycleGreen)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }

        // ── Live Stats ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Primary stat: Distance
            LiveStatPrimary(
                value = "%.2f".format(liveStats.distanceKm),
                unit = "km",
                label = "DISTANCE"
            )

            // Stats row 1: Current Speed + Avg Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveStatCard(
                    modifier = Modifier.weight(1f),
                    value = "%.1f".format(liveStats.currentSpeedKmh),
                    unit = "km/h",
                    label = "SPEED",
                    icon = Icons.Filled.Speed
                )
                LiveStatCard(
                    modifier = Modifier.weight(1f),
                    value = "%.1f".format(liveStats.avgSpeedKmh),
                    unit = "km/h",
                    label = "AVG SPEED",
                    icon = Icons.Filled.Timeline
                )
            }

            // Stats row 2: Max Speed + Calories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveStatCard(
                    modifier = Modifier.weight(1f),
                    value = "%.1f".format(liveStats.maxSpeedKmh),
                    unit = "km/h",
                    label = "MAX SPEED",
                    icon = Icons.Filled.TrendingUp
                )
                LiveStatCard(
                    modifier = Modifier.weight(1f),
                    value = "$calories",
                    unit = "kcal*",
                    label = "CALORIES",
                    icon = Icons.Filled.LocalFireDepartment,
                    accentColor = Color(0xFFFF6F00)
                )
            }

            // Timer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Timer, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    SpeedCalculator.formatDuration(liveStats.elapsedSeconds),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // ── Control Buttons ───────────────────────────────────────────────
            when (trackingState) {
                TrackingState.Idle -> {
                    if (locationPermission.status.isGranted) {
                        StartButton(onClick = { viewModel.startTracking() })
                    } else {
                        Button(
                            onClick = { locationPermission.launchPermissionRequest() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CycleMidBlue),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.LocationOn, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Enable Location to Start", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                TrackingState.Running -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.pauseTracking() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(2.dp, CyclePause)
                        ) {
                            Icon(Icons.Filled.Pause, null, tint = CyclePause)
                            Spacer(Modifier.width(6.dp))
                            Text("Pause", color = CyclePause, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.requestStop() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CycleStop)
                        ) {
                            Icon(Icons.Filled.Stop, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Stop", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                TrackingState.Paused -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.resumeTracking() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CycleGreen)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Resume", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.requestStop() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CycleStop)
                        ) {
                            Icon(Icons.Filled.Stop, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Stop", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                TrackingState.Stopped -> {
                    Text(
                        "Ride saved! Head to History to view it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CycleGreen,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Text(
                "* Calorie values are estimates based on MET formula, not medical measurements.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ── Composable helpers ────────────────────────────────────────────────────────

@Composable
private fun StartButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CycleGreen),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
        Icon(Icons.Filled.DirectionsBike, null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            "Start Cycling",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun LiveStatPrimary(value: String, unit: String, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(listOf(CycleDarkBlue, CycleMidBlue))
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.titleLarge,
                    color = CycleGreen,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.7f))
        }
    }
}

@Composable
private fun LiveStatCard(
    modifier: Modifier = Modifier,
    value: String,
    unit: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color = CycleGreen
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    " $unit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
