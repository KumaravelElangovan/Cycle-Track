package com.cycletrack.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cycletrack.app.data.preferences.UserPreferencesManager
import com.cycletrack.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    /**
     * Continuously observed user profile from DataStore.
     * Initialized with a sensible default until the first emission arrives.
     */
    val userProfile: StateFlow<UserProfile> = preferencesManager.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserProfile()
        )

    /**
     * Persist the given [profile] to DataStore via [UserPreferencesManager].
     */
    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            preferencesManager.saveUserProfile(profile)
        }
    }
}
