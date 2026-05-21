package com.acadmate.attendance.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.db.UserEntity
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    val assignedCourses: List<String> = emptyList(),
    val error: String? = null,
    val isQrMode: Boolean = false,
    val currentQrToken: String? = null
)

@HiltViewModel
class FacultyAttendanceViewModel @Inject constructor(
    private val acousticGenerator: AcousticTokenGenerator,
    private val timetableRepository: com.acadmate.core.db.TimetableRepository,
    private val userRepository: com.acadmate.core.db.UserRepository
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(FacultyAttendanceUiState())
    val uiState: StateFlow<FacultyAttendanceUiState> = _uiState.asStateFlow()
    
    private var qrJob: kotlinx.coroutines.Job? = null

    fun toggleQrMode() {
        val newMode = !_uiState.value.isQrMode
        _uiState.value = _uiState.value.copy(isQrMode = newMode)
        if (newMode && _uiState.value.isSessionActive) {
            startQrRotation()
        } else {
            qrJob?.cancel()
        }
    }

    private fun startQrRotation() {
        qrJob?.cancel()
        qrJob = viewModelScope.launch {
            val classId = _uiState.value.className
            while (isActive && _uiState.value.isSessionActive && _uiState.value.isQrMode) {
                val newToken = "QR_${classId}_${System.currentTimeMillis() / 10000}" // Updates every 10s
                _uiState.value = _uiState.value.copy(currentQrToken = newToken)
                
                // Update in Firestore for student verification
                firestore.collection("active_sessions").document(classId)
                    .update("dynamicQrToken", newToken)
                
                delay(10000)
            }
        }
    }

    fun startAttendanceSession(classId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // ... verification logic ...
            val calendar = java.util.Calendar.getInstance()
            val dayOfWeek = when(calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.MONDAY -> 1
                java.util.Calendar.TUESDAY -> 2
                java.util.Calendar.WEDNESDAY -> 3
                java.util.Calendar.THURSDAY -> 4
                java.util.Calendar.FRIDAY -> 5
                java.util.Calendar.SATURDAY -> 6
                java.util.Calendar.SUNDAY -> 7
                else -> 1
            }

            val todaySchedule = timetableRepository.getTimetableForDaySync(dayOfWeek)
            val nowTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE))
            
            // More robust name retrieval
            val repoUser = userRepository.getCurrentUser().first()
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val firestoreUser = if (repoUser == null && currentUid.isNotBlank()) {
                userRepository.getUserFromFirestore(currentUid).getOrNull()
            } else null
            
            val currentFacultyName = repoUser?.name ?: firestoreUser?.name ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName ?: ""
            
            val scheduledClass = todaySchedule.find { item ->
                // Flexible matching for ID or Subject names
                val idMatch = item.id == classId || 
                             item.subject.contains(classId, ignoreCase = true) || 
                             classId.contains(item.subject, ignoreCase = true)
                
                val nameMatch = currentFacultyName.isNotBlank() && (
                    item.faculty.contains(currentFacultyName, ignoreCase = true) || 
                    currentFacultyName.contains(item.faculty, ignoreCase = true)
                )
                
                idMatch && (nameMatch || item.faculty.isBlank() || item.faculty == "TBA" || item.faculty == "Not Assigned")
            }

            val isWithinPeriod = scheduledClass?.let { 
                nowTime >= it.startTime && nowTime <= it.endTime 
            } ?: false

            if (scheduledClass == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Access Denied: You are not assigned to $classId today."
                )
                return@launch
            }

            if (!isWithinPeriod && classId != "General") {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Timing Error: This class is scheduled for ${scheduledClass.startTime} - ${scheduledClass.endTime}. It is currently $nowTime."
                )
                return@launch
            }

            val startTime = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isSessionActive = true,
                sessionStartTime = startTime,
                className = classId,
                error = null
            )
            
            // Save active session to Firestore
            val sessionData = hashMapOf(
                "classId" to classId,
                "startTime" to startTime,
                "facultyId" to currentUid,
                "facultyName" to currentFacultyName,
                "status" to "ACTIVE"
            )
            firestore.collection("active_sessions").document(classId).set(sessionData).await()

            // Start playing Acoustic Token (18.5kHz)
            acousticGenerator.playToken("ATT_SESSION_$classId")
            
            if (_uiState.value.isQrMode) {
                startQrRotation()
            }

            // Automatically stop after 10 minutes
            delay(10 * 60 * 1000L)
            if (_uiState.value.isSessionActive) {
                stopAttendanceSession()
            }
        }
    }

    fun stopAttendanceSession() {
        val classId = _uiState.value.className
        qrJob?.cancel()
        _uiState.value = _uiState.value.copy(isSessionActive = false, sessionStartTime = null, currentQrToken = null)
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
                // Fetch assigned courses for this faculty
                val currentUser = userRepository.getCurrentUser().first()
                val facultyName = currentUser?.name ?: ""
                
                if (facultyName.isNotBlank()) {
                    val coursesSnapshot = firestore.collection("courses")
                        .whereEqualTo("assignedFaculty", facultyName)
                        .get()
                        .await()
                    val courseNames = coursesSnapshot.documents.map { it.getString("name") ?: "" }.filter { it.isNotBlank() }
                    _uiState.value = _uiState.value.copy(assignedCourses = courseNames)
                }

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
                        
                                val markedStudentIds = snapshot?.documents?.map { it.getString("studentId") ?: "" } ?: emptyList()

                        // Also fetch anomalies for this session
                        firestore.collection("active_sessions")
                            .document(classId)
                            .collection("anomalies")
                            .addSnapshotListener { anomalySnapshot, ae ->
                                val anomalyMap = anomalySnapshot?.documents?.associate { 
                                    it.getString("userId") to it.getString("reason") 
                                } ?: emptyMap()

                                val attendanceRecords = students.map { student ->
                                    val currentRegNo = student.regNo ?: student.id
                                    StudentAttendanceRecord(
                                        studentId = currentRegNo,
                                        studentName = student.name,
                                        enrollmentNumber = currentRegNo,
                                        profilePictureUrl = student.profilePictureUrl,
                                        isPresent = markedStudentIds.contains(currentRegNo),
                                        hasAnomaly = anomalyMap.containsKey(currentRegNo),
                                        anomalyReason = anomalyMap[currentRegNo]
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
