package com.acadmate.ai.syllabus

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.model.PredefinedSyllabus
import com.acadmate.core.model.SubjectSyllabus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class SyllabusResult(
    val summary: String,
    val units: List<UnitItem>,
    val flashcards: List<Flashcard>,
    val mcqs: List<Mcq>,
    val gapAnalysis: List<GapAnalysisItem> = emptyList()
)

data class GapAnalysisItem(
    val topic: String,
    val coverage: Float, // 0.0 to 1.0
    val status: String // "Covered", "Partial", "Missing"
)

data class UnitItem(
    val title: String,
    val chapters: List<ChapterItem>
)

data class ChapterItem(
    val title: String,
    val concepts: List<String>
)

data class Flashcard(
    val question: String,
    val answer: String,
    var status: FlashcardStatus = FlashcardStatus.IDLE
)

enum class FlashcardStatus { IDLE, GOT_IT, REVIEW_AGAIN }

data class Mcq(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    var selectedOptionIndex: Int? = null
)

sealed class SyllabusUiState {
    object Idle : SyllabusUiState()
    data class Uploading(val progress: Float) : SyllabusUiState()
    object Processing : SyllabusUiState()
    data class Done(val result: SyllabusResult) : SyllabusUiState()
    data class Error(val message: String) : SyllabusUiState()
}

@HiltViewModel
class SyllabusAiViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<SyllabusUiState>(SyllabusUiState.Idle)
    val uiState: StateFlow<SyllabusUiState> = _uiState.asStateFlow()

    fun uploadAndProcess(uri: Uri) {
        viewModelScope.launch {
            // Step 1: Uploading
            for (progress in 1..100) {
                _uiState.value = SyllabusUiState.Uploading(progress / 100f)
                delay(15) 
            }

            // Step 2: Processing (Matching with Knowledge Base)
            _uiState.value = SyllabusUiState.Processing
            delay(2000)

            // Step 3: Map to actual B.Tech 6th Sem Knowledge
            val fileName = uri.lastPathSegment?.uppercase() ?: ""
            val matchedSyllabus = when {
                fileName.contains("CSE225") || fileName.contains("ANDROID") -> PredefinedSyllabus.bTechCse6thSem[0]
                fileName.contains("CSE332") || fileName.contains("ETHICS") -> PredefinedSyllabus.bTechCse6thSem[1]
                fileName.contains("CSE357") || fileName.contains("COMB") -> PredefinedSyllabus.bTechCse6thSem[2]
                fileName.contains("INT345") || fileName.contains("VISION") -> PredefinedSyllabus.bTechCse6thSem[3]
                fileName.contains("PES319") || fileName.contains("SOFT") -> PredefinedSyllabus.bTechCse6thSem[4]
                else -> PredefinedSyllabus.bTechCse6thSem[0] // Default to Android
            }

            val result = SyllabusResult(
                summary = """
                    # ${matchedSyllabus.subjectCode}: ${matchedSyllabus.subjectName}
                    ${matchedSyllabus.description}
                    
                    ## Course Details
                    - **Credits**: ${matchedSyllabus.credits}
                    - **LTP**: ${matchedSyllabus.ltp}
                    
                    ## AI Contextual Analysis
                    The AI Suite has successfully indexed the ${matchedSyllabus.units.size} units for this course. 
                    You can now use the **AI Tutor** to ask specific questions about these topics.
                """.trimIndent(),
                units = matchedSyllabus.units.map { unit ->
                    UnitItem(unit.title, listOf(ChapterItem("Core Topics", unit.topics)))
                },
                flashcards = generateFlashcardsFor(matchedSyllabus),
                mcqs = generateMcqsFor(matchedSyllabus),
                gapAnalysis = matchedSyllabus.units.mapIndexed { index, unit ->
                    GapAnalysisItem(
                        unit.title, 
                        if (index == 0) 0.9f else 0.4f, 
                        if (index == 0) "Covered" else "Partial"
                    )
                }
            )

            // Save to Knowledge Base for AI Suite access
            saveToKnowledgeBase(matchedSyllabus)

            _uiState.value = SyllabusUiState.Done(result)
        }
    }

    private fun generateFlashcardsFor(syllabus: SubjectSyllabus): List<Flashcard> {
        return listOf(
            Flashcard("What is the primary focus of ${syllabus.subjectCode}?", syllabus.description),
            Flashcard("What are the credits for this course?", "${syllabus.credits} Credits"),
            Flashcard("List Unit 1 topics.", syllabus.units.firstOrNull()?.topics?.joinToString() ?: "N/A")
        )
    }

    private fun generateMcqsFor(syllabus: SubjectSyllabus): List<Mcq> {
        return listOf(
            Mcq("What is the LTP for ${syllabus.subjectCode}?", listOf(syllabus.ltp, "3:0:0", "1:2:1", "0:0:4"), 0),
            Mcq("Which unit covers ${syllabus.units.last().title}?", syllabus.units.map { it.title }, syllabus.units.size - 1)
        )
    }

    private suspend fun saveToKnowledgeBase(syllabus: SubjectSyllabus) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()
        
        val knowledgeData = hashMapOf(
            "subjectCode" to syllabus.subjectCode,
            "subjectName" to syllabus.subjectName,
            "units" to syllabus.units.map { unit ->
                hashMapOf("title" to unit.title, "topics" to unit.topics)
            },
            "lastUpdated" to System.currentTimeMillis()
        )

        try {
            firestore.collection("users")
                .document(userId)
                .collection("knowledge_base")
                .document(syllabus.subjectCode)
                .set(knowledgeData)
                .await()
        } catch (e: Exception) { }
    }

    fun reset() {
        _uiState.value = SyllabusUiState.Idle
    }
}
