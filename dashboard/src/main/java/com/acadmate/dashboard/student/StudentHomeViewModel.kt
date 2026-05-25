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
    val studentSection: String = "",
    val studentRole: String = "",
    val studentAddress: String = "N/A",
    val attendancePercentage: Float = 0f,
    val currentClass: String? = null,
    val nextClass: String? = null,
    val nextClassIn: String = "--",
    val cgpaEstimate: Float = 0f,
    val isRefreshing: Boolean = false,
    val schedule: List<ScheduleItem> = emptyList(),
    val insights: List<AiInsight> = emptyList(),
    val deadlines: List<Deadline> = emptyList(),
    val courseProgress: Map<String, Float> = emptyMap(),
    val attendanceStreak: Int = 0,
    val taskCount: Int = 0,
    val events: List<com.acadmate.core.model.CampusEvent> = emptyList(),
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
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        studentName = user.name,
                        studentEmail = user.email,
                        studentPhone = user.phoneNumber,
                        studentEnrollment = user.regNo ?: "N/A",
                        studentDepartment = user.department ?: "General",
                        studentSection = user.section ?: "N/A",
                        studentRole = user.role.name,
                        studentAddress = user.address ?: "N/A",
                        profilePictureUrl = user.profilePictureUrl
                    )
                } else {
                    _uiState.value = HomeUiState()
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
                val liveClassId = activeSession?.getString("classId")

                // Fetch Real Timetable for Today
                val calendar = Calendar.getInstance()
                val nowTime = String.format(Locale.getDefault(), "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
                
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
                
                val todayScheduleRaw = timetableRepository.getTimetableForDaySync(dayOfWeek)
                
                // Find currently running class from timetable
                val timetableCurrent = todayScheduleRaw.find { 
                    nowTime >= it.startTime && nowTime <= it.endTime 
                }
                
                // Logic: Student can only mark attendance if:
                // 1. Teacher has started an active session
                // 2. The session corresponds to a class currently running in the timetable
                val isWithinPeriod = liveClassId != null && todayScheduleRaw.any { 
                    it.subject == liveClassId && nowTime >= it.startTime && nowTime <= it.endTime 
                }

                val currentClassDisplay = if (isWithinPeriod) liveClassId else null

                // Find next upcoming class
                val nextUpcoming = todayScheduleRaw.filter { it.startTime > nowTime }
                    .minByOrNull { it.startTime }
                
                val nextClassIn = nextUpcoming?.let {
                    try {
                        val startParts = it.startTime.split(":")
                        val startHour = startParts[0].toInt()
                        val startMin = startParts[1].toInt()
                        
                        val diffMin = (startHour * 60 + startMin) - (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE))
                        if (diffMin > 60) "${diffMin / 60}h ${diffMin % 60}m" else "${diffMin}m"
                    } catch (e: Exception) { "--" }
                } ?: "--"

                val todaySchedule = todayScheduleRaw.map { entity ->
                    ScheduleItem(
                        id = entity.id,
                        subject = entity.subject,
                        faculty = if (entity.faculty.isBlank()) "Not Assigned" else entity.faculty,
                        room = entity.room,
                        time = "${entity.startTime} - ${entity.endTime}",
                        isCurrent = liveClassId == entity.subject || (nowTime >= entity.startTime && nowTime <= entity.endTime)
                    )
                }

                // Calculate course-wise progress based on attendance records
                val progressMap = attendanceTask.documents.groupBy { it.getString("subject") ?: "Other" }
                    .mapValues { (_, records) -> (records.size / 24f).coerceIn(0f, 1f) }

                // Fetch real task count (pending submissions)
                val submissionsTask = firestore.collection("assignment_submissions")
                    .whereEqualTo("studentId", regNo)
                    .get()
                    .await()
                
                val submittedIds = submissionsTask.documents.mapNotNull { it.getString("assignmentId") }.toSet()
                val totalAssignmentsSnapshot = firestore.collection("assignments").get().await()
                val pendingTasksCount = totalAssignmentsSnapshot.size() - submittedIds.size

                // Calculate Dynamic Attendance Streak
                val streak = calculateStreak(attendanceTask.documents)

                // Fetch real events for students
                val eventsSnapshot = firestore.collection("events")
                    .whereGreaterThanOrEqualTo("endDate", System.currentTimeMillis())
                    .get()
                    .await()
                
                val realEvents = eventsSnapshot.documents.mapNotNull { doc ->
                    com.acadmate.core.model.CampusEvent(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        startDate = doc.getLong("startDate") ?: 0L,
                        endDate = doc.getLong("endDate") ?: 0L,
                        location = doc.getString("location") ?: "",
                        type = doc.getString("type") ?: "ACADEMIC"
                    )
                }

                _uiState.value = _uiState.value.copy(
                    attendancePercentage = attendancePercentage,
                    currentClass = currentClassDisplay,
                    nextClass = nextUpcoming?.subject,
                    nextClassIn = nextClassIn,
                    deadlines = deadlines,
                    schedule = todaySchedule,
                    courseProgress = progressMap,
                    attendanceStreak = streak,
                    taskCount = pendingTasksCount.coerceAtLeast(0),
                    events = realEvents,
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

    private fun calculateStreak(attendanceDocs: List<com.google.firebase.firestore.DocumentSnapshot>): Int {
        if (attendanceDocs.isEmpty()) return 0

        val sdf = SimpleDateFormat("yyyy-M-d", Locale.getDefault())
        val attendanceDates = attendanceDocs.mapNotNull { it.getString("date") }
            .mapNotNull { 
                try { sdf.parse(it) } catch (e: Exception) { null }
            }
            .map { 
                val cal = Calendar.getInstance()
                cal.time = it
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis 
            }
            .distinct()
            .sortedDescending()

        if (attendanceDates.isEmpty()) return 0

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterday = today - 86400000L

        // Streak only counts if they attended today or yesterday
        if (attendanceDates[0] < yesterday) return 0

        var currentStreak = 1
        for (i in 0 until attendanceDates.size - 1) {
            val diff = attendanceDates[i] - attendanceDates[i + 1]
            if (diff <= 86400000L) { // 1 day difference (allowing for multiple sessions same day)
                currentStreak++
            } else {
                break
            }
        }
        return currentStreak
    }

    fun dismissInsight(id: String) {
        _uiState.value = _uiState.value.copy(
            insights = _uiState.value.insights.filter { it.id != id }
        )
    }
}
