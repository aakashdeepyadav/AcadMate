package com.acadmate.core.alarms

import java.time.LocalDateTime

data class AlarmItem(
    val id: String,
    val time: LocalDateTime,
    val title: String,
    val message: String,
    val type: String = "GENERIC" // "CLASS", "ASSIGNMENT", "NOTICE", etc.
)
