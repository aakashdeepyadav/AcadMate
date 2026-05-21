package com.acadmate.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoticeDao {
    @Query("SELECT * FROM notices ORDER BY timestamp DESC")
    fun getAllNotices(): Flow<List<NoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<NoticeEntity>)

    @Query("DELETE FROM notices")
    suspend fun clearAllNotices()
}
