package com.cycletrack.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycletrack.app.data.preferences.UserPreferencesManager
import com.cycletrack.app.data.repository.WorkoutRepository
import com.cycletrack.app.domain.model.CyclingIntensity
import com.cycletrack.app.domain.model.UserProfile
import com.cycletrack.app.domain.model.Workout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class StatsPeriod(val displayName: String) {
    WEEK("Week"),
    MONTH("Month"),
    ALL_TIME("All Time")
}

data class DayStat(
    val label: String,
    val distanceKm: Double,
    val dateEpochMs: Long
)

data class PersonalRecords(
    val longestRideKm: Double = 0.0,
    val longestRideDateMs: Long = 0L,
    val fastestAvgSpeedKmh: Double = 0.0,
    val fastestRideDateMs: Long = 0L,
    val maxSpeedKmh: Double = 0.0,
    val highestCalories: Int = 0,
    val longestDurationSeconds: Long = 0L
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = preferencesManager.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.WEEK)
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod.asStateFlow()

    private val allWorkouts: StateFlow<List<Workout>> = workoutRepository.getAllWorkoutsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalRecords: StateFlow<PersonalRecords> = allWorkouts.map { workouts ->
        if (workouts.isEmpty()) {
            PersonalRecords()
        } else {
            val longest = workouts.maxByOrNull { it.distanceKm }
            val fastestAvg = workouts.maxByOrNull { it.avgSpeedKmh }
            val maxSpeed = workouts.maxOfOrNull { it.maxSpeedKmh } ?: 0.0
            val maxCal = workouts.maxOfOrNull { it.caloriesBurned } ?: 0
            val longestTime = workouts.maxOfOrNull { it.durationSeconds } ?: 0L

            PersonalRecords(
                longestRideKm = longest?.distanceKm ?: 0.0,
                longestRideDateMs = longest?.startTime ?: 0L,
                fastestAvgSpeedKmh = fastestAvg?.avgSpeedKmh ?: 0.0,
                fastestRideDateMs = fastestAvg?.startTime ?: 0L,
                maxSpeedKmh = maxSpeed,
                highestCalories = maxCal,
                longestDurationSeconds = longestTime
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonalRecords())

    val chartData: StateFlow<List<DayStat>> = combine(allWorkouts, _selectedPeriod) { workouts, period ->
        when (period) {
            StatsPeriod.WEEK -> {
                // Last 7 days including today
                val calendar = Calendar.getInstance()
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                (6 downTo 0).map { daysAgo ->
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -daysAgo)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val dayStart = cal.timeInMillis
                    val dayEnd = dayStart + 86400000L
                    val dayDistance = workouts
                        .filter { it.startTime in dayStart until dayEnd }
                        .sumOf { it.distanceKm }
                    DayStat(
                        label = dayFormat.format(cal.time),
                        distanceKm = dayDistance,
                        dateEpochMs = dayStart
                    )
                }
            }
            StatsPeriod.MONTH -> {
                // 4 weekly blocks of the current month
                val calendar = Calendar.getInstance()
                val weekFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                (3 downTo 0).map { weeksAgo ->
                    val calStart = Calendar.getInstance().apply {
                        add(Calendar.WEEK_OF_YEAR, -weeksAgo)
                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val weekStart = calStart.timeInMillis
                    val weekEnd = weekStart + (7 * 86400000L)
                    val weekDistance = workouts
                        .filter { it.startTime in weekStart until weekEnd }
                        .sumOf { it.distanceKm }
                    DayStat(
                        label = "W${4 - weeksAgo}",
                        distanceKm = weekDistance,
                        dateEpochMs = weekStart
                    )
                }
            }
            StatsPeriod.ALL_TIME -> {
                // Last 6 months
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                (5 downTo 0).map { monthsAgo ->
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.MONTH, -monthsAgo)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val monthStart = cal.timeInMillis
                    val nextMonthCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    val monthEnd = nextMonthCal.timeInMillis

                    val monthDistance = workouts
                        .filter { it.startTime in monthStart until monthEnd }
                        .sumOf { it.distanceKm }

                    DayStat(
                        label = monthFormat.format(cal.time),
                        distanceKm = monthDistance,
                        dateEpochMs = monthStart
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intensityCounts: StateFlow<Map<CyclingIntensity, Int>> = allWorkouts.map { workouts ->
        val counts = mutableMapOf(
            CyclingIntensity.EASY to 0,
            CyclingIntensity.MODERATE to 0,
            CyclingIntensity.VIGOROUS to 0
        )
        workouts.forEach {
            counts[it.intensity] = (counts[it.intensity] ?: 0) + 1
        }
        counts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val periodTotals: StateFlow<Triple<Double, Long, Int>> = combine(allWorkouts, _selectedPeriod) { workouts, period ->
        val filtered = when (period) {
            StatsPeriod.WEEK -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                }
                workouts.filter { it.startTime >= cal.timeInMillis }
            }
            StatsPeriod.MONTH -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                }
                workouts.filter { it.startTime >= cal.timeInMillis }
            }
            StatsPeriod.ALL_TIME -> workouts
        }

        val totalDist = filtered.sumOf { it.distanceKm }
        val totalSec = filtered.sumOf { it.durationSeconds }
        val totalCal = filtered.sumOf { it.caloriesBurned }
        Triple(totalDist, totalSec, totalCal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0.0, 0L, 0))

    fun setPeriod(period: StatsPeriod) {
        _selectedPeriod.value = period
    }
}
