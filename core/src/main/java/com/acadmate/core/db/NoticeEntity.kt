package com.acadmate.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notices",
    indices = [Index(value = ["timestamp"])]
)
data class NoticeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val date: String,
    val timestamp: Long,
    val attachmentUrl: String? = null
)
