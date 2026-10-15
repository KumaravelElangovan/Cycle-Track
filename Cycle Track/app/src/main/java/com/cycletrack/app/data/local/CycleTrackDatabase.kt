package com.cycletrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cycletrack.app.data.local.dao.WorkoutDao
import com.cycletrack.app.data.local.entities.RoutePointEntity
import com.cycletrack.app.data.local.entities.WorkoutEntity

@Database(
    entities = [WorkoutEntity::class, RoutePointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CycleTrackDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
