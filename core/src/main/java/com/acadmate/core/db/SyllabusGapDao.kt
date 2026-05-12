package com.acadmate.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyllabusGapDao {
    @Query("SELECT * FROM syllabus_gaps WHERE subjectId = :subjectId")
    fun getGapsForSubject(subjectId: String): Flow<List<SyllabusGapEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGaps(gaps: List<SyllabusGapEntity>)

    @Query("DELETE FROM syllabus_gaps WHERE subjectId = :subjectId")
    suspend fun deleteGapsForSubject(subjectId: String)

    @Query("DELETE FROM syllabus_gaps")
    suspend fun clearAllGaps()
}
