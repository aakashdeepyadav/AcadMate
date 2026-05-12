package com.acadmate.ai.tutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.datastore.OnboardingDataStore
import com.acadmate.core.model.Assignment
import com.acadmate.core.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AiTutorViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val generativeModel: GenerativeModel,
    private val onboardingDataStore: OnboardingDataStore
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private var currentSessionId: String = UUID.randomUUID().toString()

    fun loadConversationHistory(sessionId: String) {
        currentSessionId = sessionId
        viewModelScope.launch {
            chatDao.getMessagesBySession(sessionId).collect {
                _messages.value = it
            }
        }
    }

    private suspend fun getStudentContext(): String {
        val userId = auth.currentUser?.uid ?: return ""
        val role = onboardingDataStore.selectedRole.first()
        
        if (role != UserRole.STUDENT) return ""

        return try {
            // Fetch Assignments
            val assignmentsTask = firestore.collection("assignments").get().await()
            val assignments = assignmentsTask.toObjects(Assignment::class.java)
            val assignmentsContext = if (assignments.isNotEmpty()) {
                "Recent Assignments: " + assignments.joinToString("; ") { "${it.title} (Due: ${it.dueDate})" }
            } else "No assignments found."

            // Fetch Attendance
            val attendanceTask = firestore.collection("attendance")
                .whereEqualTo("studentId", userId)
                .get()
                .await()
            val attendanceRecords = attendanceTask.documents
            val attendanceContext = if (attendanceRecords.isNotEmpty()) {
                "Attendance: You have attended ${attendanceRecords.size} sessions recently."
            } else "No attendance records found."

            // Fetch Lecture Notes for RAG context
            val notesTask = firestore.collection("lecture_notes")
                .whereEqualTo("userId", userId)
                .limit(5)
                .get()
                .await()
            val notes = notesTask.documents.mapNotNull { it.getString("content") }
            val notesContext = if (notes.isNotEmpty()) {
                "\nRecent Lecture Notes (The following are transcripts/summaries of recent lectures the student has recorded. Answer questions based on these if the student refers to 'my notes' or 'the lecture'):\n" + notes.joinToString("\n---\n")
            } else ""

            // Fetch Exam Results for gap analysis context
            val examTask = firestore.collection("exam_results")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .await()
            val exams = examTask.documents
            val examContext = if (exams.isNotEmpty()) {
                "\nRecent Exam Performance (Help the student with these weak topics):\n" + exams.joinToString("\n") { 
                    "Score: ${it.getLong("score")}/${it.getLong("total")}. Weak Topics: ${it.get("weakTopics")}"
                }
            } else ""

            // Fetch Syllabus Gaps
            val gapsDoc = firestore.collection("syllabus_gaps").document(userId).get().await()
            val gapsContext = if (gapsDoc.exists()) {
                "\nSyllabus Gaps (Focus areas for study planning):\n" + gapsDoc.get("gaps")
            } else ""

            // Fetch Knowledge Base (Syllabus) context
            val knowledgeSnapshot = firestore.collection("users")
                .document(userId)
                .collection("knowledge_base")
                .get()
                .await()
            
            val syllabusContext = if (!knowledgeSnapshot.isEmpty) {
                "\nIndexed Syllabi (You have access to these full course syllabi for B.Tech CSE 6th Sem):\n" + 
                knowledgeSnapshot.documents.joinToString("\n") { doc ->
                    "${doc.getString("subjectCode")}: ${doc.getString("subjectName")} - Units: ${doc.get("units")}"
                }
            } else ""

            "\nContextual Data:\n$assignmentsContext\n$attendanceContext$notesContext$examContext$gapsContext$syllabusContext"
        } catch (e: Exception) {
            ""
        }
    }

    fun sendMessage(text: String, subject: String?, unit: String? = null) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            sessionId = currentSessionId,
            text = text,
            role = MessageRole.USER
        )

        viewModelScope.launch {
            chatDao.insertMessage(userMessage)
            _suggestions.value = emptyList()
            _isTyping.value = true

            val aiMessageId = UUID.randomUUID().toString()
            var aiText = ""
            
            val aiMessage = ChatMessage(
                id = aiMessageId,
                sessionId = currentSessionId,
                text = "",
                role = MessageRole.MODEL,
                isStreaming = true
            )
            _messages.value = _messages.value + userMessage + aiMessage

            try {
                val context = getStudentContext()
                val predefinedSyllabus = com.acadmate.core.model.PredefinedSyllabus.bTechCse6thSem.joinToString("\n") { sub ->
                    "${sub.subjectCode}: ${sub.subjectName} - ${sub.description}\nUnits: " + sub.units.joinToString("; ") { it.title }
                }

                val systemPrompt = """
                    You are AcadMate AI, an elite B.Tech Computer Science Engineering Professor and academic tutor. 
                    You specialize in the current B.Tech CSE 6th Semester curriculum which includes:
                    $predefinedSyllabus

                    When answering questions, provide concrete code examples (Java/Python/C++), discuss Time/Space complexity, 
                    and relate concepts to real-world software architecture where applicable.
                    Use the following student context to personalize your answers if relevant:
                    $context
                """.trimIndent()

                val unitContext = if (unit != null && subject != null) {
                    val topics = com.acadmate.core.model.PredefinedSyllabus.bTechCse6thSem
                        .find { it.subjectName == subject }
                        ?.units?.find { it.title == unit }
                        ?.topics?.joinToString()
                    "Focusing on Unit: $unit. Specific topics in this unit: $topics\n"
                } else if (unit != null) {
                    "Focusing on Unit: $unit\n"
                } else ""
                val fullPrompt = if (subject != null) {
                    "$systemPrompt\n\n${unitContext}Subject: $subject\nQuestion: $text"
                } else {
                    "$systemPrompt\n\nQuestion: $text"
                }
                
                generativeModel.generateContentStream(fullPrompt).collect { chunk ->
                    val textChunk = chunk.text
                    if (textChunk != null) {
                        aiText += textChunk
                        updateStreamingMessage(aiMessageId, aiText)
                    }
                }
                
                val finalMessage = ChatMessage(
                    id = aiMessageId,
                    sessionId = currentSessionId,
                    text = aiText,
                    role = MessageRole.MODEL,
                    isStreaming = false
                )
                chatDao.insertMessage(finalMessage)
                _suggestions.value = suggestFollowUps(aiText)
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("firebasevertexai.googleapis.com") == true -> 
                        "Error: The Vertex AI API is not enabled. Please go to the Firebase Console, navigate to 'Vertex AI', and click 'Get Started' to enable the API. This usually takes a few minutes to propagate."
                    e.message?.contains("API_KEY_INVALID") == true -> "Error: Invalid API Key. Please check your local.properties file."
                    e.message?.contains("404") == true -> "Error: Model not found. Check your configuration."
                    else -> "Error: ${e.message}"
                }
                updateStreamingMessage(aiMessageId, errorMessage)
            } finally {
                _isTyping.value = false
            }
        }
    }

    private fun updateStreamingMessage(id: String, text: String) {
        _messages.value = _messages.value.map {
            if (it.id == id) it.copy(text = text) else it
        }
    }

    private fun suggestFollowUps(lastResponse: String): List<String> {
        // Find current subject and suggest next topics
        val currentSubject = messages.value.lastOrNull { it.role == MessageRole.USER }?.text ?: ""
        
        return listOf(
            "Explain the next topic in the unit",
            "Give me a real-world project idea for this",
            "What are the common exam questions from here?"
        )
    }

    fun clearChat() {
        viewModelScope.launch {
            chatDao.deleteSessionMessages(currentSessionId)
            _messages.value = emptyList()
        }
    }
}
