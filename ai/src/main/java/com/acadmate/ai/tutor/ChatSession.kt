package com.acadmate.ai.tutor

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val mode: String, // TUTOR, INTERVIEW, PLANNER
    val subject: String? = null,
    val unit: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
