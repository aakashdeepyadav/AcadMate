package com.acadmate.ai.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val topic: String = "General",
    var selectedIndex: Int? = null
)

data class ExamResult(
    val score: Int,
    val total: Int,
    val strongTopics: List<String>,
    val weakTopics: List<String>,
    val questions: List<Question>
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
    private val generativeModel: GenerativeModel
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow<ExamUiState>(ExamUiState.Setup)
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private val _timeLeft = MutableStateFlow(0)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private var timerJob: Job? = null

    fun generateExam(subject: String, difficulty: String, count: Int, timeLimit: Int?) {
        viewModelScope.launch {
            _uiState.value = ExamUiState.Loading
            
            try {
                val matchedSyllabus = com.acadmate.core.model.PredefinedSyllabus.bTechCse6thSem
                    .find { it.subjectName.equals(subject, ignoreCase = true) }
                
                val syllabusContext = matchedSyllabus?.let {
                    "Use the following topics from the official syllabus for this subject:\n" +
                    it.units.joinToString("\n") { unit -> "- ${unit.title}: ${unit.topics.joinToString()}" }
                } ?: ""

                val prompt = """
                    Generate a practice exam for the subject '$subject' with difficulty '$difficulty'.
                    $syllabusContext
                    Provide exactly $count multiple choice questions.
                    Return the result as a JSON array of objects with these fields:
                    - text: the question text
                    - options: a list of 4 possible answers
                    - correctIndex: the index of the correct answer (0-3)
                    - explanation: a brief explanation why the answer is correct
                    - topic: the specific topic or unit name from the syllabus this question relates to
                    
                    Respond only with the JSON.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val responseText = response.text?.trim() ?: throw Exception("Empty response from AI")
                
                // Extract JSON if AI wrapped it in markdown code blocks
                val jsonString = if (responseText.startsWith("```json")) {
                    responseText.substringAfter("```json").substringBeforeLast("```").trim()
                } else if (responseText.startsWith("```")) {
                    responseText.substringAfter("```").substringBeforeLast("```").trim()
                } else {
                    responseText
                }

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

                _uiState.value = ExamUiState.Ongoing(questions, 0)
                
                timeLimit?.let {
                    startTimer(it * 60)
                }
            } catch (e: Exception) {
                // Check if it's the Vertex AI API enablement error
                if (e.message?.contains("firebasevertexai.googleapis.com") == true) {
                    _uiState.value = ExamUiState.Error(
                        "The Vertex AI API is not enabled. Please enable it in the Firebase Console under 'Vertex AI'."
                    )
                    return@launch
                }

                // Fallback to local generation if AI fails
                val questions = List(count) { i ->
                    Question(
                        id = "$i",
                        text = "Sample Question $i for $subject ($difficulty)?",
                        options = listOf("Option A", "Option B", "Option C", "Option D"),
                        correctIndex = (0..3).random(),
                        explanation = "AI generation failed: ${e.message}. This is a fallback question."
                    )
                }
                _uiState.value = ExamUiState.Ongoing(questions, 0)
                
                timeLimit?.let {
                    startTimer(it * 60)
                }
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
                strongTopics = strongTopics,
                weakTopics = weakTopics,
                questions = questions
            )
            
            saveResultToFirestore(result)
            _uiState.value = ExamUiState.Finished(result)
        }
    }

    private fun saveResultToFirestore(result: ExamResult) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        val examData = hashMapOf(
            "userId" to userId,
            "score" to result.score,
            "total" to result.total,
            "percentage" to (result.score.toFloat() / result.total) * 100,
            "timestamp" to System.currentTimeMillis(),
            "strongTopics" to result.strongTopics,
            "weakTopics" to result.weakTopics,
            "detailedQuestions" to result.questions.map { 
                hashMapOf(
                    "text" to it.text,
                    "selectedIndex" to it.selectedIndex,
                    "correctIndex" to it.correctIndex,
                    "isCorrect" to (it.selectedIndex == it.correctIndex)
                )
            }
        )
        
        viewModelScope.launch {
            try {
                firestore.collection("exam_results").add(examData)
            } catch (e: Exception) {
                // Silently fail or log
            }
        }
    }

    fun reset() {
        _uiState.value = ExamUiState.Setup
        _timeLeft.value = 0
        timerJob?.cancel()
    }
}
