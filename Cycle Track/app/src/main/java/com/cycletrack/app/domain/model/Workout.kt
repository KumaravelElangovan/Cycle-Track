package com.cycletrack.app.domain.model

enum class CyclingIntensity(val displayName: String, val met: Double) {
    EASY("Easy (Leisure)", 4.0),
    MODERATE("Moderate (12–14 km/h)", 8.0),
    VIGOROUS("Vigorous (>22 km/h)", 10.0)
}

data class Workout(
    val id: Long = 0,
    val startTime: Long,          // epoch millis
    val endTime: Long,
    val durationSeconds: Long,    // active moving time (pauses excluded)
    val distanceKm: Double,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val caloriesBurned: Int,
    val intensity: CyclingIntensity,
    val routePoints: List<RoutePoint> = emptyList()
)
