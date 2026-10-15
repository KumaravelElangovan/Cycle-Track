package com.cycletrack.app.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cycletrack.app.MainActivity
import com.cycletrack.app.R
import com.cycletrack.app.domain.model.RoutePoint
import com.cycletrack.app.domain.util.DistanceCalculator
import com.cycletrack.app.domain.util.SpeedCalculator
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class CyclingTrackingService : Service() {

    companion object {
        const val ACTION_START   = "ACTION_START"
        const val ACTION_PAUSE   = "ACTION_PAUSE"
        const val ACTION_RESUME  = "ACTION_RESUME"
        const val ACTION_STOP    = "ACTION_STOP"

        const val CHANNEL_ID        = "cycling_tracking_channel"
        const val NOTIFICATION_ID   = 1001
        const val UPDATE_INTERVAL_MS = 3000L
        const val MIN_DISPLACEMENT_M = 5f

        // Singleton state flows — ViewModels observe these
        private val _trackingState = MutableStateFlow<TrackingState>(TrackingState.Idle)
        val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

        private val _liveStats = MutableStateFlow(LiveStats())
        val liveStats: StateFlow<LiveStats> = _liveStats.asStateFlow()

        private val _routePoints = MutableStateFlow<List<RoutePoint>>(emptyList())
        val routePoints: StateFlow<List<RoutePoint>> = _routePoints.asStateFlow()

        fun reset() {
            _trackingState.value = TrackingState.Idle
            _liveStats.value = LiveStats()
            _routePoints.value = emptyList()
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Timer using monotonic elapsed realtime (not affected by clock changes)
    private var startElapsedRealtime: Long = 0L
    private var accumulatedActiveMs: Long = 0L     // time before pauses
    private var lastPauseElapsedRealtime: Long = 0L

    private var lastAcceptedPoint: RoutePoint? = null
    private var totalDistanceKm: Double = 0.0
    private var maxSpeedKmh: Double = 0.0
    private var currentSpeedKmh: Double = 0.0

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (_trackingState.value == TrackingState.Running) {
                updateTimerAndStats()
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setupLocationCallback()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (_trackingState.value != TrackingState.Running) return
                result.lastLocation?.let { location ->
                    processLocationUpdate(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START  -> handleStart()
            ACTION_PAUSE  -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP   -> handleStop()
        }
        return START_STICKY
    }

    private fun handleStart() {
        reset()
        startElapsedRealtime = SystemClock.elapsedRealtime()
        _trackingState.value = TrackingState.Running
        startForeground(NOTIFICATION_ID, buildNotification("GPS tracking active…"))
        requestLocationUpdates()
        handler.post(timerRunnable)
        Log.d("CycleService", "Tracking started")
    }

    private fun handlePause() {
        _trackingState.value = TrackingState.Paused
        lastPauseElapsedRealtime = SystemClock.elapsedRealtime()
        accumulatedActiveMs += (lastPauseElapsedRealtime - startElapsedRealtime)
        stopLocationUpdates()
        handler.removeCallbacks(timerRunnable)
        updateNotification("Paused – tap to resume")
        Log.d("CycleService", "Tracking paused")
    }

    private fun handleResume() {
        startElapsedRealtime = SystemClock.elapsedRealtime()
        _trackingState.value = TrackingState.Running
        // Reset last accepted point to avoid phantom distance jump after pause
        lastAcceptedPoint = null
        requestLocationUpdates()
        handler.post(timerRunnable)
        updateNotification("GPS tracking active…")
        Log.d("CycleService", "Tracking resumed")
    }

    private fun handleStop() {
        accumulatedActiveMs += if (_trackingState.value == TrackingState.Running) {
            SystemClock.elapsedRealtime() - startElapsedRealtime
        } else {
            0L
        }
        val finalSeconds = accumulatedActiveMs / 1000L
        _liveStats.value = _liveStats.value.copy(elapsedSeconds = finalSeconds)
        _trackingState.value = TrackingState.Stopped
        stopLocationUpdates()
        handler.removeCallbacks(timerRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d("CycleService", "Tracking stopped. Distance: ${totalDistanceKm}km")
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISPLACEMENT_M)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun processLocationUpdate(location: Location) {
        val newPoint = RoutePoint(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = location.time,
            altitudeM = location.altitude,
            accuracyM = location.accuracy
        )

        val (newDistance, accepted) = DistanceCalculator.processLocation(
            previous = lastAcceptedPoint,
            current = location,
            accumulated = totalDistanceKm
        )

        if (accepted) {
            totalDistanceKm = newDistance
            lastAcceptedPoint = newPoint
            _routePoints.value = _routePoints.value + newPoint

            // Update speed
            val gpsSpeed = SpeedCalculator.currentSpeedKmh(location)
            currentSpeedKmh = gpsSpeed ?: 0.0
            if (currentSpeedKmh > maxSpeedKmh) maxSpeedKmh = currentSpeedKmh
        }

        updateTimerAndStats()
    }

    private fun updateTimerAndStats() {
        val nowElapsed = SystemClock.elapsedRealtime()
        val activeMs = if (_trackingState.value == TrackingState.Running) {
            accumulatedActiveMs + (nowElapsed - startElapsedRealtime)
        } else {
            accumulatedActiveMs
        }
        val activeSeconds = activeMs / 1000L
        val avgSpeed = SpeedCalculator.averageSpeedKmh(totalDistanceKm, activeSeconds)

        _liveStats.value = _liveStats.value.copy(
            distanceKm = totalDistanceKm,
            currentSpeedKmh = currentSpeedKmh,
            avgSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeedKmh,
            elapsedSeconds = activeSeconds,
            hasGpsSignal = lastAcceptedPoint != null
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_bicycle)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        handler.removeCallbacks(timerRunnable)
    }
}

// ── State Models ──────────────────────────────────────────────────────────────

sealed class TrackingState {
    object Idle    : TrackingState()
    object Running : TrackingState()
    object Paused  : TrackingState()
    object Stopped : TrackingState()
}

data class LiveStats(
    val distanceKm: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val elapsedSeconds: Long = 0L,
    val hasGpsSignal: Boolean = false
)
