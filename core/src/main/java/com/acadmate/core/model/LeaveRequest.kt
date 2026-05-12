package com.acadmate.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaveRequest(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val reason: String = "",
    val status: LeaveStatus = LeaveStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val handledBy: String? = null,
    val responseNote: String? = null
)

enum class LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED
}
