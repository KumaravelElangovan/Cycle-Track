package com.cycletrack.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycletrack.app.data.preferences.UserPreferencesManager
import com.cycletrack.app.domain.model.CyclingIntensity
import com.cycletrack.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = preferencesManager.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    fun updatePreferKilometers(preferKm: Boolean) {
        val current = userProfile.value
        viewModelScope.launch {
            preferencesManager.saveUserProfile(
                current.copy(
                    preferKilometers = preferKm,
                    preferKmh = preferKm // keep speed unit consistent
                )
            )
        }
    }

    fun updateDailyGoal(goalKm: Float) {
        val current = userProfile.value
        viewModelScope.launch {
            preferencesManager.saveUserProfile(current.copy(dailyGoalKm = goalKm))
        }
    }

    fun updateIntensity(intensity: CyclingIntensity) {
        val current = userProfile.value
        viewModelScope.launch {
            preferencesManager.saveUserProfile(current.copy(intensity = intensity))
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
        }
    }

    fun updateProfile(name: String, weightKg: Float, age: Int) {
        val current = userProfile.value
        viewModelScope.launch {
            preferencesManager.saveUserProfile(
                current.copy(
                    name = name,
                    weightKg = weightKg,
                    ageYears = age
                )
            )
        }
    }
}
