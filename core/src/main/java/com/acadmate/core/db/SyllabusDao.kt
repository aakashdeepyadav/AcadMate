package com.acadmate.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyllabusDao {
    @Query("SELECT * FROM syllabuses")
    fun getAllSyllabuses(): Flow<List<SyllabusEntity>>

    @Query("SELECT * FROM syllabuses WHERE subjectCode = :subjectCode")
    suspend fun getSyllabus(subjectCode: String): SyllabusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyllabuses(syllabuses: List<SyllabusEntity>)

    @Query("DELETE FROM syllabuses")
    suspend fun clearAllSyllabuses()
}
