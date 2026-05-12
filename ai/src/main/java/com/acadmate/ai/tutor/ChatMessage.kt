package com.acadmate.ai.tutor

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MessageRole {
    USER, MODEL
}

@Entity(tableName = "ai_chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val text: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isStreaming: Boolean = false
)
