package com.acadmate.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserEntity::class,
        AttendanceEntity::class,
        TimetableEntity::class,
        SyllabusGapEntity::class,
        AssignmentEntity::class,
        NoticeEntity::class,
        SyllabusEntity::class,
        SemesterResultEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun timetableDao(): TimetableDao
    abstract fun syllabusGapDao(): SyllabusGapDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun noticeDao(): NoticeDao
    abstract fun syllabusDao(): SyllabusDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun resultDao(): ResultDao
}
