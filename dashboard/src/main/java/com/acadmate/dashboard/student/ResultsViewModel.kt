package com.acadmate.dashboard.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.db.ResultsRepository
import com.acadmate.core.db.SemesterResultEntity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultsUiState(
    val results: List<SemesterResultEntity> = emptyList(),
    val currentCgpa: Float = 0f,
    val isLoading: Boolean = true
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val repository: ResultsRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    init {
        loadResults()
    }

    private fun loadResults() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val userResults = repository.getResultsForUser(userId)
                
                val cgpa = if (userResults.isNotEmpty()) {
                    userResults.map { it.sgpa * it.credits }.sum() / userResults.sumOf { it.credits }.coerceAtLeast(1)
                } else {
                    0f
                }

                _uiState.value = ResultsUiState(
                    results = userResults,
                    currentCgpa = cgpa,
                    isLoading = false
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
