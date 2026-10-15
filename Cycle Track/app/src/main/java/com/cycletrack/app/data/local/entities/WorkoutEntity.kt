package com.cycletrack.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cycletrack.app.domain.model.CyclingIntensity
import com.cycletrack.app.domain.model.Workout

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceKm: Double,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val caloriesBurned: Int,
    val intensity: String  // stored as enum name()
) {
    fun toDomain(routePoints: List<com.cycletrack.app.domain.model.RoutePoint> = emptyList()): Workout {
        return Workout(
            id = id,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            distanceKm = distanceKm,
            avgSpeedKmh = avgSpeedKmh,
            maxSpeedKmh = maxSpeedKmh,
            caloriesBurned = caloriesBurned,
            intensity = CyclingIntensity.valueOf(intensity),
            routePoints = routePoints
        )
    }

    companion object {
        fun fromDomain(workout: Workout): WorkoutEntity {
            return WorkoutEntity(
                id = workout.id,
                startTime = workout.startTime,
                endTime = workout.endTime,
                durationSeconds = workout.durationSeconds,
                distanceKm = workout.distanceKm,
                avgSpeedKmh = workout.avgSpeedKmh,
                maxSpeedKmh = workout.maxSpeedKmh,
                caloriesBurned = workout.caloriesBurned,
                intensity = workout.intensity.name
            )
        }
    }
}
