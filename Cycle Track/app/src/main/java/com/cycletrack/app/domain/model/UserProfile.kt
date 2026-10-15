package com.cycletrack.app.domain.model

data class UserProfile(
    val name: String = "",
    val ageYears: Int = 0,
    val weightKg: Float = 70f,
    val preferKilometers: Boolean = true,
    val preferKmh: Boolean = true,
    val dailyGoalKm: Float = 10f,
    val intensity: CyclingIntensity = CyclingIntensity.MODERATE,
    val darkMode: Boolean = false,
    val onboardingDone: Boolean = false
)
