package com.cycletrack.app.data.repository

import com.cycletrack.app.data.local.dao.WorkoutDao
import com.cycletrack.app.data.local.entities.RoutePointEntity
import com.cycletrack.app.data.local.entities.WorkoutEntity
import com.cycletrack.app.domain.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface WorkoutRepository {
    fun getAllWorkoutsFlow(): Flow<List<Workout>>
    suspend fun saveWorkout(workout: Workout): Long
    suspend fun getWorkoutById(id: Long): Workout?
    suspend fun deleteWorkout(workout: Workout)
    suspend fun deleteWorkoutById(id: Long)
    fun getTotalDistanceKmFlow(): Flow<Double>
    fun getTotalDurationSecondsFlow(): Flow<Long>
    fun getTotalCaloriesFlow(): Flow<Int>
    fun getTotalRideCountFlow(): Flow<Int>
    fun getAllTimeMaxSpeedFlow(): Flow<Double>
    suspend fun getDistanceSince(startOfDayMs: Long): Double
    suspend fun getWorkoutsInRange(from: Long, to: Long): List<Workout>
}

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val dao: WorkoutDao
) : WorkoutRepository {

    override fun getAllWorkoutsFlow(): Flow<List<Workout>> {
        return dao.getAllWorkoutsFlow().map { entities ->
            entities.map { entity ->
                val routePoints = dao.getRoutePointsForWorkout(entity.id)
                    .map { it.toDomain() }
                entity.toDomain(routePoints)
            }
        }
    }

    override suspend fun saveWorkout(workout: Workout): Long {
        val entity = WorkoutEntity.fromDomain(workout)
        val workoutId = dao.insertWorkout(entity)

        // Save route points in batch
        if (workout.routePoints.isNotEmpty()) {
            val routeEntities = workout.routePoints.map { point ->
                RoutePointEntity.fromDomain(point, workoutId)
            }
            dao.insertRoutePoints(routeEntities)
        }
        return workoutId
    }

    override suspend fun getWorkoutById(id: Long): Workout? {
        val entity = dao.getWorkoutById(id) ?: return null
        val routePoints = dao.getRoutePointsForWorkout(id).map { it.toDomain() }
        return entity.toDomain(routePoints)
    }

    override suspend fun deleteWorkout(workout: Workout) {
        dao.deleteWorkout(WorkoutEntity.fromDomain(workout))
    }

    override suspend fun deleteWorkoutById(id: Long) {
        dao.deleteWorkoutById(id)
    }

    override fun getTotalDistanceKmFlow(): Flow<Double> = dao.getTotalDistanceKmFlow()
    override fun getTotalDurationSecondsFlow(): Flow<Long> = dao.getTotalDurationSecondsFlow()
    override fun getTotalCaloriesFlow(): Flow<Int> = dao.getTotalCaloriesFlow()
    override fun getTotalRideCountFlow(): Flow<Int> = dao.getTotalRideCountFlow()
    override fun getAllTimeMaxSpeedFlow(): Flow<Double> = dao.getAllTimeMaxSpeedFlow()
    override suspend fun getDistanceSince(startOfDayMs: Long): Double = dao.getDistanceSince(startOfDayMs)
    override suspend fun getWorkoutsInRange(from: Long, to: Long): List<Workout> {
        return dao.getWorkoutsInRange(from, to).map { entity ->
            val routePoints = dao.getRoutePointsForWorkout(entity.id).map { it.toDomain() }
            entity.toDomain(routePoints)
        }
    }
}
