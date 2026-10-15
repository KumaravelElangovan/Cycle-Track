package com.cycletrack.app.domain.util

import android.location.Location
import com.cycletrack.app.domain.model.RoutePoint
import kotlin.math.*

object DistanceCalculator {

    /** Maximum GPS accuracy threshold (meters). Points worse than this are ignored. */
    private const val MAX_ACCURACY_M = 30f

    /** Maximum realistic cycling speed (km/h). Jumps above this are filtered out. */
    private const val MAX_REALISTIC_SPEED_KMH = 150.0

    /**
     * Calculate the Haversine distance in kilometers between two lat/lon pairs.
     */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    /**
     * Add a new GPS location to the running total distance.
     *
     * @param previous    The last accepted RoutePoint (null if this is the first point).
     * @param current     The newly received Location from GPS.
     * @param accumulated Current total accumulated distance in km.
     * @return Pair of (new accumulated distance km, whether point was accepted).
     */
    fun processLocation(
        previous: RoutePoint?,
        current: Location,
        accumulated: Double
    ): Pair<Double, Boolean> {
        // 1. Filter inaccurate points
        if (current.accuracy > MAX_ACCURACY_M) {
            return Pair(accumulated, false)
        }

        // 2. First point — accept but don't add distance
        if (previous == null) {
            return Pair(accumulated, true)
        }

        // 3. Compute segment distance
        val segmentKm = haversineKm(
            previous.latitude, previous.longitude,
            current.latitude, current.longitude
        )

        // 4. Compute elapsed time seconds between two points
        val elapsedSec = (current.time - previous.timestamp) / 1000.0
        if (elapsedSec <= 0) return Pair(accumulated, false)

        // 5. Compute implied speed for this segment (km/h)
        val impliedSpeedKmh = (segmentKm / (elapsedSec / 3600.0))
        if (impliedSpeedKmh > MAX_REALISTIC_SPEED_KMH) {
            return Pair(accumulated, false)
        }

        return Pair(accumulated + segmentKm, true)
    }

    /**
     * Convert km to miles.
     */
    fun kmToMiles(km: Double): Double = km * 0.621371

    fun formatDistance(km: Double, useKm: Boolean): String {
        return if (useKm) {
            "%.2f km".format(km)
        } else {
            "%.2f mi".format(kmToMiles(km))
        }
    }
}
