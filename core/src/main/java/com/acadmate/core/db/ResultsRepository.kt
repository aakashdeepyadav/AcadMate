package com.acadmate.core.db

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResultsRepository @Inject constructor(
    private val resultDao: ResultDao
) {
    private val firestore = FirebaseFirestore.getInstance()

    fun getResultsForUser(userId: String): Flow<List<SemesterResultEntity>> {
        return resultDao.getResultsForUser(userId)
    }

    suspend fun syncResults(userId: String) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("results")
                .orderBy("semesterName")
                .get()
                .await()

            val remoteItems = snapshot.documents.mapNotNull { doc ->
                val subjectsMap = doc.get("subjects") as? List<Map<String, Any>> ?: emptyList()
                val subjects = subjectsMap.map { subMap ->
                    SubjectGradeEntity(
                        subjectName = subMap["subjectName"] as? String ?: "",
                        code = subMap["code"] as? String ?: "",
                        grade = subMap["grade"] as? String ?: "",
                        credits = (subMap["credits"] as? Long)?.toInt() ?: 0
                    )
                }

                SemesterResultEntity(
                    id = doc.id,
                    userId = userId,
                    semesterName = doc.getString("semesterName") ?: "",
                    sgpa = (doc.getDouble("sgpa"))?.toFloat() ?: 0f,
                    credits = (doc.getLong("credits"))?.toInt() ?: 0,
                    subjects = subjects
                )
            }

            if (remoteItems.isNotEmpty()) {
                // For a clean cache, we might want to clear old results for this user
                // but Room clearAll usually deletes everything. 
                // Since this is a single user app for now, it's fine.
                resultDao.insertResults(remoteItems)
            }
        } catch (e: Exception) {
            // Offline
        }
    }

    suspend fun insertInitialSemesterResults(userId: String) {
        val initialResults = listOf(
            SemesterResultEntity(
                id = "4",
                userId = userId,
                semesterName = "Semester 4",
                sgpa = 8.6f,
                credits = 24,
                subjects = listOf(
                    SubjectGradeEntity("Operating Systems", "CS401", "A", 4),
                    SubjectGradeEntity("Computer Networks", "CS402", "A+", 4),
                    SubjectGradeEntity("Software Engineering", "CS403", "B+", 4)
                )
            ),
            SemesterResultEntity(
                id = "5",
                userId = userId,
                semesterName = "Semester 5",
                sgpa = 8.9f,
                credits = 24,
                subjects = listOf(
                    SubjectGradeEntity("Database Management Systems", "CS501", "A+", 4),
                    SubjectGradeEntity("Theory of Computation", "CS502", "A", 4),
                    SubjectGradeEntity("Artificial Intelligence", "CS503", "A+", 4)
                )
            )
        )

        for (result in initialResults) {
            firestore.collection("users")
                .document(userId)
                .collection("results")
                .document(result.id)
                .set(result)
                .await()
        }
        syncResults(userId)
    }
}
