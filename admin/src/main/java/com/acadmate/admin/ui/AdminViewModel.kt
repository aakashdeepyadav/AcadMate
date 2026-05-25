package com.acadmate.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.db.UserEntity
import com.acadmate.core.model.*
import com.acadmate.core.util.ValidationUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val userRepository: com.acadmate.core.db.UserRepository
) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Loading)
    val uiState: StateFlow<AdminUiState> = _uiState
    
    private val _usersList = MutableStateFlow<List<UserEntity>>(emptyList())
    val usersList: StateFlow<List<UserEntity>> = _usersList

    private val _departments = MutableStateFlow<List<String>>(emptyList())
    val departments: StateFlow<List<String>> = _departments

    private val _sections = MutableStateFlow<List<String>>(emptyList())
    val sections: StateFlow<List<String>> = _sections

    private val _currentSemester = MutableStateFlow(1)
    val currentSemester: StateFlow<Int> = _currentSemester

    private val _leaveRequests = MutableStateFlow<List<LeaveRequest>>(emptyList())
    val leaveRequests: StateFlow<List<LeaveRequest>> = _leaveRequests

    private val _events = MutableStateFlow<List<CampusEvent>>(emptyList())
    val events: StateFlow<List<CampusEvent>> = _events

    init {
        loadAdminDashboard()
        loadUsers()
        loadInstitutionData()
        loadLeaveRequests()
        loadEvents()
    }

    fun loadLeaveRequests() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("leave_requests")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()
                _leaveRequests.value = snapshot.toObjects(LeaveRequest::class.java)
            } catch (e: Exception) {}
        }
    }

    fun updateLeaveStatus(leaveId: String, status: LeaveStatus, responseNote: String? = null) {
        viewModelScope.launch {
            try {
                val leaveRef = firestore.collection("leave_requests").document(leaveId)
                val leaveDoc = leaveRef.get().await()
                val leave = leaveDoc.toObject(LeaveRequest::class.java) ?: return@launch

                firestore.runTransaction { transaction ->
                    transaction.update(leaveRef, mapOf(
                        "status" to status.name,
                        "responseNote" to responseNote,
                        "handledBy" to "Admin"
                    ))
                }.await()

                // If approved, rectify attendance for the period
                if (status == LeaveStatus.APPROVED) {
                    rectifyAttendanceForLeave(leave)
                }

                loadLeaveRequests()
                loadAdminDashboard()
                logAdminAction("Leave ${status.name}", ActionType.LEAVE_APPROVED, "Request $leaveId status changed to ${status.name}")
            } catch (e: Exception) {}
        }
    }

    private suspend fun rectifyAttendanceForLeave(leave: LeaveRequest) {
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-M-d", java.util.Locale.getDefault())
            
            val startCal = java.util.Calendar.getInstance().apply { timeInMillis = leave.startDate }
            val endCal = java.util.Calendar.getInstance().apply { timeInMillis = leave.endDate }
            
            // Iterate through each day of the leave
            val currentCal = startCal.clone() as java.util.Calendar
            while (!currentCal.after(endCal)) {
                val dateStr = sdf.format(currentCal.time)
                
                // Find attendance records for this student on this date
                val attendanceSnapshot = firestore.collection("attendance")
                    .whereEqualTo("studentId", leave.studentId)
                    .whereEqualTo("date", dateStr)
                    .get()
                    .await()
                
                val batch = firestore.batch()
                attendanceSnapshot.documents.forEach { doc ->
                    // Change status to LEAVE (rectified)
                    batch.update(doc.reference, "status", "LEAVE")
                }
                batch.commit().await()
                
                currentCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        } catch (e: Exception) {
            // Log error or handle
        }
    }

    fun loadEvents() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("events")
                    .orderBy("startDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()
                _events.value = snapshot.documents.mapNotNull { doc ->
                    CampusEvent(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        startDate = doc.getLong("startDate") ?: 0L,
                        endDate = doc.getLong("endDate") ?: 0L,
                        location = doc.getString("location") ?: ""
                    )
                }
            } catch (e: Exception) {}
        }
    }

    fun createEvent(title: String, desc: String, start: Long, end: Long, location: String) {
        viewModelScope.launch {
            try {
                val event = hashMapOf(
                    "title" to title,
                    "description" to desc,
                    "startDate" to start,
                    "endDate" to end,
                    "location" to location
                )
                firestore.collection("events").add(event).await()
                loadEvents()
                loadAdminDashboard()
                logAdminAction("Event Created", ActionType.EVENT_CREATED, title)
            } catch (e: Exception) {}
        }
    }

    private fun loadInstitutionData() {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("institution").document("config").get().await()
                if (doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    _departments.value = doc.get("departments") as? List<String> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    _sections.value = doc.get("sections") as? List<String> ?: emptyList()
                    _currentSemester.value = (doc.getLong("currentSemester") ?: 1L).toInt()
                }
            } catch (e: Exception) {}
        }
    }

    fun updateSemester(semester: Int) {
        viewModelScope.launch {
            try {
                firestore.collection("institution").document("config")
                    .set(mapOf("currentSemester" to semester), SetOptions.merge()).await()
                _currentSemester.value = semester
                logAdminAction("Semester Updated", ActionType.INSTITUTION_UPDATED, "Institutional semester set to $semester")
            } catch (e: Exception) {}
        }
    }

    fun addDepartment(name: String) {
        viewModelScope.launch {
            val newList = _departments.value + name
            firestore.collection("institution").document("config")
                .set(mapOf("departments" to newList), SetOptions.merge()).await()
            _departments.value = newList
            logAdminAction("Department Added", ActionType.INSTITUTION_UPDATED, "New department: $name")
        }
    }

    fun deleteDepartment(name: String) {
        viewModelScope.launch {
            val newList = _departments.value - name
            firestore.collection("institution").document("config")
                .set(mapOf("departments" to newList), SetOptions.merge()).await()
            _departments.value = newList
            logAdminAction("Department Removed", ActionType.INSTITUTION_UPDATED, "Removed department: $name")
        }
    }

    fun addSection(name: String) {
        viewModelScope.launch {
            val newList = _sections.value + name
            firestore.collection("institution").document("config")
                .set(mapOf("sections" to newList), SetOptions.merge()).await()
            _sections.value = newList
            logAdminAction("Section Added", ActionType.INSTITUTION_UPDATED, "New section: $name")
        }
    }

    fun deleteSection(name: String) {
        viewModelScope.launch {
            val newList = _sections.value - name
            firestore.collection("institution").document("config")
                .set(mapOf("sections" to newList), SetOptions.merge()).await()
            _sections.value = newList
            logAdminAction("Section Removed", ActionType.INSTITUTION_UPDATED, "Removed section: $name")
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").get().await()
                val users = snapshot.documents.map { doc ->
                    UserEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unknown",
                        email = doc.getString("email") ?: "",
                        phoneNumber = doc.getString("phoneNumber") ?: "",
                        regNo = doc.getString("regNo"),
                        role = UserRole.fromString(doc.getString("role")),
                        department = doc.getString("department"),
                        section = doc.getString("section"),
                        isFirstLogin = doc.getBoolean("isFirstLogin") ?: true
                    )
                }
                _usersList.value = users
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun bulkCreateUsers(users: List<Map<String, String>>) {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                val batch = firestore.batch()
                var count = 0
                
                users.forEach { data ->
                    val regNo = data["regNo"] ?: return@forEach
                    val name = data["name"] ?: ""
                    val email = data["email"] ?: ""
                    val roleStr = data["role"] ?: "STUDENT"
                    val role = UserRole.fromString(roleStr)
                    val phone = data["phoneNumber"] ?: ""
                    
                    val formattedPhone = if (phone.startsWith("+")) phone else "+91$phone"
                    
                    val userData = hashMapOf(
                        "regNo" to regNo,
                        "name" to name,
                        "email" to email,
                        "role" to role.name,
                        "phoneNumber" to formattedPhone,
                        "password" to "Password@123",
                        "isFirstLogin" to true,
                        "createdAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    
                    val docRef = firestore.collection("users").document(regNo)
                    batch.set(docRef, userData)
                    count++
                }
                
                if (count > 0) {
                    batch.commit().await()
                    logAdminAction("Bulk Import Completed", ActionType.USER_CREATED, "Imported $count users via CSV.")
                }
                
                loadUsers()
                loadAdminDashboard()
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Bulk import failed: ${e.message}")
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId).delete().await()
                _usersList.value = _usersList.value.filter { it.id != userId }
                logAdminAction("User Removed", ActionType.SYSTEM_ALERT, "Account ID $userId was deleted from the system.")
                loadAdminDashboard() // Refresh stats
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Failed to delete user: ${e.message}")
            }
        }
    }

    fun loadAdminDashboard() {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            
            // Collect user data to ensure name is always up to date
            userRepository.getCurrentUser().collectLatest { user ->
                val adminName = user?.name ?: "System Admin"
                
                try {
                    // Fetch stats from Firestore
                    val studentCount = firestore.collection("users")
                        .whereEqualTo("role", UserRole.STUDENT.name)
                        .get()
                        .await()
                        .size()

                    val facultyCount = firestore.collection("users")
                        .whereEqualTo("role", UserRole.FACULTY.name)
                        .get()
                        .await()
                        .size()

                    val courseCount = firestore.collection("courses")
                        .get()
                        .await()
                        .size()
                    
                    val activeSessions = firestore.collection("active_sessions")
                        .get()
                        .await()
                        .size()

                    val pendingLeaves = firestore.collection("leave_requests")
                        .whereEqualTo("status", LeaveStatus.PENDING.name)
                        .get()
                        .await()
                        .size()

                    val activeEvents = firestore.collection("events")
                        .whereGreaterThan("endDate", System.currentTimeMillis())
                        .get()
                        .await()
                        .size()
                    
                    // Calculate real average attendance across all students
                    val attendanceSnapshot = firestore.collection("attendance").get().await()
                    val studentsSnapshotForAvg = firestore.collection("users").whereEqualTo("role", UserRole.STUDENT.name).get().await()
                    val totalPossibleAttendances = studentsSnapshotForAvg.size().coerceAtLeast(1)
                    
                    val calendar = java.util.Calendar.getInstance()
                    val dateStr = "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH) + 1}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
                    
                    val uniqueMarkedToday = attendanceSnapshot.documents.filter { 
                        it.getString("date") == dateStr
                    }.distinctBy { it.getString("studentId") }.size
                    
                    val avgAttendance = (uniqueMarkedToday.toFloat() / totalPossibleAttendances) * 100f

                    // Fetch recent logs from Firestore
                    val actionsSnapshot = firestore.collection("admin_logs")
                        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(10)
                        .get()
                        .await()
                    
                    val realActions = actionsSnapshot.documents.map { doc ->
                        AdminAction(
                            id = doc.id,
                            title = doc.getString("title") ?: "Action",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            type = ActionType.valueOf(doc.getString("type") ?: "USER_CREATED"),
                            description = doc.getString("description") ?: ""
                        )
                    }

                    // Fetch real-time anomalies across all active sessions
                    val anomaliesSnapshot = firestore.collectionGroup("anomalies").get().await()
                    val totalAnomalies = anomaliesSnapshot.size()

                    // Dynamic Departmental Attendance
                    val deptAttendanceMap = mutableMapOf<String, Float>()
                    val deptStudents = studentsSnapshotForAvg.documents.groupBy { it.getString("department") ?: "General" }
                    
                    deptStudents.forEach { (dept, students) ->
                        val studentIds = students.map { it.id }.toSet()
                        val attendanceForDept = attendanceSnapshot.documents.filter { 
                            it.getString("date") == dateStr && it.getString("studentId") in studentIds
                        }.distinctBy { it.getString("studentId") }.size
                        
                        val deptPercentage = (attendanceForDept.toFloat() / students.size.coerceAtLeast(1))
                        deptAttendanceMap[dept] = deptPercentage
                    }

                    // Dynamic Pass Percentage (from grades collection if exists, otherwise placeholder)
                    var passPercentage = 88.5f // Default
                    try {
                        val gradesSnapshot = firestore.collection("grades").get().await()
                        if (!gradesSnapshot.isEmpty) {
                            val totalGrades = gradesSnapshot.size()
                            val passingGrades = gradesSnapshot.documents.count { 
                                val grade = it.getString("grade") ?: "F"
                                grade != "F" && grade != "E"
                            }
                            passPercentage = (passingGrades.toFloat() / totalGrades) * 100f
                        }
                    } catch (e: Exception) {
                        // Keep default if grades collection doesn't exist yet
                    }

                    _uiState.value = AdminUiState.Success(
                        totalStudents = studentCount,
                        totalFaculty = facultyCount,
                        totalCourses = courseCount,
                        activeClasses = activeSessions,
                        avgAttendance = avgAttendance,
                        pendingApprovals = totalAnomalies,
                        pendingLeaves = pendingLeaves,
                        activeEvents = activeEvents,
                        institutionName = adminName,
                        departmentAttendance = deptAttendanceMap,
                        passPercentage = passPercentage,
                        recentActions = realActions.ifEmpty { 
                            listOf(
                                AdminAction(
                                    "0",
                                    "System Initialization",
                                    System.currentTimeMillis(),
                                    ActionType.INSTITUTION_UPDATED,
                                    "Institutional dashboard is now live and monitoring $studentCount students."
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    _uiState.value = AdminUiState.Error(e.message ?: "Failed to load dashboard")
                }
            }
        }
    }

    private suspend fun logAdminAction(title: String, type: ActionType, description: String) {
        try {
            val actionData = hashMapOf(
                "title" to title,
                "timestamp" to System.currentTimeMillis(),
                "type" to type.name,
                "description" to description
            )
            firestore.collection("admin_logs").add(actionData).await()
        } catch (e: Exception) {
            // Silently fail logging
        }
    }

    fun createInstitutionalUser(
        regNo: String,
        name: String,
        email: String,
        role: UserRole,
        phoneNumber: String,
        department: String? = null,
        section: String? = null
    ) {
        if (!ValidationUtils.isValidRegistrationNumber(regNo, role)) {
            _uiState.value = AdminUiState.Error(ValidationUtils.getRegistrationNumberErrorMessage(role))
            return
        }

        viewModelScope.launch {
            try {
                // Check if user with this regNo already exists
                val existing = firestore.collection("users")
                    .whereEqualTo("regNo", regNo)
                    .get()
                    .await()
                
                if (!existing.isEmpty) {
                    _uiState.value = AdminUiState.Error("User with Registration Number $regNo already exists")
                    return@launch
                }

                // Ensure phone number has international prefix for correct Auth matching
                val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"

                val userData = hashMapOf(
                    "regNo" to regNo,
                    "name" to name,
                    "email" to email,
                    "role" to role.name,
                    "phoneNumber" to formattedPhone,
                    "password" to "Password@123", // Default institutional password
                    "department" to department,
                    "section" to section,
                    "isFirstLogin" to true,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
                
                // Add to Firestore. In a production app, you would also create a Firebase Auth entry.
                firestore.collection("users").document(regNo).set(userData).await()
                
                logAdminAction("New User Created", ActionType.USER_CREATED, "$name ($role) added to the system.")
                
                loadUsers() // Refresh list
                loadAdminDashboard() // Refresh stats
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Failed to create user: ${e.message}")
            }
        }
    }

    fun updateUser(
        userId: String,
        name: String,
        email: String,
        role: UserRole,
        phoneNumber: String,
        department: String? = null,
        section: String? = null
    ) {
        viewModelScope.launch {
            try {
                val userData = hashMapOf<String, Any?>(
                    "name" to name,
                    "email" to email,
                    "role" to role.name,
                    "phoneNumber" to phoneNumber,
                    "department" to department,
                    "section" to section,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                firestore.collection("users").document(userId).update(userData).await()
                
                logAdminAction("User Updated", ActionType.USER_CREATED, "Profile for $name was updated.")
                
                loadUsers() // Refresh list
                loadAdminDashboard() // Refresh stats
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Failed to update user: ${e.message}")
            }
        }
    }

    fun postAnnouncement(title: String, content: String, attachmentUrl: String? = null) {
        viewModelScope.launch {
            try {
                val announcementData = hashMapOf(
                    "title" to title,
                    "content" to content,
                    "timestamp" to System.currentTimeMillis(),
                    "author" to "Admin",
                    "attachmentUrl" to attachmentUrl
                )
                firestore.collection("announcements").add(announcementData).await()
                
                logAdminAction("Announcement Posted", ActionType.ANNOUNCEMENT_POSTED, title)
                
                loadAdminDashboard() // Refresh dashboard to see new log
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Failed to post announcement: ${e.message}")
            }
        }
    }
}
