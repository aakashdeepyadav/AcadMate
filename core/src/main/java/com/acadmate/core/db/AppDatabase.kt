package com.acadmate.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserEntity::class,
        AttendanceEntity::class,
        TimetableEntity::class,
        SyllabusGapEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun timetableDao(): TimetableDao
    abstract fun syllabusGapDao(): SyllabusGapDao
}
