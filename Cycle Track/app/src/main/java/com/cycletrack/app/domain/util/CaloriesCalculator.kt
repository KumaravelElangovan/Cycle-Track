package com.cycletrack.app.domain.util

import com.cycletrack.app.domain.model.CyclingIntensity

object CaloriesCalculator {

    /**
     * Estimate calories burned using MET-based formula.
     *
     * Calories = MET × weight(kg) × duration(hours)
     *
     * MET values:
     *   EASY      = 4.0  (leisure cycling, <12 km/h)
     *   MODERATE  = 8.0  (12–14 km/h effort)
     *   VIGOROUS  = 10.0 (>22 km/h, racing pace)
     *
     * Note: These are estimates. Actual caloric expenditure varies by
     * individual physiology, terrain, and equipment.
     *
     * @param weightKg       User body weight in kilograms.
     * @param durationSeconds Active (non-paused) workout time in seconds.
     * @param intensity       Cycling intensity level selected by user.
     * @return Estimated calories burned (kcal), rounded to nearest integer.
     */
    fun estimateCalories(
        weightKg: Float,
        durationSeconds: Long,
        intensity: CyclingIntensity
    ): Int {
        if (durationSeconds <= 0 || weightKg <= 0f) return 0
        val durationHours = durationSeconds / 3600.0
        val calories = intensity.met * weightKg * durationHours
        return calories.toInt()
    }

    /**
     * Format calories with the "estimated" disclaimer label.
     */
    fun formatCalories(kcal: Int): String = "$kcal kcal*"
}
