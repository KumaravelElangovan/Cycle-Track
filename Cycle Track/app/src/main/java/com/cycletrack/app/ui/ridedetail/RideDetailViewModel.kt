package com.cycletrack.app.ui.ridedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycletrack.app.data.preferences.UserPreferencesManager
import com.cycletrack.app.data.repository.WorkoutRepository
import com.cycletrack.app.domain.model.UserProfile
import com.cycletrack.app.domain.model.Workout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    private val navId: Long? = savedStateHandle.get<Long>("id")

    private val _workout = MutableStateFlow<Workout?>(null)
    val workout: StateFlow<Workout?> = _workout.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val userProfile: StateFlow<UserProfile> = preferencesManager.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    init {
        navId?.let { loadWorkout(it) }
    }

    fun loadWorkout(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _workout.value = workoutRepository.getWorkoutById(id)
            _isLoading.value = false
        }
    }

    fun deleteWorkout(onDeleted: () -> Unit) {
        val current = _workout.value ?: return
        viewModelScope.launch {
            workoutRepository.deleteWorkoutById(current.id)
            onDeleted()
        }
    }
}
