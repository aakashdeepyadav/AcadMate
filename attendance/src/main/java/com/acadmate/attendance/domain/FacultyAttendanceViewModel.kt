package com.acadmate.attendance.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.db.UserEntity
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import javax.inject.Inject
import java.time.LocalDateTime
import java.time.ZoneOffset
import com.acadmate.attendance.audio.AcousticTokenGenerator

data class StudentAttendanceRecord(
    val studentId: String = "",
    val studentName: String = "",
    val enrollmentNumber: String = "",
    val profilePictureUrl: String? = null,
    val isPresent: Boolean = false,
    val markedAt: Long? = null,
    val hasAnomaly: Boolean = false,
    val anomalyReason: String? = null
)

data class FacultyAttendanceUiState(
    val isLoading: Boolean = false,
    val students: List<StudentAttendanceRecord> = emptyList(),
    val className: String = "",
    val sessionTime: String = "",
    val isSessionActive: Boolean = false,
    val sessionStartTime: Long? = null,
    val error: String? = null
)

@HiltViewModel
class FacultyAttendanceViewModel @Inject constructor(
    private val acousticGenerator: AcousticTokenGenerator
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(FacultyAttendanceUiState())
    val uiState: StateFlow<FacultyAttendanceUiState> = _uiState.asStateFlow()

    fun startAttendanceSession(classId: String) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(
                isSessionActive = true,
                sessionStartTime = startTime,
                className = classId
            )
            
            // Save active session to Firestore
            val sessionData = hashMapOf(
                "classId" to classId,
                "startTime" to startTime,
                "facultyId" to (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"),
                "status" to "ACTIVE"
            )
            firestore.collection("active_sessions").document(classId).set(sessionData).await()

            // Start playing Acoustic Token (18.5kHz)
            acousticGenerator.playToken("ATT_SESSION_$classId")
            
            // Automatically stop after 10 minutes
            delay(10 * 60 * 1000L)
            if (_uiState.value.isSessionActive) {
                stopAttendanceSession()
            }
        }
    }

    fun stopAttendanceSession() {
        val classId = _uiState.value.className
        _uiState.value = _uiState.value.copy(isSessionActive = false, sessionStartTime = null)
        acousticGenerator.stop()
        
        // Remove from Firestore
        viewModelScope.launch {
            try {
                firestore.collection("active_sessions").document(classId).delete().await()
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        acousticGenerator.stop()
    }

    fun loadSessionAttendance(classId: String, hour: Int? = null, date: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, className = classId)
            try {
                // 1. Fetch all students (Static for now, but could be specific to class)
                val studentsSnapshot = firestore.collection("users")
                    .whereEqualTo("role", "STUDENT")
                    .get()
                    .await()
                
                val students = studentsSnapshot.documents.map { doc ->
                    UserEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unknown",
                        email = doc.getString("email") ?: "",
                        phoneNumber = doc.getString("phoneNumber") ?: "",
                        regNo = doc.getString("regNo") ?: doc.getString("enrollmentNumber") ?: "N/A",
                        department = doc.getString("department"),
                        role = com.acadmate.core.model.UserRole.STUDENT,
                        profilePictureUrl = doc.getString("profilePictureUrl")
                    )
                }

                // 2. Listen to real-time attendance & anomalies
                val now = java.time.LocalDateTime.now()
                val targetHour = hour ?: now.hour
                val targetDate = date ?: "${now.year}-${now.monthValue}-${now.dayOfMonth}"
                
                // Attendance listener
                firestore.collection("attendance")
                    .whereEqualTo("subject", classId) 
                    .whereEqualTo("date", targetDate)
                    .whereEqualTo("hour", targetHour)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        
                        val markedStudentIds = snapshot?.documents?.map { it.getString("studentId") } ?: emptyList()

                        // Also fetch anomalies for this session
                        firestore.collection("active_sessions")
                            .document(classId)
                            .collection("anomalies")
                            .addSnapshotListener { anomalySnapshot, ae ->
                                val anomalyMap = anomalySnapshot?.documents?.associate { 
                                    it.getString("userId") to it.getString("reason") 
                                } ?: emptyMap()

                                val attendanceRecords = students.map { student ->
                                    StudentAttendanceRecord(
                                        studentId = student.id,
                                        studentName = student.name,
                                        enrollmentNumber = student.regNo ?: "N/A",
                                        profilePictureUrl = student.profilePictureUrl,
                                        isPresent = markedStudentIds.contains(student.id),
                                        hasAnomaly = anomalyMap.containsKey(student.id),
                                        anomalyReason = anomalyMap[student.id]
                                    )
                                }

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    students = attendanceRecords,
                                    sessionTime = "Session: $targetDate | Hour: $targetHour:00"
                                )
                            }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleAttendance(studentId: String, currentStatus: Boolean) {
        val classId = _uiState.value.className
        val now = java.time.LocalDateTime.now()
        val currentHour = now.hour
        val currentDate = "${now.year}-${now.monthValue}-${now.dayOfMonth}"
        val facultyId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "Faculty"

        viewModelScope.launch {
            try {
                if (currentStatus) {
                    // Remove record (Unmark)
                    val snapshot = firestore.collection("attendance")
                        .whereEqualTo("studentId", studentId)
                        .whereEqualTo("subject", classId)
                        .whereEqualTo("date", currentDate)
                        .whereEqualTo("hour", currentHour)
                        .get()
                        .await()
                    
                    snapshot.documents.forEach { it.reference.delete().await() }
                    
                    // Also clear any anomalies for this student if manually unmarked
                    try {
                        firestore.collection("active_sessions")
                            .document(classId)
                            .collection("anomalies")
                            .document(studentId)
                            .delete()
                    } catch (e: Exception) {}
                } else {
                    // Add record (Manual Rectification)
                    val recordId = "${studentId}_${classId.replace(" ", "_")}_${currentDate}_$currentHour"
                    val record = hashMapOf(
                        "studentId" to studentId,
                        "subject" to classId,
                        "date" to currentDate,
                        "hour" to currentHour,
                        "timestamp" to System.currentTimeMillis(),
                        "status" to "PRESENT",
                        "facultyId" to facultyId,
                        "isManual" to true
                    )
                    firestore.collection("attendance").document(recordId).set(record).await()

                    // Clear anomaly if teacher manually overrides
                    try {
                        firestore.collection("active_sessions")
                            .document(classId)
                            .collection("anomalies")
                            .document(studentId)
                            .delete()
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update: ${e.message}")
            }
        }
    }
}
