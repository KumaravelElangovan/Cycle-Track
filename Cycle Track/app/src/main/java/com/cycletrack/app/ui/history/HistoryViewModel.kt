package com.cycletrack.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycletrack.app.data.preferences.UserPreferencesManager
import com.cycletrack.app.data.repository.WorkoutRepository
import com.cycletrack.app.domain.model.UserProfile
import com.cycletrack.app.domain.model.Workout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class HistoryFilter(val displayName: String) {
    ALL("All Time"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

enum class HistorySort(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    DISTANCE_DESC("Longest Distance"),
    SPEED_DESC("Highest Avg Speed")
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = preferencesManager.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    private val _filter = MutableStateFlow(HistoryFilter.ALL)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(HistorySort.DATE_DESC)
    val sort: StateFlow<HistorySort> = _sort.asStateFlow()

    private val allWorkouts: StateFlow<List<Workout>> = workoutRepository.getAllWorkoutsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredWorkouts: StateFlow<List<Workout>> = combine(allWorkouts, _filter, _sort) { workouts, filter, sort ->
        val filtered = when (filter) {
            HistoryFilter.ALL -> workouts
            HistoryFilter.THIS_WEEK -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfWeekMs = cal.timeInMillis
                workouts.filter { it.startTime >= startOfWeekMs }
            }
            HistoryFilter.THIS_MONTH -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfMonthMs = cal.timeInMillis
                workouts.filter { it.startTime >= startOfMonthMs }
            }
        }

        when (sort) {
            HistorySort.DATE_DESC -> filtered.sortedByDescending { it.startTime }
            HistorySort.DATE_ASC -> filtered.sortedBy { it.startTime }
            HistorySort.DISTANCE_DESC -> filtered.sortedByDescending { it.distanceKm }
            HistorySort.SPEED_DESC -> filtered.sortedByDescending { it.avgSpeedKmh }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalHistoryDistanceKm: StateFlow<Double> = filteredWorkouts.map { list ->
        list.sumOf { it.distanceKm }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalHistoryDurationSeconds: StateFlow<Long> = filteredWorkouts.map { list ->
        list.sumOf { it.durationSeconds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalHistoryCalories: StateFlow<Int> = filteredWorkouts.map { list ->
        list.sumOf { it.caloriesBurned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
    }

    fun setSort(sort: HistorySort) {
        _sort.value = sort
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteWorkoutById(workoutId)
        }
    }
}
