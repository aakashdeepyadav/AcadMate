package com.acadmate.attendance.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.attendance.audio.AcousticTokenReceiver
import com.acadmate.attendance.ble.BleScanner
import com.acadmate.attendance.data.AttendanceRecord
import com.acadmate.attendance.data.AttendanceSession
import com.acadmate.attendance.data.AttendanceStatus
import com.acadmate.attendance.data.AttendanceUiState
import com.acadmate.attendance.data.GeofenceResult
import com.acadmate.attendance.data.LivenessResult
import com.acadmate.attendance.data.MonthlySummary
import com.acadmate.attendance.face.FaceLivenessDetector
import com.acadmate.attendance.geo.GeofenceValidator
import com.acadmate.core.datastore.OnboardingDataStore
import com.acadmate.core.model.UserRole
import com.acadmate.core.db.UserRepository
import com.acadmate.core.db.AttendanceRepository
import com.acadmate.core.db.TimetableRepository
import com.acadmate.core.db.AttendanceEntity
import com.acadmate.core.util.DeviceIdManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val bleScanner: BleScanner,
    val faceDetector: FaceLivenessDetector,
    private val geofenceValidator: GeofenceValidator,
    private val onboardingDataStore: OnboardingDataStore,
    private val acousticTokenReceiver: AcousticTokenReceiver,
    private val deviceIdManager: DeviceIdManager,
    private val userRepository: UserRepository,
    private val attendanceRepository: AttendanceRepository,
    private val timetableRepository: TimetableRepository
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole: StateFlow<UserRole?> = _userRole.asStateFlow()

    private val _attendanceHistory = MutableStateFlow<List<AttendanceSession>>(emptyList())
    val attendanceHistory: StateFlow<List<AttendanceSession>> = _attendanceHistory.asStateFlow()

    private val _monthlySummaries = MutableStateFlow<List<MonthlySummary>>(emptyList())
    val monthlySummaries: StateFlow<List<MonthlySummary>> = _monthlySummaries.asStateFlow()

    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Idle)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val _acousticStatus = MutableStateFlow("Listening...")
    val acousticStatus: StateFlow<String> = _acousticStatus.asStateFlow()

    private val _userProfilePhoto = MutableStateFlow<String?>(null)
    val userProfilePhoto: StateFlow<String?> = _userProfilePhoto.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                _userRole.value = user?.role
                if (user != null && user.role == UserRole.STUDENT) {
                    val studentId = user.regNo ?: user.id
                    syncData(studentId)
                    observeAttendance(studentId)
                }
            }
        }

        viewModelScope.launch {
            onboardingDataStore.selectedRole.collect { role ->
                if (_userRole.value == null) _userRole.value = role
            }
        }
        
        // Ensure timetable is synced for timing validation
        viewModelScope.launch {
            try {
                timetableRepository.syncGlobalTimetable()
            } catch (e: Exception) {}
        }
    }

    private fun syncData(studentId: String) {
        viewModelScope.launch {
            attendanceRepository.syncAttendance(studentId)
        }
    }

    private fun observeAttendance(studentId: String) {
        viewModelScope.launch {
            attendanceRepository.getAttendanceForUser(studentId).collect { entities ->
                val sessions = entities.map { it.toSession() }
                _attendanceHistory.value = sessions
                updateMonthlySummaries(sessions)
            }
        }
    }

    private fun updateMonthlySummaries(history: List<AttendanceSession>) {
        val summaries = history.groupBy { YearMonth.of(it.date.year, it.date.monthValue) }
            .map { (yearMonth, sessions) ->
                val attended = sessions.count { it.attended }
                MonthlySummary(
                    month = yearMonth.monthValue,
                    year = yearMonth.year,
                    totalClasses = sessions.size,
                    classesAttended = attended,
                    attendancePercentage = (attended.toFloat() / sessions.size.coerceAtLeast(1)) * 100
                )
            }.sortedWith(compareBy<MonthlySummary> { it.year }.thenBy { it.month }.reversed())
        _monthlySummaries.value = summaries
    }

    private fun AttendanceEntity.toSession() = AttendanceSession(
        id = id,
        subject = subject,
        date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()),
        startTime = "--", // Could be stored in entity if needed
        endTime = "--",
        faculty = faculty,
        attended = status == "PRESENT",
        markedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    )

    fun startAttendanceFlow(subject: String, faculty: String, context: android.content.Context) {
        _uiState.value = AttendanceUiState.Loading
        viewModelScope.launch {
            try {
                val currentDeviceId = deviceIdManager.getDeviceId()
                val userId = auth.currentUser?.uid ?: return@launch
                
                val userDoc = firestore.collection("users").document(userId).get().await()
                
                if (!userDoc.exists()) {
                    _uiState.value = AttendanceUiState.Failed("Profile Incomplete")
                    return@launch
                } else {
                    val profilePhoto = userDoc.getString("profilePictureUrl")
                    if (profilePhoto.isNullOrBlank()) {
                        _uiState.value = AttendanceUiState.Failed("Profile Photo Required. Please set it in Profile settings first.")
                        return@launch
                    }
                    _userProfilePhoto.value = profilePhoto
                    
                    val boundDeviceId = userDoc.getString("boundDeviceId")
                    if (boundDeviceId != null && boundDeviceId != currentDeviceId) {
                        _uiState.value = AttendanceUiState.Failed("Security Violation: This account is bound to another device. Contact Admin to reset.")
                        return@launch
                    }
                }

                _uiState.value = AttendanceUiState.VerifyingAcoustic
                val tokenDetected = acousticTokenReceiver.listenForToken(timeoutMs = 15000)
                if (!tokenDetected) {
                    // Try QR as fallback
                    _uiState.value = AttendanceUiState.ScanningQr
                    return@launch
                }

                proceedToBiometrics(subject, faculty, context)
            } catch (e: Exception) {
                _uiState.value = AttendanceUiState.Failed(e.message ?: "Unknown Error")
            }
        }
    }

    fun verifyQrToken(qrText: String, subject: String, faculty: String, context: android.content.Context) {
        viewModelScope.launch {
            try {
                // Find classId from subject
                val sessionDoc = firestore.collection("active_sessions")
                    .whereEqualTo("classId", subject)
                    .get()
                    .await()
                
                val activeSession = sessionDoc.documents.firstOrNull()
                val serverToken = activeSession?.getString("dynamicQrToken")
                val realFaculty = activeSession?.getString("facultyName") ?: faculty

                if (serverToken != null && qrText == serverToken) {
                    proceedToBiometrics(subject, realFaculty, context)
                } else {
                    _uiState.value = AttendanceUiState.Failed("Invalid or Expired QR Code. Tokens rotate every 10 seconds.")
                }
            } catch (e: Exception) {
                _uiState.value = AttendanceUiState.Failed("QR Verification Failed: ${e.message}")
            }
        }
    }

    private suspend fun proceedToBiometrics(subject: String, faculty: String, context: android.content.Context) {
        _uiState.value = AttendanceUiState.VerifyingIdentity
        val faceResult = withTimeoutOrNull(30000L) {
            faceDetector.livenessResultFlow.filter { it is LivenessResult.Passed }.first()
        }
        if (faceResult !is LivenessResult.Passed) {
            _uiState.value = AttendanceUiState.Failed("Face Verification Failed")
            return
        }

        _uiState.value = AttendanceUiState.VerifyingLocation
        val geoResult = geofenceValidator.validateLocation()
        if (geoResult !is GeofenceResult.InsideCampus) {
            _uiState.value = AttendanceUiState.Failed("Outside Campus")
            return
        }

        markAttendanceSuccess(subject, faculty, context)
    }

    private suspend fun markAttendanceSuccess(subject: String, faculty: String, context: android.content.Context) {
        // Double check if session is still active and within period
        val calendar = java.util.Calendar.getInstance()
        val nowTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE))
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
        
        // Flexible matching: check if subject name matches or is contained within the timetable entry
        val currentPeriod = todaySchedule.find { item ->
            val match = item.subject.contains(subject, ignoreCase = true) || 
                        subject.contains(item.subject, ignoreCase = true)
            match && nowTime >= item.startTime && nowTime <= item.endTime 
        }

        if (currentPeriod == null && subject != "General") {
            _uiState.value = AttendanceUiState.Failed("Timing Validation Failed: This session ($subject) is not scheduled for $nowTime in your timetable.")
            return
        }

        val now = LocalDateTime.now()
        val timestamp = System.currentTimeMillis()
        val userId = auth.currentUser?.uid ?: ""
        val user = userRepository.getCurrentUser().first()
        val regNo = user?.regNo ?: userId

        val record = AttendanceEntity(
            id = "${regNo}_${timestamp}",
            userId = regNo,
            subject = currentPeriod?.subject ?: subject,
            faculty = faculty,
            timestamp = timestamp,
            status = "PRESENT",
            syncStatus = 0
        )

        attendanceRepository.saveAttendanceLocally(record)
        _uiState.value = AttendanceUiState.Verified(record.id, subject, faculty, now)

        try {
            val dateStr = "${now.year}-${now.monthValue}-${now.dayOfMonth}"
            firestore.collection("attendance").document(record.id).set(hashMapOf(
                "studentId" to regNo,
                "subject" to subject,
                "faculty" to faculty,
                "timestamp" to timestamp,
                "status" to "PRESENT",
                "date" to dateStr,
                "hour" to now.hour
            )).await()
            attendanceRepository.markRecordSynced(record.id)
        } catch (e: Exception) {}
    }

    fun reset() {
        _uiState.value = AttendanceUiState.Idle
        faceDetector.reset()
    }
}
