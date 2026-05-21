package com.acadmate.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments ORDER BY dueDate ASC")
    fun getAllAssignments(): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE facultyId = :facultyId ORDER BY dueDate ASC")
    fun getAssignmentsForFaculty(facultyId: String): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignments(assignments: List<AssignmentEntity>)

    @Query("DELETE FROM assignments")
    suspend fun clearAllAssignments()
}
