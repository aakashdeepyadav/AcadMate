package com.acadmate.core.di

import android.content.Context
import androidx.room.Room
import com.acadmate.core.db.AppDatabase
import com.acadmate.core.db.UserDao
import com.acadmate.core.db.UserRepository
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "acadmate_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideTimetableDao(database: AppDatabase): com.acadmate.core.db.TimetableDao {
        return database.timetableDao()
    }

    @Provides
    @Singleton
    fun provideSyllabusGapDao(database: AppDatabase): com.acadmate.core.db.SyllabusGapDao {
        return database.syllabusGapDao()
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        timetableDao: com.acadmate.core.db.TimetableDao,
        syllabusGapDao: com.acadmate.core.db.SyllabusGapDao
    ): UserRepository {
        return UserRepository(userDao, timetableDao, syllabusGapDao)
    }

    @Provides
    @Singleton
    fun provideTimetableRepository(
        timetableDao: com.acadmate.core.db.TimetableDao
    ): com.acadmate.core.db.TimetableRepository {
        return com.acadmate.core.db.TimetableRepository(timetableDao)
    }

    @Provides
    @Singleton
    fun provideSyllabusRepository(
        syllabusGapDao: com.acadmate.core.db.SyllabusGapDao
    ): com.acadmate.core.db.SyllabusRepository {
        return com.acadmate.core.db.SyllabusRepository(syllabusGapDao)
    }
}
