package com.acadmate.dashboard.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.db.UserRepository
import com.acadmate.core.db.TimetableRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

data class HomeUiState(
    val studentName: String = "Loading...",
    val studentEmail: String = "",
    val studentPhone: String = "",
    val studentEnrollment: String = "",
    val studentDepartment: String = "",
    val studentRole: String = "",
    val studentAddress: String = "N/A",
    val attendancePercentage: Float = 0f,
    val nextClass: String = "No Active Class",
    val nextClassIn: String = "--",
    val cgpaEstimate: Float = 0f,
    val isRefreshing: Boolean = false,
    val schedule: List<ScheduleItem> = emptyList(),
    val insights: List<AiInsight> = emptyList(),
    val deadlines: List<Deadline> = emptyList(),
    val courseProgress: Map<String, Float> = emptyMap(),
    val profilePictureUrl: String? = null,
    val smartSuggestion: String? = "Stay ahead! Review your syllabus progress today.",
    val freeTimeUtilization: String? = null
)

data class ScheduleItem(
    val id: String,
    val subject: String,
    val faculty: String,
    val room: String,
    val time: String,
    val isCurrent: Boolean = false
)

data class AiInsight(
    val id: String,
    val message: String,
    val type: String = "warning"
)

data class Deadline(
    val id: String,
    val title: String,
    val dueDate: String,
    val urgency: Urgency = Urgency.MEDIUM
)

enum class Urgency { HIGH, MEDIUM, LOW }

@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val timetableRepository: TimetableRepository
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(HomeUiState(
        studentName = auth.currentUser?.displayName ?: "Loading...",
        studentEmail = auth.currentUser?.email ?: "",
        studentPhone = auth.currentUser?.phoneNumber ?: ""
    ))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        loadDashboardData()
        calculateSmartFreeTime()
        prefetchTabData()
        
        // Sync Global Timetable from Firestore
        viewModelScope.launch {
            timetableRepository.syncGlobalTimetable()
        }
    }

    private fun prefetchTabData() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                firestore.collection("syllabus_gaps").whereEqualTo("studentId", userId).get()
                firestore.collection("attendance").whereEqualTo("studentId", userId).limit(10).get()
            } catch (e: Exception) { }
        }
    }

    private fun calculateSmartFreeTime() {
        viewModelScope.launch {
            delay(1000)
            val freeTimeMsg = "Detected a gap. Would you like to cover 'Android Components' now?"
            _uiState.value = _uiState.value.copy(freeTimeUtilization = freeTimeMsg)
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            // Trigger sync first to ensure local data is up to date
            userRepository.syncUserData(userId)
            
            userRepository.getCurrentUser().collectLatest { user ->
                user?.let {
                    _uiState.value = _uiState.value.copy(
                        studentName = it.name,
                        studentEmail = it.email,
                        studentPhone = it.phoneNumber,
                        studentEnrollment = it.regNo ?: "N/A",
                        studentDepartment = it.department ?: "General",
                        studentRole = it.role.name,
                        studentAddress = it.address ?: "N/A",
                        profilePictureUrl = it.profilePictureUrl
                    )
                }
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            val user = userRepository.getCurrentUser().first()
            val regNo = user?.regNo ?: return@launch

            try {
                // Fetch Attendance Stats using regNo
                val attendanceTask = firestore.collection("attendance")
                    .whereEqualTo("studentId", regNo)
                    .get()
                    .await()
                
                val attendedCount = attendanceTask.size()
                val attendancePercentage = if (attendedCount > 0) (attendedCount / 136f).coerceIn(0f, 1f) else 0f

                // Fetch Upcoming Assignments
                val assignmentsTask = firestore.collection("assignments")
                    .orderBy("dueDate", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .limit(5)
                    .get()
                    .await()
                
                val deadlines = assignmentsTask.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val dueDateLong = doc.getLong("dueDate") ?: 0L
                    Deadline(
                        id = doc.id,
                        title = title,
                        dueDate = formatTimestamp(dueDateLong),
                        urgency = if (dueDateLong - System.currentTimeMillis() < 86400000) Urgency.HIGH else Urgency.MEDIUM
                    )
                }

                // Fetch Active Sessions from real Firestore collection
                val activeSessionsTask = firestore.collection("active_sessions")
                    .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                // Find any live session (prefer the most recent one)
                val activeSession = activeSessionsTask.documents.firstOrNull()
                
                val currentClass = if (activeSession != null) {
                    activeSession.getString("classId") ?: "No Active Class"
                } else {
                    "No Active Class"
                }

                // Fetch Real Timetable for Today
                val calendar = Calendar.getInstance()
                val dayOfWeek = when(calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> 1
                }
                
                val todaySchedule = timetableRepository.getTimetableForDaySync(dayOfWeek)
                    .map { entity ->
                        ScheduleItem(
                            id = entity.id,
                            subject = entity.subject,
                            faculty = if (entity.faculty.isBlank()) "Not Assigned" else entity.faculty,
                            room = entity.room,
                            time = "${entity.startTime} - ${entity.endTime}",
                            isCurrent = activeSession?.getString("classId") == entity.subject
                        )
                    }

                // Calculate course-wise progress based on attendance records
                val progressMap = attendanceTask.documents.groupBy { it.getString("subject") ?: "Other" }
                    .mapValues { (_, records) -> (records.size / 24f).coerceIn(0f, 1f) }

                _uiState.value = _uiState.value.copy(
                    attendancePercentage = attendancePercentage,
                    nextClass = currentClass,
                    deadlines = deadlines,
                    schedule = todaySchedule,
                    courseProgress = progressMap,
                    isRefreshing = false,
                    smartSuggestion = if (activeSession != null) "A live session for ${activeSession.getString("classId")} is active. Mark your attendance now!" else "Stay ahead! Review your syllabus progress today."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun dismissInsight(id: String) {
        _uiState.value = _uiState.value.copy(
            insights = _uiState.value.insights.filter { it.id != id }
        )
    }
}
