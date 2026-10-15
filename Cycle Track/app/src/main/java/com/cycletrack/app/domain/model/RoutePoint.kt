package com.cycletrack.app.domain.model

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,      // epoch millis
    val altitudeM: Double = 0.0,
    val accuracyM: Float = 0f
)
