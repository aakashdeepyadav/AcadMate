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
import kotlinx.coroutines.Job
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

    private val _currentMode = MutableStateFlow("TUTOR")
    val currentMode: StateFlow<String> = _currentMode.asStateFlow()

    private val _realSyllabus = MutableStateFlow<List<com.acadmate.core.model.SubjectSyllabus>>(emptyList())
    val realSyllabus: StateFlow<List<com.acadmate.core.model.SubjectSyllabus>> = _realSyllabus.asStateFlow()

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

    private var currentSessionId: String = UUID.randomUUID().toString()
    private var messagesJob: Job? = null

    init {
        loadRealSyllabus()
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            chatDao.getAllSessions().collect {
                _sessions.value = it
            }
        }
    }

    private fun loadRealSyllabus() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                // First try to get subjects assigned to this student specifically
                val studentDoc = firestore.collection("users").document(userId).get().await()
                val department = studentDoc.getString("department")
                
                val snapshot = if (department != null) {
                    firestore.collection("syllabuses")
                        .whereEqualTo("department", department)
                        .get()
                        .await()
                } else {
                    firestore.collection("syllabuses").get().await()
                }
                
                val list = snapshot.toObjects(com.acadmate.core.model.SubjectSyllabus::class.java)
                if (list.isNotEmpty()) {
                    _realSyllabus.value = list
                } else {
                    _realSyllabus.value = com.acadmate.core.model.PredefinedSyllabus.bTechCse6thSem
                }
            } catch (e: Exception) {
                _realSyllabus.value = com.acadmate.core.model.PredefinedSyllabus.bTechCse6thSem
            }
        }
    }

    fun setMode(mode: String) {
        _currentMode.value = mode
        viewModelScope.launch {
            // Wait for sessions to be loaded or check DB
            val sessionList = if (_sessions.value.isEmpty()) {
                chatDao.getAllSessions().first()
            } else {
                _sessions.value
            }

            val latestForMode = sessionList.firstOrNull { it.mode == mode }
            if (latestForMode != null) {
                if (currentSessionId != latestForMode.id) {
                    loadConversationHistory(latestForMode.id)
                }
            } else {
                createNewSession(mode)
            }
        }
    }

    fun createNewSession(mode: String, subject: String? = null, unit: String? = null) {
        viewModelScope.launch {
            val newSession = ChatSession(
                title = if (subject != null) "Study: $subject" else "New $mode Session",
                mode = mode,
                subject = subject,
                unit = unit
            )
            chatDao.insertSession(newSession)
            loadConversationHistory(newSession.id)
        }
    }

    fun loadConversationHistory(sessionId: String) {
        currentSessionId = sessionId
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            val session = chatDao.getSessionById(sessionId)
            _currentSession.value = session
            if (session != null) {
                _currentMode.value = session.mode
            }

            chatDao.getMessagesBySession(sessionId).collect { dbMessages ->
                // Merge with in-memory streaming message to prevent disappearing during generation
                val streamingMessage = _messages.value.find { it.isStreaming }
                if (streamingMessage != null && !dbMessages.any { it.id == streamingMessage.id }) {
                    _messages.value = dbMessages + streamingMessage
                } else {
                    _messages.value = dbMessages
                }
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

        viewModelScope.launch {
            // 1. Ensure we have a valid session and capture its ID deterministically
            val sessionId = if (_currentSession.value == null) {
                val newSessionId = UUID.randomUUID().toString()
                val newSession = ChatSession(
                    id = newSessionId,
                    title = if (subject != null) "Study: $subject" else "New ${_currentMode.value} Session",
                    mode = _currentMode.value,
                    subject = subject,
                    unit = unit
                )
                chatDao.insertSession(newSession)
                loadConversationHistory(newSessionId)
                newSessionId
            } else {
                val currentSess = _currentSession.value!!
                val updatedSession = currentSess.copy(
                    lastUpdated = System.currentTimeMillis(),
                    subject = subject ?: currentSess.subject,
                    unit = unit ?: currentSess.unit,
                    title = if (currentSess.title.startsWith("New")) text.take(20) + "..." else currentSess.title
                )
                chatDao.insertSession(updatedSession)
                _currentSession.value = updatedSession
                currentSess.id
            }

            val userMessage = ChatMessage(
                sessionId = sessionId,
                text = text,
                role = MessageRole.USER
            )

            chatDao.insertMessage(userMessage)
            _suggestions.value = emptyList()
            _isTyping.value = true

            val aiMessageId = UUID.randomUUID().toString()
            var aiText = ""
            
            val aiMessage = ChatMessage(
                id = aiMessageId,
                sessionId = sessionId,
                text = "",
                role = MessageRole.MODEL,
                isStreaming = true
            )
            
            // Add to UI immediately
            _messages.value = _messages.value + aiMessage

            try {
                val studentContext = getStudentContext()
                
                // Fetch the latest syllabus from the database to ensure it's dynamic and not hardcoded
                val latestSyllabusSnapshot = firestore.collection("syllabuses").get().await()
                val currentSyllabusList = latestSyllabusSnapshot.toObjects(com.acadmate.core.model.SubjectSyllabus::class.java)
                    .ifEmpty { com.acadmate.core.model.PredefinedSyllabus.bTechCse6thSem }

                // Determine which subject the student is asking about if not explicitly selected
                val inferredSubject = if (subject == null) {
                    currentSyllabusList.find { text.contains(it.subjectName, ignoreCase = true) || text.contains(it.subjectCode, ignoreCase = true) }?.subjectName
                } else subject

                // Build a high-fidelity syllabus context for the relevant subject(s)
                val relevantSyllabusContext = if (inferredSubject != null) {
                    val sub = currentSyllabusList.find { it.subjectName == inferredSubject }
                    if (sub != null) {
                        "OFFICIAL SYLLABUS FOR ${sub.subjectName} (${sub.subjectCode}):\n" +
                        "Description: ${sub.description}\n" +
                        sub.units.joinToString("\n") { unit -> 
                            "Unit: ${unit.title}\nTopics: ${unit.topics.joinToString(", ")}"
                        }
                    } else "General Syllabus context unavailable."
                } else {
                    "OVERVIEW OF SEMESTER SYLLABUS:\n" + currentSyllabusList.joinToString("\n") { sub ->
                        "${sub.subjectCode}: ${sub.subjectName} - Units: " + sub.units.joinToString("; ") { it.title }
                    }
                }

                val tutorPrompt = """
                    You are AcadMate AI, an elite B.Tech Computer Science Engineering Professor. 
                    Your knowledge is strictly grounded in the following official institutional syllabus retrieved from the database:
                    $relevantSyllabusContext

                    INSTRUCTIONS:
                    1. If the student asks for notes, explain the topics specified in the syllabus for that unit in detail.
                    2. Provide concrete code examples where applicable.
                    3. Discuss technical architecture and real-world relevance.
                    4. Use the following student performance context to personalize your tone:
                    $studentContext
                """.trimIndent()

                val interviewPrompt = """
                    You are AcadMate Technical Interviewer, an elite lead engineer from a Top Tier Big-Tech company (MAANG level).
                    Your goal is to prepare the student for high-stakes modern technical interviews and placements.
                    
                    CRITICAL: Do NOT focus on college syllabus or academic theory unless specifically asked. Focus on industry-standard skills:
                    1. Data Structures & Algorithms (LeetCode style, optimization, complexity)
                    2. System Design (Scalability, Load Balancing, Microservices, Databases)
                    3. Core CS Engineering: OS (Concurrency, Memory), DBMS (Indexing, Transactions), Computer Networks (TCP/IP, HTTP/S)
                    4. Modern Tech Stacks: Android (Jetpack Compose, MVVM), Frontend (React/Next.js), Backend (Spring Boot, Node.js, Go)
                    5. Behavioral: Leadership Principles, STAR method, Soft Skills for placements.
                    
                    When the student mentions a role or company, tailor your mock interview to that specific company's bar.
                    Student Context: $studentContext
                """.trimIndent()

                val plannerPrompt = """
                    You are AcadMate Study Architect. Your goal is to create a hyper-personalized study roadmap for the student.
                    Analyze their upcoming assignments, attendance, and syllabus gaps to prioritize what they should study next.
                    Institutional Syllabus for Reference:
                    $relevantSyllabusContext
                    
                    Student Context: $studentContext
                    
                    When asked for a plan, provide a day-by-day or topic-by-topic schedule that focuses on bridging their "Syllabus Gaps" first.
                """.trimIndent()

                val facultyPlannerPrompt = """
                    You are AcadMate Lesson Architect, a specialized AI consultant for University Professors.
                    Your goal is to help a Professor create a comprehensive, engaging, and time-optimized Lesson Plan for their specific syllabus.
                    
                    Institutional Syllabus Context:
                    $relevantSyllabusContext

                    When a professor asks for a lesson plan or lecture draft:
                    1.  **Learning Objectives**: Define what students should know by the end of the session.
                    2.  **Lecture Breakdown**: Provide a minute-by-minute or topic-by-topic flow.
                    3.  **Active Learning**: Suggest 1-2 interactive activities or demos.
                    4.  **Assessment**: Provide 2-3 formative questions to check student understanding.
                    5.  **Modern Insights**: Link concepts to current industry trends (e.g., if teaching OS, mention Kubernetes).

                    Always be professional, concise, and academically rigorous.
                """.trimIndent()

                val currentRole = onboardingDataStore.selectedRole.first()
                val systemPrompt = when(_currentMode.value) {
                    "INTERVIEW" -> interviewPrompt
                    "PLANNER" -> if (currentRole == UserRole.FACULTY) facultyPlannerPrompt else plannerPrompt
                    else -> tutorPrompt
                }

                val unitContext = if (unit != null && subject != null) {
                    val topics = currentSyllabusList
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
                    sessionId = sessionId,
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
