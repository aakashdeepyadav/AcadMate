package com.acadmate.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultDao {
    @Query("SELECT * FROM semester_results WHERE userId = :userId ORDER BY id ASC")
    fun getResultsForUser(userId: String): Flow<List<SemesterResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<SemesterResultEntity>)

    @Query("DELETE FROM semester_results")
    suspend fun clearAll()
}
