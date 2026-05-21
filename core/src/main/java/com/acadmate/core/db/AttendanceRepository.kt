package com.acadmate.core.db

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val attendanceDao: AttendanceDao
) {
    private val firestore = FirebaseFirestore.getInstance()

    fun getAttendanceForUser(userId: String): Flow<List<AttendanceEntity>> {
        return attendanceDao.getAttendanceForUser(userId)
    }

    suspend fun syncAttendance(studentId: String) {
        try {
            val snapshot = firestore.collection("attendance")
                .whereEqualTo("studentId", studentId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val remoteRecords = snapshot.documents.mapNotNull { doc ->
                AttendanceEntity(
                    id = doc.id,
                    userId = studentId,
                    subject = doc.getString("subject") ?: "",
                    faculty = doc.getString("faculty") ?: "Unknown",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    status = doc.getString("status") ?: "PRESENT",
                    syncStatus = 1
                )
            }

            if (remoteRecords.isNotEmpty()) {
                attendanceDao.insertAttendance(remoteRecords)
            }
        } catch (e: Exception) {
            // Offline
        }
    }

    suspend fun saveAttendanceLocally(record: AttendanceEntity) {
        attendanceDao.insertAttendance(listOf(record))
    }

    suspend fun markRecordSynced(id: String) {
        attendanceDao.markSynced(id)
    }

    suspend fun getUnsyncedRecords(): List<AttendanceEntity> {
        return attendanceDao.getUnsyncedRecords()
    }
}
