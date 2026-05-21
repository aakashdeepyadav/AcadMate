package com.acadmate.ai.lecture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class LectureUiState {
    object Idle : LectureUiState()
    object Recording : LectureUiState()
    object Processing : LectureUiState()
    data class Success(val notes: String) : LectureUiState()
    data class Error(val message: String) : LectureUiState()
}

@HiltViewModel
class LectureNotesViewModel @Inject constructor(
    private val generativeModel: GenerativeModel
) : ViewModel() {

    private val _uiState = MutableStateFlow<LectureUiState>(LectureUiState.Idle)
    val uiState: StateFlow<LectureUiState> = _uiState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _availableSubjects = MutableStateFlow<List<com.acadmate.core.model.SubjectSyllabus>>(emptyList())
    val availableSubjects: StateFlow<List<com.acadmate.core.model.SubjectSyllabus>> = _availableSubjects.asStateFlow()

    private var audioRecord: android.media.AudioRecord? = null
    private var recordingJob: kotlinx.coroutines.Job? = null
    private val transcriptBuilder = StringBuilder()

    private val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    init {
        loadAvailableSubjects()
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

    @android.annotation.SuppressLint("MissingPermission")
    fun startRecording() {
        _uiState.value = LectureUiState.Recording
        _isRecording.value = true
        transcriptBuilder.clear()
        
        // ... rest of recording logic remains same
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val sampleRate = 44100
            val bufferSize = android.media.AudioRecord.getMinBufferSize(
                sampleRate,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioRecord = android.media.AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                sampleRate,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val buffer = ShortArray(bufferSize)
            audioRecord?.startRecording()

            while (_isRecording.value) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    simulateSpeechToText(buffer, read)
                }
            }
            
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }
    }

    private fun simulateSpeechToText(buffer: ShortArray, length: Int) {
        if (transcriptBuilder.isEmpty()) {
            transcriptBuilder.append("Lecture recording started. Analyzing technical discussion on system architecture and algorithms...")
        }
    }

    fun saveNotes(notes: String, subjectId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val noteRecord = hashMapOf(
                    "userId" to userId,
                    "subjectId" to subjectId,
                    "content" to notes,
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("lecture_notes").add(noteRecord).await()
                _uiState.value = LectureUiState.Idle // Go back to idle after saving
            } catch (e: Exception) { }
        }
    }

    fun stopRecordingAndProcess(subjectName: String = "Current Lecture", unitName: String? = null) {
        _isRecording.value = false
        viewModelScope.launch {
            _uiState.value = LectureUiState.Processing
            
            try {
                val transcript = transcriptBuilder.toString().ifBlank { 
                    "Exploring core concepts and theoretical foundations of $subjectName${if (unitName != null) " - $unitName" else ""}." 
                }

                val matchedSyllabus = _availableSubjects.value
                    .find { it.subjectName.equals(subjectName, ignoreCase = true) }
                
                val syllabusContext = matchedSyllabus?.let {
                    val unitInfo = if (unitName != null) {
                        it.units.find { u -> u.title.equals(unitName, ignoreCase = true) }?.let { u ->
                            "Focus on Unit: ${u.title}. Topics: ${u.topics.joinToString()}"
                        }
                    } else {
                        "General syllabus context: " + it.units.joinToString { u -> u.title }
                    }
                    "\nRelevant Syllabus Context: $unitInfo"
                } ?: ""

                val prompt = """
                    You are an elite academic transcriptionist and a B.Tech Computer Science Engineering Professor. 
                    Analyze the following lecture transcript for the subject '$subjectName' and generate high-fidelity, structured study notes.
                    $syllabusContext
                    
                    Transcript: "$transcript"

                    Structure requirements:
                    # [Topic Name]
                    ## Summary
                    [Concise 3-sentence overview]
                    
                    ## Key Takeaways
                    - [Critical algorithms, protocols, or concepts]
                    
                    ## Technical Details
                    [Architecture or Code Snippets if relevant]
                    
                    ## Exam Insight
                    [Predict 2 likely exam questions based on this unit]
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: "AI Synthesis failed."
                
                _uiState.value = LectureUiState.Success(text)
            } catch (e: Exception) {
                _uiState.value = LectureUiState.Error(e.message ?: "AI Transcription failed")
            }
        }
    }

    fun reset() {
        _uiState.value = LectureUiState.Idle
    }
}
