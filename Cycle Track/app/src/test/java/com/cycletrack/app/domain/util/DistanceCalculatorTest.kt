package com.cycletrack.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceCalculatorTest {

    @Test
    fun haversineKm_sameCoordinates_returnsZero() {
        val dist = DistanceCalculator.haversineKm(51.5074, 0.1278, 51.5074, 0.1278)
        assertEquals(0.0, dist, 0.0001)
    }

    @Test
    fun haversineKm_knownDistance_returnsAccurateKm() {
        // Paris (48.8566, 2.3522) to London (51.5074, -0.1278) is approx ~343-344 km
        val dist = DistanceCalculator.haversineKm(48.8566, 2.3522, 51.5074, -0.1278)
        assertTrue(dist > 340.0 && dist < 346.0)
    }

    @Test
    fun kmToMiles_convertsCorrectly() {
        val miles = DistanceCalculator.kmToMiles(10.0)
        assertEquals(6.21371, miles, 0.001)
    }

    @Test
    fun formatDistance_kmAndMiles() {
        assertEquals("12.35 km", DistanceCalculator.formatDistance(12.3456, useKm = true))
        assertEquals("7.67 mi", DistanceCalculator.formatDistance(12.3456, useKm = false))
    }
}
