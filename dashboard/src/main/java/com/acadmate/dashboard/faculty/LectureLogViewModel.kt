package com.acadmate.dashboard.faculty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.model.LectureLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LectureLogViewModel @Inject constructor() : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _logs = MutableStateFlow<List<LectureLog>>(emptyList())
    val logs: StateFlow<List<LectureLog>> = _logs

    init {
        loadLogs()
    }

    fun loadLogs() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("lecture_logs")
                    .whereEqualTo("facultyId", userId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                _logs.value = snapshot.documents.map { doc ->
                    LectureLog(
                        id = doc.id,
                        facultyId = doc.getString("facultyId") ?: "",
                        topicCovered = doc.getString("topicCovered") ?: "",
                        unitTitle = doc.getString("unitTitle") ?: "",
                        date = doc.getString("date") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        notes = doc.getString("notes") ?: ""
                    )
                }
            } catch (e: Exception) {}
        }
    }

    fun addLog(topic: String, unit: String, notes: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val newLog = LectureLog(
                    facultyId = userId,
                    topicCovered = topic,
                    unitTitle = unit,
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    timestamp = System.currentTimeMillis(),
                    notes = notes
                )
                firestore.collection("lecture_logs").add(newLog).await()
                loadLogs()
            } catch (e: Exception) {}
        }
    }
}
