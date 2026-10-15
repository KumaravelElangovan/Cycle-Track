package com.cycletrack.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycletrack.app.data.preferences.UserPreferencesManager
import com.cycletrack.app.data.repository.WorkoutRepository
import com.cycletrack.app.domain.model.UserProfile
import com.cycletrack.app.domain.model.Workout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = preferencesManager.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val totalDistanceKm: StateFlow<Double> = workoutRepository.getTotalDistanceKmFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCalories: StateFlow<Int> = workoutRepository.getTotalCaloriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalDurationSeconds: StateFlow<Long> = workoutRepository.getTotalDurationSecondsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val rideCount: StateFlow<Int> = workoutRepository.getTotalRideCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allTimeMaxSpeed: StateFlow<Double> = workoutRepository.getAllTimeMaxSpeedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _allWorkouts = workoutRepository.getAllWorkoutsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentWorkouts: StateFlow<List<Workout>> = _allWorkouts
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val avgSpeedKmh: StateFlow<Double> = _allWorkouts
        .map { workouts ->
            val totalDist = workouts.sumOf { it.distanceKm }
            val totalTime = workouts.sumOf { it.durationSeconds }
            if (totalTime > 0) totalDist / (totalTime / 3600.0) else 0.0
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _todayDistanceKm = MutableStateFlow(0.0)
    val todayDistanceKm: StateFlow<Double> = _todayDistanceKm.asStateFlow()

    // Last 7 days (day label, distance km)
    private val _weeklyDistances = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val weeklyDistances: StateFlow<List<Pair<String, Double>>> = _weeklyDistances.asStateFlow()

    init {
        refreshDailyStats()
        // Also refresh when workouts change
        viewModelScope.launch {
            _allWorkouts.collect { refreshDailyStats() }
        }
    }

    private fun refreshDailyStats() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()

            // Today's distance
            calendar.time = Date(now)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfToday = calendar.timeInMillis
            _todayDistanceKm.value = workoutRepository.getDistanceSince(startOfToday)

            // Weekly distances (last 7 days)
            val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val weeklyData = mutableListOf<Pair<String, Double>>()
            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = cal.timeInMillis

                val distance = workoutRepository.getWorkoutsInRange(startOfDay, endOfDay)
                    .sumOf { it.distanceKm }

                val dayOfWeek = Calendar.getInstance().apply {
                    timeInMillis = startOfDay
                }.get(Calendar.DAY_OF_WEEK)
                val label = when (dayOfWeek) {
                    Calendar.MONDAY -> "Mon"
                    Calendar.TUESDAY -> "Tue"
                    Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"
                    Calendar.FRIDAY -> "Fri"
                    Calendar.SATURDAY -> "Sat"
                    else -> "Sun"
                }
                weeklyData.add(Pair(label, distance))
            }
            _weeklyDistances.value = weeklyData
        }
    }
}
