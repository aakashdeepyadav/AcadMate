package com.acadmate.dashboard.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.db.ResultsRepository
import com.acadmate.core.db.SemesterResultEntity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val _isSyncing = MutableStateFlow(false)

    val uiState: StateFlow<ResultsUiState> = combine(
        repository.getResultsForUser(auth.currentUser?.uid ?: ""),
        _isSyncing
    ) { results, syncing ->
        val cgpa = if (results.isNotEmpty()) {
            results.map { it.sgpa * it.credits }.sum() / results.sumOf { it.credits }.coerceAtLeast(1)
        } else {
            0f
        }
        
        ResultsUiState(
            results = results,
            currentCgpa = cgpa,
            isLoading = syncing && results.isEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResultsUiState())

    init {
        syncData()
    }

    private fun syncData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                _isSyncing.value = true
                repository.syncResults(userId)
                _isSyncing.value = false
            }
        }
    }
}
