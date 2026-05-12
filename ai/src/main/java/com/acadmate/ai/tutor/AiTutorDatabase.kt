package com.acadmate.ai.tutor

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ChatMessage::class], version = 1, exportSchema = false)
abstract class AiTutorDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
