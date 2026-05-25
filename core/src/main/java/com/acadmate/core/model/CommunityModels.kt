package com.acadmate.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CommunityGroup(
    val id: String = "",
    val section: String = "",
    val subject: String = "",
    val facultyId: String = "",
    val facultyName: String = "",
    val crIds: List<String> = emptyList(), // Max 2
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class CommunityMessage(
    val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val text: String = "",
    val attachmentUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.APPROVED // Default for Admin/Faculty/CR
)

enum class MessageStatus {
    PENDING,
    APPROVED,
    REJECTED
}
