package com.cycletrack.app.ui.tracking

import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycletrack.app.data.preferences.UserPreferencesManager
import com.cycletrack.app.data.repository.WorkoutRepository
import com.cycletrack.app.domain.model.CyclingIntensity
import com.cycletrack.app.domain.model.RoutePoint
import com.cycletrack.app.domain.model.UserProfile
import com.cycletrack.app.domain.model.Workout
import com.cycletrack.app.domain.util.CaloriesCalculator
import com.cycletrack.app.service.CyclingTrackingService
import com.cycletrack.app.service.LiveStats
import com.cycletrack.app.service.TrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutRepository: WorkoutRepository,
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    val trackingState: StateFlow<TrackingState> = CyclingTrackingService.trackingState
    val liveStats: StateFlow<LiveStats> = CyclingTrackingService.liveStats
    val routePoints: StateFlow<List<RoutePoint>> = CyclingTrackingService.routePoints

    val userProfile: StateFlow<UserProfile> = preferencesManager.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    /** Estimated calories — recomputed whenever liveStats changes */
    val estimatedCalories: StateFlow<Int> = combine(liveStats, userProfile) { stats, profile ->
        CaloriesCalculator.estimateCalories(
            weightKg = profile.weightKg,
            durationSeconds = stats.elapsedSeconds,
            intensity = profile.intensity
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _savedWorkoutId = MutableStateFlow<Long?>(null)
    val savedWorkoutId: StateFlow<Long?> = _savedWorkoutId.asStateFlow()

    private val _showStopConfirmation = MutableStateFlow(false)
    val showStopConfirmation: StateFlow<Boolean> = _showStopConfirmation.asStateFlow()

    fun startTracking() {
        sendServiceAction(CyclingTrackingService.ACTION_START)
    }

    fun pauseTracking() {
        sendServiceAction(CyclingTrackingService.ACTION_PAUSE)
    }

    fun resumeTracking() {
        sendServiceAction(CyclingTrackingService.ACTION_RESUME)
    }

    fun requestStop() {
        _showStopConfirmation.value = true
    }

    fun dismissStopConfirmation() {
        _showStopConfirmation.value = false
    }

    fun confirmStop() {
        _showStopConfirmation.value = false
        val stats = liveStats.value
        val profile = userProfile.value
        val points = routePoints.value

        // Save workout to Room DB
        viewModelScope.launch {
            val workout = Workout(
                startTime = System.currentTimeMillis() - (stats.elapsedSeconds * 1000),
                endTime = System.currentTimeMillis(),
                durationSeconds = stats.elapsedSeconds,
                distanceKm = stats.distanceKm,
                avgSpeedKmh = stats.avgSpeedKmh,
                maxSpeedKmh = stats.maxSpeedKmh,
                caloriesBurned = CaloriesCalculator.estimateCalories(
                    weightKg = profile.weightKg,
                    durationSeconds = stats.elapsedSeconds,
                    intensity = profile.intensity
                ),
                intensity = profile.intensity,
                routePoints = points
            )
            val id = workoutRepository.saveWorkout(workout)
            _savedWorkoutId.value = id
            sendServiceAction(CyclingTrackingService.ACTION_STOP)
        }
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(context, CyclingTrackingService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
