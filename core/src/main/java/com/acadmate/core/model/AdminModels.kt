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
    SYSTEM_ALERT
}
