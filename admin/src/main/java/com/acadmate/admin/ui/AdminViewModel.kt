package com.acadmate.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.db.UserEntity
import com.acadmate.core.model.ActionType
import com.acadmate.core.model.AdminAction
import com.acadmate.core.model.UserRole
import com.acadmate.core.util.ValidationUtils
import com.google.firebase.firestore.FirebaseFirestore
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

    init {
        loadAdminDashboard()
        loadUsers()
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
                        isFirstLogin = doc.getBoolean("isFirstLogin") ?: true
                    )
                }
                _usersList.value = users
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId).delete().await()
                _usersList.value = _usersList.value.filter { it.id != userId }
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
                        com.acadmate.core.model.AdminAction(
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

                    _uiState.value = AdminUiState.Success(
                        totalStudents = studentCount,
                        totalFaculty = facultyCount,
                        totalCourses = courseCount,
                        activeClasses = activeSessions,
                        avgAttendance = avgAttendance,
                        pendingApprovals = totalAnomalies,
                        institutionName = adminName,
                        recentActions = realActions.ifEmpty { 
                            listOf(
                                com.acadmate.core.model.AdminAction(
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

    fun createInstitutionalUser(
        regNo: String,
        name: String,
        email: String,
        role: UserRole,
        phoneNumber: String
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
                    "isFirstLogin" to true,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
                
                // Add to Firestore. In a production app, you would also create a Firebase Auth entry.
                firestore.collection("users").document(regNo).set(userData).await()
                
                // Log the action
                val actionData = hashMapOf(
                    "title" to "New User Created",
                    "timestamp" to System.currentTimeMillis(),
                    "type" to ActionType.USER_CREATED.name,
                    "description" to "$name ($role) added to the system."
                )
                firestore.collection("admin_logs").add(actionData).await()
                
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
        phoneNumber: String
    ) {
        viewModelScope.launch {
            try {
                val userData = hashMapOf<String, Any>(
                    "name" to name,
                    "email" to email,
                    "role" to role.name,
                    "phoneNumber" to phoneNumber,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                firestore.collection("users").document(userId).update(userData).await()
                
                loadUsers() // Refresh list
                loadAdminDashboard() // Refresh stats
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Failed to update user: ${e.message}")
            }
        }
    }

    fun postAnnouncement(title: String, content: String) {
        viewModelScope.launch {
            try {
                val announcementData = hashMapOf(
                    "title" to title,
                    "content" to content,
                    "timestamp" to System.currentTimeMillis(),
                    "author" to "Admin"
                )
                firestore.collection("announcements").add(announcementData).await()
                
                // Add to recent actions
                val newAction = com.acadmate.core.model.AdminAction(
                    id = System.currentTimeMillis().toString(),
                    title = "Announcement Posted",
                    timestamp = System.currentTimeMillis(),
                    type = ActionType.ANNOUNCEMENT_POSTED,
                    description = title
                )
                
                val currentState = _uiState.value
                if (currentState is AdminUiState.Success) {
                    _uiState.value = currentState.copy(
                        recentActions = listOf(newAction) + currentState.recentActions.take(9)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error("Failed to post announcement: ${e.message}")
            }
        }
    }
}
