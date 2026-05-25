package com.acadmate.core.db

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoticeRepository @Inject constructor(
    private val noticeDao: NoticeDao
) {
    private val firestore = FirebaseFirestore.getInstance()

    fun getAllNotices(): Flow<List<NoticeEntity>> {
        return noticeDao.getAllNotices()
    }

    suspend fun syncNotices() {
        try {
            val snapshot = firestore.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val remoteNotices = snapshot.documents.map { doc ->
                val timestamp = doc.getLong("timestamp") ?: 0L
                val sdf = java.text.SimpleDateFormat("dd MMM, yyyy", java.util.Locale.getDefault())
                val dateStr = sdf.format(java.util.Date(timestamp))
                
                NoticeEntity(
                    id = doc.id,
                    title = doc.getString("title") ?: "No Title",
                    date = dateStr,
                    content = doc.getString("content") ?: "",
                    timestamp = timestamp,
                    attachmentUrl = doc.getString("attachmentUrl")
                )
            }

            if (remoteNotices.isNotEmpty()) {
                noticeDao.clearAllNotices()
                noticeDao.insertNotices(remoteNotices)
            }
        } catch (e: Exception) {
            // Offline
        }
    }

    suspend fun postNotice(title: String, content: String, attachmentUrl: String? = null) {
        val timestamp = System.currentTimeMillis()
        val noticeData = hashMapOf(
            "title" to title,
            "content" to content,
            "timestamp" to timestamp,
            "date" to java.text.SimpleDateFormat("dd MMM, yyyy", java.util.Locale.getDefault()).format(java.util.Date(timestamp)),
            "attachmentUrl" to attachmentUrl
        )
        firestore.collection("announcements").add(noticeData).await()
        syncNotices()
    }
}
