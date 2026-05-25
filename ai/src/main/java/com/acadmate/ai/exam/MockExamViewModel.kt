package com.acadmate.ai.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class Question(
    val id: String = "",
    val text: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = "",
    val topic: String = "General",
    var selectedIndex: Int? = null
)

data class ExamResult(
    val score: Int = 0,
    val total: Int = 0,
    val subject: String = "",
    val strongTopics: List<String> = emptyList(),
    val weakTopics: List<String> = emptyList(),
    val questions: List<Question> = emptyList()
)

sealed class ExamUiState {
    object Setup : ExamUiState()
    object Loading : ExamUiState()
    data class Ongoing(val questions: List<Question>, val currentIndex: Int) : ExamUiState()
    data class Finished(val result: ExamResult) : ExamUiState()
    data class Error(val message: String) : ExamUiState()
}

@Serializable
data class AiQuestion(
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val topic: String = "General"
)

@HiltViewModel
class MockExamViewModel @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val onboardingDataStore: com.acadmate.core.datastore.OnboardingDataStore
) : ViewModel() {

    private val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow<ExamUiState>(ExamUiState.Setup)
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private val _availableSubjects = MutableStateFlow<List<com.acadmate.core.model.SubjectSyllabus>>(emptyList())
    val availableSubjects: StateFlow<List<com.acadmate.core.model.SubjectSyllabus>> = _availableSubjects.asStateFlow()

    private val _availableQuizzes = MutableStateFlow<List<PublishedQuiz>>(emptyList())
    val availableQuizzes: StateFlow<List<PublishedQuiz>> = _availableQuizzes.asStateFlow()

    private val _timeLeft = MutableStateFlow(0)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    val userRole: StateFlow<com.acadmate.core.model.UserRole?> = onboardingDataStore.selectedRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUserId: String? get() = auth.currentUser?.uid

    private var activeSubject: String = ""
    private var activeQuizId: String? = null
    private var activeFacultyId: String? = null
    private var timerJob: Job? = null

    init {
        loadAvailableSubjects()
        loadPublishedQuizzes()
    }

    private fun loadPublishedQuizzes() {
        viewModelScope.launch {
            try {
                firestore.collection("published_quizzes")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        val quizzes = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(PublishedQuiz::class.java)?.copy(id = doc.id)
                        } ?: emptyList()
                        _availableQuizzes.value = quizzes
                    }
            } catch (e: Exception) {}
        }
    }

    private fun loadAvailableSubjects() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("syllabuses").get().await()
                val list = snapshot.toObjects(com.acadmate.core.model.SubjectSyllabus::class.java)
                _availableSubjects.value = if (list.isNotEmpty()) list 
                                          else com.acadmate.core.model.PredefinedSyllabus.bTechCse6thSem
            } catch (e: Exception) {
                _availableSubjects.value = com.acadmate.core.model.PredefinedSyllabus.bTechCse6thSem
            }
        }
    }

    fun publishQuiz(subject: String, difficulty: String, questions: List<Question>) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDoc = firestore.collection("users").document(userId).get().await()
                val facultyName = userDoc.getString("name") ?: "Professor"

                val quizData = PublishedQuiz(
                    title = "$subject Quiz ($difficulty)",
                    subject = subject,
                    facultyName = facultyName,
                    facultyId = userId,
                    questions = questions,
                    createdAt = System.currentTimeMillis()
                )
                
                firestore.collection("published_quizzes").add(quizData).await()
                _uiState.value = ExamUiState.Setup // Go back to setup after publishing
            } catch (e: Exception) {
                _uiState.value = ExamUiState.Error("Failed to publish: ${e.message}")
            }
        }
    }

    fun startPublishedQuiz(quiz: PublishedQuiz) {
        activeSubject = quiz.subject
        activeQuizId = quiz.id
        activeFacultyId = quiz.facultyId
        _uiState.value = ExamUiState.Ongoing(quiz.questions, 0)
        startTimer(20 * 60) // Default 20 mins for published quizzes
    }

    fun generateExam(subject: String, difficulty: String, count: Int, timeLimit: Int?, shouldPublish: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = ExamUiState.Loading
            
            try {
                // Fetch the latest syllabus directly from Firestore for this subject to ensure dynamic content
                val snapshot = firestore.collection("syllabuses")
                    .whereEqualTo("subjectName", subject)
                    .get()
                    .await()
                
                val matchedSyllabus = snapshot.toObjects(com.acadmate.core.model.SubjectSyllabus::class.java).firstOrNull()
                    ?: _availableSubjects.value.find { it.subjectName.equals(subject, ignoreCase = true) }
                
                val syllabusContext = matchedSyllabus?.let {
                    "Use the following topics from the official database syllabus for this subject:\n" +
                    it.units.joinToString("\n") { unit -> "- ${unit.title}: ${unit.topics.joinToString()}" }
                } ?: ""

                val prompt = """
                    Generate a practice exam for the subject '$subject' with difficulty '$difficulty'.
                    DATABASE SYLLABUS CONTEXT:
                    $syllabusContext
                    
                    Provide exactly $count multiple choice questions based on the topics above.
                    Return the result as a JSON array of objects with these fields:
                    - text: the question text
                    - options: a list of 4 possible answers
                    - correctIndex: the index of the correct answer (0-3)
                    - explanation: a brief explanation why the answer is correct
                    - topic: the specific topic or unit name from the syllabus this question relates to
                    
                    Respond only with the JSON array.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val responseText = response.text?.trim() ?: throw Exception("Empty response from AI")
                
                // Robust JSON extraction: Find the first '[' and last ']'
                val startIndex = responseText.indexOf("[")
                val endIndex = responseText.lastIndexOf("]")
                
                if (startIndex == -1 || endIndex == -1 || endIndex < startIndex) {
                    throw Exception("Could not find valid JSON array in AI response")
                }
                
                val jsonString = responseText.substring(startIndex, endIndex + 1)

                val aiQuestions = json.decodeFromString<List<AiQuestion>>(jsonString)
                val questions = aiQuestions.mapIndexed { i, aq ->
                    Question(
                        id = "${System.currentTimeMillis()}_$i",
                        text = aq.text,
                        options = aq.options,
                        correctIndex = aq.correctIndex,
                        explanation = aq.explanation,
                        topic = aq.topic
                    )
                }

                if (shouldPublish) {
                    publishQuiz(subject, difficulty, questions)
                } else {
                    activeSubject = subject
                    activeQuizId = null
                    activeFacultyId = null
                    _uiState.value = ExamUiState.Ongoing(questions, 0)
                    timeLimit?.let {
                        startTimer(it * 60)
                    }
                }
            } catch (e: Exception) {
                // ... fallback logic ...
                _uiState.value = ExamUiState.Error("Generation failed: ${e.message}")
            }
        }
    }

    private fun startTimer(seconds: Int) {
        _timeLeft.value = seconds
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0) {
                delay(1000)
                _timeLeft.value -= 1
            }
            submitExam()
        }
    }

    fun selectOption(questionIndex: Int, optionIndex: Int) {
        val currentState = _uiState.value
        if (currentState is ExamUiState.Ongoing) {
            val updatedQuestions = currentState.questions.toMutableList()
            updatedQuestions[questionIndex] = updatedQuestions[questionIndex].copy(selectedIndex = optionIndex)
            _uiState.value = currentState.copy(questions = updatedQuestions)
        }
    }

    fun nextQuestion() {
        val currentState = _uiState.value
        if (currentState is ExamUiState.Ongoing && currentState.currentIndex < currentState.questions.size - 1) {
            _uiState.value = currentState.copy(currentIndex = currentState.currentIndex + 1)
        }
    }

    fun previousQuestion() {
        val currentState = _uiState.value
        if (currentState is ExamUiState.Ongoing && currentState.currentIndex > 0) {
            _uiState.value = currentState.copy(currentIndex = currentState.currentIndex - 1)
        }
    }

    fun submitExam() {
        timerJob?.cancel()
        val currentState = _uiState.value
        if (currentState is ExamUiState.Ongoing) {
            val questions = currentState.questions
            val score = questions.count { it.selectedIndex == it.correctIndex }
            
            val strongTopics = questions.filter { it.selectedIndex == it.correctIndex }
                .map { it.topic }.distinct()
            val weakTopics = questions.filter { it.selectedIndex != it.correctIndex }
                .map { it.topic }.distinct()
            
            val result = ExamResult(
                score = score,
                total = questions.size,
                subject = activeSubject,
                strongTopics = strongTopics,
                weakTopics = weakTopics,
                questions = questions
            )
            
            saveResultToFirestore(result, activeQuizId, activeFacultyId)
            _uiState.value = ExamUiState.Finished(result)
        }
    }

    private fun saveResultToFirestore(result: ExamResult, quizId: String?, facultyId: String?) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                val studentName = userDoc.getString("name") ?: "Student"

                val examData = hashMapOf(
                    "userId" to userId,
                    "studentName" to studentName,
                    "subject" to result.subject,
                    "quizId" to (quizId ?: ""),
                    "facultyId" to (facultyId ?: ""),
                    "score" to result.score,
                    "total" to result.total,
                    "percentage" to (result.score.toFloat() / result.total) * 100,
                    "timestamp" to System.currentTimeMillis(),
                    "strongTopics" to result.strongTopics,
                    "weakTopics" to result.weakTopics
                )
                firestore.collection("exam_results").add(examData)
            } catch (e: Exception) { }
        }
    }

    fun reset() {
        _uiState.value = ExamUiState.Setup
        _timeLeft.value = 0
        timerJob?.cancel()
    }
}

data class PublishedQuiz(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val facultyName: String = "",
    val facultyId: String = "",
    val questions: List<Question> = emptyList(),
    val createdAt: Long = 0
)

