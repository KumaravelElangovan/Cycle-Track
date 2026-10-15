package com.cycletrack.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cycletrack.app.domain.model.RoutePoint

@Entity(
    tableName = "route_points",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId")]
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val altitudeM: Double = 0.0,
    val accuracyM: Float = 0f
) {
    fun toDomain(): RoutePoint = RoutePoint(
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        altitudeM = altitudeM,
        accuracyM = accuracyM
    )

    companion object {
        fun fromDomain(point: RoutePoint, workoutId: Long): RoutePointEntity {
            return RoutePointEntity(
                workoutId = workoutId,
                latitude = point.latitude,
                longitude = point.longitude,
                timestamp = point.timestamp,
                altitudeM = point.altitudeM,
                accuracyM = point.accuracyM
            )
        }
    }
}
