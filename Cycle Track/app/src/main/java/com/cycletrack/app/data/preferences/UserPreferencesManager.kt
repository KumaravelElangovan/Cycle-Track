package com.cycletrack.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.cycletrack.app.domain.model.CyclingIntensity
import com.cycletrack.app.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val NAME = stringPreferencesKey("name")
        val AGE = intPreferencesKey("age")
        val WEIGHT_KG = floatPreferencesKey("weight_kg")
        val PREFER_KM = booleanPreferencesKey("prefer_km")
        val PREFER_KMH = booleanPreferencesKey("prefer_kmh")
        val DAILY_GOAL_KM = floatPreferencesKey("daily_goal_km")
        val INTENSITY = stringPreferencesKey("intensity")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            UserProfile(
                name = prefs[Keys.NAME] ?: "",
                ageYears = prefs[Keys.AGE] ?: 0,
                weightKg = prefs[Keys.WEIGHT_KG] ?: 70f,
                preferKilometers = prefs[Keys.PREFER_KM] ?: true,
                preferKmh = prefs[Keys.PREFER_KMH] ?: true,
                dailyGoalKm = prefs[Keys.DAILY_GOAL_KM] ?: 10f,
                intensity = try {
                    CyclingIntensity.valueOf(prefs[Keys.INTENSITY] ?: "MODERATE")
                } catch (e: IllegalArgumentException) {
                    CyclingIntensity.MODERATE
                },
                darkMode = prefs[Keys.DARK_MODE] ?: false,
                onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false
            )
        }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name
            prefs[Keys.AGE] = profile.ageYears
            prefs[Keys.WEIGHT_KG] = profile.weightKg
            prefs[Keys.PREFER_KM] = profile.preferKilometers
            prefs[Keys.PREFER_KMH] = profile.preferKmh
            prefs[Keys.DAILY_GOAL_KM] = profile.dailyGoalKm
            prefs[Keys.INTENSITY] = profile.intensity.name
            prefs[Keys.DARK_MODE] = profile.darkMode
            prefs[Keys.ONBOARDING_DONE] = profile.onboardingDone
        }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_DONE] = true
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }
}
