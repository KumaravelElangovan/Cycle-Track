package com.cycletrack.app.domain.util

import com.cycletrack.app.domain.model.CyclingIntensity
import org.junit.Assert.assertEquals
import org.junit.Test

class CaloriesCalculatorTest {

    @Test
    fun estimateCalories_zeroDuration_returnsZero() {
        val calories = CaloriesCalculator.estimateCalories(
            weightKg = 70f,
            durationSeconds = 0L,
            intensity = CyclingIntensity.MODERATE
        )
        assertEquals(0, calories)
    }

    @Test
    fun estimateCalories_zeroWeight_returnsZero() {
        val calories = CaloriesCalculator.estimateCalories(
            weightKg = 0f,
            durationSeconds = 3600L,
            intensity = CyclingIntensity.MODERATE
        )
        assertEquals(0, calories)
    }

    @Test
    fun estimateCalories_oneHourModerate70kg_calculatesCorrectly() {
        // MET for MODERATE is 8.0
        // Calories = 8.0 * 70 * 1 = 560
        val calories = CaloriesCalculator.estimateCalories(
            weightKg = 70f,
            durationSeconds = 3600L,
            intensity = CyclingIntensity.MODERATE
        )
        assertEquals(560, calories)
    }

    @Test
    fun estimateCalories_oneHourEasy70kg_calculatesCorrectly() {
        // MET for EASY is 4.0
        // Calories = 4.0 * 70 * 1 = 280
        val calories = CaloriesCalculator.estimateCalories(
            weightKg = 70f,
            durationSeconds = 3600L,
            intensity = CyclingIntensity.EASY
        )
        assertEquals(280, calories)
    }

    @Test
    fun estimateCalories_oneHourVigorous80kg_calculatesCorrectly() {
        // MET for VIGOROUS is 10.0
        // Calories = 10.0 * 80 * 1 = 800
        val calories = CaloriesCalculator.estimateCalories(
            weightKg = 80f,
            durationSeconds = 3600L,
            intensity = CyclingIntensity.VIGOROUS
        )
        assertEquals(800, calories)
    }

    @Test
    fun formatCalories_appendsKcal() {
        val formatted = CaloriesCalculator.formatCalories(450)
        assertEquals("450 kcal*", formatted)
    }
}
