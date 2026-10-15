package com.cycletrack.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedCalculatorTest {

    @Test
    fun averageSpeedKmh_validValues_calculatesCorrectly() {
        // 20 km in 3600 seconds (1 hour) = 20.0 km/h
        val avg = SpeedCalculator.averageSpeedKmh(20.0, 3600L)
        assertEquals(20.0, avg, 0.001)
    }

    @Test
    fun averageSpeedKmh_zeroDuration_returnsZero() {
        val avg = SpeedCalculator.averageSpeedKmh(20.0, 0L)
        assertEquals(0.0, avg, 0.001)
    }

    @Test
    fun averageSpeedKmh_zeroDistance_returnsZero() {
        val avg = SpeedCalculator.averageSpeedKmh(0.0, 3600L)
        assertEquals(0.0, avg, 0.001)
    }

    @Test
    fun kmhToMph_convertsCorrectly() {
        val mph = SpeedCalculator.kmhToMph(10.0)
        assertEquals(6.21371, mph, 0.001)
    }

    @Test
    fun formatDuration_variousSeconds_formatsHHMMSS() {
        assertEquals("00:00:00", SpeedCalculator.formatDuration(0L))
        assertEquals("00:01:05", SpeedCalculator.formatDuration(65L))
        assertEquals("01:00:00", SpeedCalculator.formatDuration(3600L))
        assertEquals("02:30:15", SpeedCalculator.formatDuration(9015L))
    }
}
