package com.cycletrack.app.domain.util

import android.location.Location

object SpeedCalculator {

    /** Maximum valid speed in km/h to filter GPS noise */
    private const val MAX_VALID_SPEED_KMH = 150.0

    /**
     * Extract current speed in km/h from a GPS Location.
     * Returns null if speed is unavailable or exceeds the valid threshold.
     *
     * @param location  The GPS Location object.
     * @return Speed in km/h, or null if unreliable.
     */
    fun currentSpeedKmh(location: Location): Double? {
        if (!location.hasSpeed()) return null
        val speedKmh = location.speed * 3.6  // m/s → km/h
        return if (speedKmh > MAX_VALID_SPEED_KMH || speedKmh < 0) null else speedKmh
    }

    /**
     * Calculate average speed from total distance and total moving time.
     *
     * @param totalDistanceKm Total distance cycled in km.
     * @param movingTimeSeconds Active (non-paused) seconds.
     * @return Average speed in km/h.
     */
    fun averageSpeedKmh(totalDistanceKm: Double, movingTimeSeconds: Long): Double {
        if (movingTimeSeconds <= 0 || totalDistanceKm <= 0) return 0.0
        val hours = movingTimeSeconds / 3600.0
        return totalDistanceKm / hours
    }

    /**
     * Convert km/h to mph.
     */
    fun kmhToMph(kmh: Double): Double = kmh * 0.621371

    /**
     * Format a speed value with units.
     */
    fun formatSpeed(kmh: Double, useKmh: Boolean): String {
        return if (useKmh) {
            "%.1f km/h".format(kmh)
        } else {
            "%.1f mph".format(kmhToMph(kmh))
        }
    }

    /**
     * Format elapsed seconds as HH:MM:SS.
     */
    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
