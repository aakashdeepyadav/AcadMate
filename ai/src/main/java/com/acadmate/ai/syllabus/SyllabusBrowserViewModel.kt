package com.acadmate.ai.syllabus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.model.SubjectSyllabus
import com.acadmate.core.db.UserRepository
import com.acadmate.core.db.SyllabusRepository
import com.acadmate.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BrowserUiState {
    object Loading : BrowserUiState()
    data class Success(val syllabuses: List<SubjectSyllabus>) : BrowserUiState()
    data class Error(val message: String) : BrowserUiState()
}

@HiltViewModel
class SyllabusBrowserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val syllabusRepository: SyllabusRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val uiState: StateFlow<BrowserUiState> = combine(
        syllabusRepository.getAllSyllabuses(),
        userRepository.getCurrentUser(),
        _isLoading
    ) { syllabuses, user, loading ->
        if (loading && syllabuses.isEmpty()) {
            BrowserUiState.Loading
        } else {
            var list = syllabuses
            if (user?.role == UserRole.FACULTY) {
                // Filter logic for faculty could be moved to repository if needed
                // For now, keep it here or simplify
            }
            BrowserUiState.Success(list)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowserUiState.Loading)
    
    init {
        syncData()
    }
    
    fun syncData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                syllabusRepository.syncSyllabuses()
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}
