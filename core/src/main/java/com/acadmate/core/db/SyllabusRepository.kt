package com.acadmate.core.db

import com.acadmate.core.model.SubjectSyllabus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyllabusRepository @Inject constructor(
    private val syllabusGapDao: SyllabusGapDao,
    private val syllabusDao: SyllabusDao
) {
    private val firestore = FirebaseFirestore.getInstance()

    fun getAllSyllabuses(): Flow<List<SubjectSyllabus>> {
        return syllabusDao.getAllSyllabuses().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun syncSyllabuses() {
        try {
            val snapshot = firestore.collection("syllabuses").get().await()
            val remoteItems = snapshot.toObjects(SubjectSyllabus::class.java)
            
            if (remoteItems.isNotEmpty()) {
                val entities = remoteItems.map { it.toEntity() }
                syllabusDao.clearAllSyllabuses()
                syllabusDao.insertSyllabuses(entities)
            }
        } catch (e: Exception) {
            // Offline
        }
    }

    fun getGapsForSubject(subjectId: String): Flow<List<SyllabusGapEntity>> {
        return syllabusGapDao.getGapsForSubject(subjectId)
    }

    suspend fun syncSyllabusGaps(userId: String, subjectId: String) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("subjects")
                .document(subjectId)
                .collection("gaps")
                .get()
                .await()

            val remoteGaps = snapshot.documents.mapNotNull { doc ->
                SyllabusGapEntity(
                    topic = doc.id,
                    coverage = doc.getDouble("coverage")?.toFloat() ?: 0f,
                    status = doc.getString("status") ?: "Missing",
                    subjectId = subjectId
                )
            }

            if (remoteGaps.isNotEmpty()) {
                syllabusGapDao.deleteGapsForSubject(subjectId)
                syllabusGapDao.insertGaps(remoteGaps)
            }
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    suspend fun saveGapAnalysis(userId: String, subjectId: String, gaps: List<SyllabusGapEntity>) {
        // Save locally
        syllabusGapDao.insertGaps(gaps)

        // Sync to cloud
        try {
            val batch = firestore.batch()
            val collectionRef = firestore.collection("users")
                .document(userId)
                .collection("subjects")
                .document(subjectId)
                .collection("gaps")

            gaps.forEach { gap ->
                val docRef = collectionRef.document(gap.topic)
                batch.set(docRef, mapOf(
                    "coverage" to gap.coverage,
                    "status" to gap.status
                ))
            }
            batch.commit().await()
        } catch (e: Exception) {
            // Handle sync failure
        }
    }

    private fun SubjectSyllabus.toEntity() = SyllabusEntity(
        subjectCode = subjectCode,
        subjectName = subjectName,
        description = description,
        credits = credits,
        ltp = ltp,
        units = units
    )

    private fun SyllabusEntity.toModel() = SubjectSyllabus(
        subjectCode = subjectCode,
        subjectName = subjectName,
        description = description,
        credits = credits,
        ltp = ltp,
        units = units
    )
}
