package com.cycletrack.app.di

import android.content.Context
import androidx.room.Room
import com.cycletrack.app.data.local.CycleTrackDatabase
import com.cycletrack.app.data.local.dao.WorkoutDao
import com.cycletrack.app.data.repository.WorkoutRepository
import com.cycletrack.app.data.repository.WorkoutRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CycleTrackDatabase {
        return Room.databaseBuilder(
            context,
            CycleTrackDatabase::class.java,
            "cycletrack_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkoutDao(database: CycleTrackDatabase): WorkoutDao {
        return database.workoutDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(
        impl: WorkoutRepositoryImpl
    ): WorkoutRepository
}
