package com.acadmate.core.model

data class AdminAction(
    val id: String = "",
    val title: String = "",
    val timestamp: Long = 0L,
    val type: ActionType = ActionType.SYSTEM_ALERT,
    val description: String = ""
)

enum class ActionType {
    USER_CREATED,
    INSTITUTION_UPDATED,
    ANNOUNCEMENT_POSTED,
    COURSE_ADDED,
    ATTENDANCE_ANALYTICS_GENERATED,
    SYSTEM_ALERT,
    EVENT_CREATED,
    LEAVE_APPROVED,
    INFRASTRUCTURE_UPDATED
}

data class CampusEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val location: String = "",
    val type: String = "ACADEMIC"
)
