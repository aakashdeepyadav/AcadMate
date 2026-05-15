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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.YearMonth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.acadmate.core.util.DeviceIdManager
import com.acadmate.core.db.UserRepository

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val bleScanner: BleScanner,
    val faceDetector: FaceLivenessDetector,
    private val geofenceValidator: GeofenceValidator,
    private val onboardingDataStore: OnboardingDataStore,
    private val acousticTokenReceiver: AcousticTokenReceiver,
    private val deviceIdManager: DeviceIdManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole: StateFlow<UserRole?> = _userRole.asStateFlow()

    init {
        viewModelScope.launch {
            // Priority 1: Check UserRepository (local db synced with remote)
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    _userRole.value = user.role
                    if (user.role == UserRole.STUDENT) {
                        loadAttendanceHistory(user.regNo ?: user.id)
                    }
                }
            }
        }

        // Priority 2: Fallback to onboarding choice if local is empty
        viewModelScope.launch {
            onboardingDataStore.selectedRole.collect { role ->
                if (_userRole.value == null) {
                    _userRole.value = role
                }
            }
        }
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Idle)
    val uiState: StateFlow<AttendanceUiState> = _uiState

    private val _acousticStatus = MutableStateFlow("Listening...")
    val acousticStatus: StateFlow<String> = _acousticStatus.asStateFlow()

    private val _userProfilePhoto = MutableStateFlow<String?>(null)
    val userProfilePhoto: StateFlow<String?> = _userProfilePhoto.asStateFlow()

    private val _attendanceHistory = MutableStateFlow<List<AttendanceSession>>(emptyList())
    val attendanceHistory: StateFlow<List<AttendanceSession>> = _attendanceHistory

    private val _monthlySummaries = MutableStateFlow<List<MonthlySummary>>(emptyList())
    val monthlySummaries: StateFlow<List<MonthlySummary>> = _monthlySummaries

    // Track verification steps
    private var acousticVerified = false
    private var faceVerified = false
    private var geoVerified = false
    private var isClassInProgress = false

    fun startAttendanceFlow(subject: String, faculty: String, context: android.content.Context) {
        _uiState.value = AttendanceUiState.Loading
        viewModelScope.launch {
            try {
                // Step 0: Device Binding Security (Anti-Phone-Passing)
                val currentDeviceId = deviceIdManager.getDeviceId()
                val userId = auth.currentUser?.uid
                
                if (userId == null) {
                    _uiState.value = AttendanceUiState.Failed("User not authenticated. Please log in again.")
                    return@launch
                }
                
                val userDoc = firestore.collection("users").document(userId).get().await()
                
                if (!userDoc.exists()) {
                    // Document missing, attempt to create a basic one or at least bind device
                    val basicUser = hashMapOf(
                        "id" to userId,
                        "name" to (auth.currentUser?.displayName ?: "User"),
                        "email" to (auth.currentUser?.email ?: ""),
                        "boundDeviceId" to currentDeviceId,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    firestore.collection("users").document(userId).set(basicUser, com.google.firebase.firestore.SetOptions.merge()).await()
                    _uiState.value = AttendanceUiState.Failed("Profile Incomplete: Please set up your profile photo for Face ID verification.")
                    return@launch
                } else {
                    val profilePhoto = userDoc.getString("profilePictureUrl")
                    _userProfilePhoto.value = profilePhoto
                    if (profilePhoto.isNullOrBlank()) {
                        _uiState.value = AttendanceUiState.Failed("Profile Photo Required: Please upload your photo in profile settings for Face ID matching.")
                        return@launch
                    }

                    val boundDeviceId = userDoc.getString("boundDeviceId")
                    
                    if (boundDeviceId != null && boundDeviceId != currentDeviceId) {
                        _uiState.value = AttendanceUiState.Failed("Security Violation: Account is bound to another device. You cannot use a friend's phone.")
                        return@launch
                    } else if (boundDeviceId == null) {
                        // Bind this device on first use
                        firestore.collection("users").document(userId)
                            .update("boundDeviceId", currentDeviceId).await()
                    }
                }

                // Step 1: Check 10-minute window (Layer 1)
                // Fetch session start time from Firestore
                val sessionDoc = firestore.collection("active_sessions").document(subject).get().await()
                
                if (!sessionDoc.exists()) {
                    _uiState.value = AttendanceUiState.Failed("No active session found for '$subject'. Ask faculty to start the session.")
                    return@launch
                }

                // Removed the 10-minute restriction to allow teachers to start attendance whenever they want
                // and students to mark it as long as the session is active.

                // Step 2: Acoustic Verification (Layer 2 - Immediate)
                _uiState.value = AttendanceUiState.VerifyingAcoustic
                _acousticStatus.value = "Analyzing ambient signal..."
                delay(1000)
                _acousticStatus.value = "Processing secure token..."
                
                val tokenDetected = acousticTokenReceiver.listenForToken(timeoutMs = 15000)
                
                if (!tokenDetected) {
                    _acousticStatus.value = "Verification failed"
                    _uiState.value = AttendanceUiState.Failed("Not in Classroom: Acoustic signal not found. Make sure both devices are close and microphone is not covered.")
                    return@launch
                }
                _acousticStatus.value = "Acoustic Signal Detected!"
                acousticVerified = true

                // Step 3: Identity & Geo (Layer 3)
                continueToFinalChecks(subject, faculty, context)
                
            } catch (e: Exception) {
                _uiState.value = AttendanceUiState.Failed("Error: ${e.message}")
            }
        }
    }

    private suspend fun continueToFinalChecks(subject: String, faculty: String, context: android.content.Context) {
        // Step 3: Face Identity & Liveness (Layer 3)
        _uiState.value = AttendanceUiState.VerifyingIdentity
        
        // Wait for the Face Detector with a 30-second timeout
        val faceResult = withTimeoutOrNull(30000L) {
            faceDetector.livenessResultFlow
                .filter { it is LivenessResult.Passed }
                .first()
        }

        if (faceResult !is LivenessResult.Passed) {
            _uiState.value = AttendanceUiState.Failed("Face verification timed out or failed. Please look at the camera and blink.")
            return
        }
        faceVerified = true

        // Step 4: Geofence Check (Layer 4)
        _uiState.value = AttendanceUiState.VerifyingLocation
        val geoResult = validateGeofence()
        if (geoResult !is GeofenceResult.InsideCampus) {
            val message = when(geoResult) {
                is GeofenceResult.OutsideCampus -> "Off campus: ${String.format("%.0f", geoResult.distance)}m away"
                is GeofenceResult.Error -> "Location error: ${geoResult.message}"
                else -> "Off campus"
            }
            _uiState.value = AttendanceUiState.Failed(message)
            return
        }
        geoVerified = true

        // Final Success
        val sessionId = generateSessionId()
        _uiState.value = AttendanceUiState.Verified(
            sessionId = sessionId,
            subject = subject,
            facultyName = faculty,
            timestamp = LocalDateTime.now()
        )
        
        saveAttendanceRecord(subject, faculty)
        startBackgroundPings(context, subject)
    }

    private fun startBackgroundPings(context: android.content.Context, subject: String) {
        isClassInProgress = true
        val intent = android.content.Intent(context, com.acadmate.attendance.geo.AttendanceForegroundService::class.java).apply {
            action = com.acadmate.attendance.geo.AttendanceForegroundService.ACTION_START
            putExtra(com.acadmate.attendance.geo.AttendanceForegroundService.EXTRA_SUBJECT, subject)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun onReauthComplete(context: android.content.Context, subject: String) {
        val intent = android.content.Intent(context, com.acadmate.attendance.geo.AttendanceForegroundService::class.java).apply {
            action = com.acadmate.attendance.geo.AttendanceForegroundService.ACTION_REAUTH_COMPLETE
            putExtra(com.acadmate.attendance.geo.AttendanceForegroundService.EXTRA_SUBJECT, subject)
        }
        context.startService(intent)
    }

    private fun stopBackgroundPings(context: android.content.Context) {
        isClassInProgress = false
        val intent = android.content.Intent(context, com.acadmate.attendance.geo.AttendanceForegroundService::class.java).apply {
            action = com.acadmate.attendance.geo.AttendanceForegroundService.ACTION_STOP
        }
        context.stopService(intent)
    }

    private fun saveAnomalyRecord(reason: String) {
        // Logic to report to faculty/admin via Firestore
    }

    private suspend fun scanForBeacon(): Boolean {
        return try {
            withTimeoutOrNull(15000L) {
                bleScanner.scanForFacultyBeacon()
                    .filter { it.rssi > -70 }
                    .first()
                true
            } == true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun detectFaceLiveness(): LivenessResult {
        return try {
            // This will be called when camera is active
            // For now, return a placeholder
            faceDetector.livenessResultFlow.value ?: LivenessResult.Failed("Detecting...")
        } catch (e: Exception) {
            LivenessResult.Failed(e.message ?: "Face detection error")
        }
    }

    private suspend fun validateGeofence(): GeofenceResult {
        return geofenceValidator.validateLocation()
    }

    private fun saveAttendanceRecord(subject: String, faculty: String) {
        val now = LocalDateTime.now()
        
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser().first()
                val regNo = user?.regNo ?: return@launch
                
                // Extract hour for specific hourly tracking (Each hour is a separate session)
                val currentHour = now.hour
                val hourlySessionId = "${subject.replace(":", "_").replace(" ", "_")}_${now.year}${now.monthValue}${now.dayOfMonth}_$currentHour"

                val displayFaculty = if (faculty.isBlank() || faculty == "Faculty") "Not Assigned" else faculty

                val record = AttendanceRecord(
                    id = hourlySessionId,
                    subject = subject,
                    date = now,
                    markedAt = now,
                    faculty = displayFaculty,
                    status = AttendanceStatus.PRESENT
                )

                // Save to Firestore with hourly session granularity
                val firestoreRecord = hashMapOf(
                    "sessionId" to hourlySessionId,
                    "studentId" to regNo,
                    "subject" to subject,
                    "faculty" to displayFaculty,
                    "hour" to currentHour,
                    "date" to "${now.year}-${now.monthValue}-${now.dayOfMonth}",
                    "timestamp" to System.currentTimeMillis(),
                    "status" to AttendanceStatus.PRESENT.name
                )
                
                firestore.collection("attendance").document("${regNo}_$hourlySessionId")
                    .set(firestoreRecord)
                    .await()

                updateAttendanceHistory(record)
            } catch (e: Exception) {
                _uiState.value = AttendanceUiState.Failed("Sync Failed: ${e.message}")
            }
        }
    }

    private fun updateAttendanceHistory(record: AttendanceRecord) {
        val newSession = AttendanceSession(
            id = record.id,
            subject = record.subject,
            date = record.date,
            startTime = String.format("%02d:%02d", record.date.hour, record.date.minute),
            endTime = String.format("%02d:%02d", record.markedAt.hour, record.markedAt.minute),
            faculty = record.faculty,
            attended = true,
            markedAt = record.markedAt
        )

        val currentHistory = _attendanceHistory.value.toMutableList()
        currentHistory.add(0, newSession)  // Add to top
        _attendanceHistory.value = currentHistory

        // Update monthly summary
        updateMonthlySummary(record)
    }

    private fun updateMonthlySummary(record: AttendanceRecord) {
        val yearMonth = YearMonth.of(record.date.year, record.date.monthValue)
        val currentSummaries = _monthlySummaries.value.toMutableList()

        val summary = currentSummaries.find {
            it.month == yearMonth.monthValue && it.year == yearMonth.year
        }?.let { existing ->
            val newAttended = if (record.status == AttendanceStatus.PRESENT)
                existing.classesAttended + 1
            else
                existing.classesAttended

            existing.copy(
                classesAttended = newAttended,
                attendancePercentage = (newAttended.toFloat() / existing.totalClasses) * 100
            )
        } ?: MonthlySummary(
            month = yearMonth.monthValue,
            year = yearMonth.year,
            totalClasses = 1,
            classesAttended = 1,
            attendancePercentage = 100f
        )

        val index = currentSummaries.indexOfFirst {
            it.month == yearMonth.monthValue && it.year == yearMonth.year
        }

        if (index >= 0) {
            currentSummaries[index] = summary
        } else {
            currentSummaries.add(summary)
        }

        _monthlySummaries.value = currentSummaries.sortedWith(
            compareBy<MonthlySummary> { it.year }.thenBy { it.month }.reversed()
        )
    }

    fun loadAttendanceHistory(studentId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("attendance")
                    .whereEqualTo("studentId", studentId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val history = snapshot.documents.mapNotNull { doc ->
                    val subject = doc.getString("subject") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val date = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(timestamp),
                        java.time.ZoneId.systemDefault()
                    )
                    
                    AttendanceSession(
                        id = doc.id,
                        subject = subject,
                        date = date,
                        startTime = String.format("%02d:%02d", date.hour, date.minute),
                        endTime = String.format("%02d:%02d", date.hour, date.minute), // End time logic can be refined
                        faculty = doc.getString("faculty") ?: "Unknown",
                        attended = true,
                        markedAt = date
                    )
                }
                
                _attendanceHistory.value = history
                updateMonthlySummariesFromHistory(history)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    private fun updateMonthlySummariesFromHistory(history: List<AttendanceSession>) {
        val summaries = history.groupBy { YearMonth.of(it.date.year, it.date.monthValue) }
            .map { (yearMonth, sessions) ->
                val attended = sessions.count { it.attended }
                MonthlySummary(
                    month = yearMonth.monthValue,
                    year = yearMonth.year,
                    totalClasses = sessions.size, // This is a simplification
                    classesAttended = attended,
                    attendancePercentage = (attended.toFloat() / sessions.size) * 100
                )
            }.sortedWith(compareBy<MonthlySummary> { it.year }.thenBy { it.month }.reversed())
        
        _monthlySummaries.value = summaries
    }

    fun getMonthlyAttendance(month: Int, year: Int): MonthlySummary? {
        return _monthlySummaries.value.find {
            it.month == month && it.year == year
        }
    }

    fun getCurrentMonthAttendance(): MonthlySummary? {
        val now = LocalDateTime.now()
        return getMonthlyAttendance(now.monthValue, now.year)
    }

    fun getAttendanceBySubject(subject: String): List<AttendanceSession> {
        return _attendanceHistory.value.filter { it.subject == subject }
    }

    fun reset() {
        faceVerified = false
        geoVerified = false
        _uiState.value = AttendanceUiState.Idle
        faceDetector.reset()
    }

    private fun generateSessionId(): String {
        return "SESSION_${System.currentTimeMillis()}"
    }

    override fun onCleared() {
        super.onCleared()
        faceDetector.reset()
    }
}



