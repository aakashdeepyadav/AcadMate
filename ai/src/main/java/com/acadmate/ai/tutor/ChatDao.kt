package com.acadmate.ai.tutor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM ai_chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM ai_chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteSessionMessages(sessionId: String)

    @Query("SELECT * FROM chat_sessions ORDER BY lastUpdated DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): ChatSession?
}
