package com.acadmate.core.db

import com.acadmate.core.model.Assignment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentRepository @Inject constructor(
    private val assignmentDao: AssignmentDao
) {
    private val firestore = FirebaseFirestore.getInstance()

    fun getAllAssignments(): Flow<List<Assignment>> {
        return assignmentDao.getAllAssignments().map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun getAssignmentsForFaculty(facultyId: String): Flow<List<Assignment>> {
        return assignmentDao.getAssignmentsForFaculty(facultyId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun syncAssignments(isFaculty: Boolean, userId: String) {
        try {
            val query = if (isFaculty) {
                firestore.collection("assignments").whereEqualTo("facultyId", userId)
            } else {
                firestore.collection("assignments")
            }

            val result = query.get().await()
            val remoteItems = result.toObjects(Assignment::class.java)
            
            val entities = remoteItems.map { it.toEntity() }
            if (entities.isNotEmpty()) {
                if (!isFaculty) {
                    assignmentDao.clearAllAssignments()
                }
                assignmentDao.insertAssignments(entities)
            }
        } catch (e: Exception) {
            // Offline or error
        }
    }

    private fun Assignment.toEntity() = AssignmentEntity(
        id = id,
        title = title,
        description = description,
        subjectId = subjectId,
        facultyId = facultyId,
        dueDate = dueDate,
        fileUrl = fileUrl,
        createdAt = createdAt
    )

    private fun AssignmentEntity.toModel() = Assignment(
        id = id,
        title = title,
        description = description,
        subjectId = subjectId,
        facultyId = facultyId,
        dueDate = dueDate,
        fileUrl = fileUrl,
        createdAt = createdAt
    )
}
