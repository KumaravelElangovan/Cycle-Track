package com.cycletrack.app.data.local.dao

import androidx.room.*
import com.cycletrack.app.data.local.entities.RoutePointEntity
import com.cycletrack.app.data.local.entities.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // ── Workout CRUD ─────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkoutById(id: Long)

    @Query("SELECT * FROM workouts ORDER BY startTime DESC")
    fun getAllWorkoutsFlow(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getWorkoutById(id: Long): WorkoutEntity?

    // ── Route Points ──────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutePoints(points: List<RoutePointEntity>)

    @Query("SELECT * FROM route_points WHERE workoutId = :workoutId ORDER BY timestamp ASC")
    suspend fun getRoutePointsForWorkout(workoutId: Long): List<RoutePointEntity>

    // ── Aggregates (for Stats screen) ─────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(distanceKm), 0.0) FROM workouts")
    fun getTotalDistanceKmFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM workouts")
    fun getTotalDurationSecondsFlow(): Flow<Long>

    @Query("SELECT COALESCE(SUM(caloriesBurned), 0) FROM workouts")
    fun getTotalCaloriesFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM workouts")
    fun getTotalRideCountFlow(): Flow<Int>

    @Query("SELECT COALESCE(MAX(maxSpeedKmh), 0.0) FROM workouts")
    fun getAllTimeMaxSpeedFlow(): Flow<Double>

    /** Distance for today (date in millis — start of day) */
    @Query("SELECT COALESCE(SUM(distanceKm), 0.0) FROM workouts WHERE startTime >= :startOfDayMs")
    suspend fun getDistanceSince(startOfDayMs: Long): Double

    /** All workouts started within a time range */
    @Query("SELECT * FROM workouts WHERE startTime >= :from AND startTime <= :to ORDER BY startTime DESC")
    suspend fun getWorkoutsInRange(from: Long, to: Long): List<WorkoutEntity>
}
