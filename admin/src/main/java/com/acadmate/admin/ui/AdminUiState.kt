package com.acadmate.admin.ui

import com.acadmate.core.model.AdminAction
import com.acadmate.core.model.UserRole

sealed class AdminUiState {
    object Loading : AdminUiState()
    data class Success(
        val totalStudents: Int = 0,
        val totalFaculty: Int = 0,
        val totalCourses: Int = 0,
        val activeClasses: Int = 0,
        val avgAttendance: Float = 0f,
        val pendingApprovals: Int = 0,
        val pendingLeaves: Int = 0,
        val activeEvents: Int = 0,
        val institutionName: String = "",
        val recentActions: List<AdminAction> = emptyList(),
        val departmentAttendance: Map<String, Float> = emptyMap(),
        val passPercentage: Float = 0f
    ) : AdminUiState()
    data class Error(val message: String) : AdminUiState()
}
