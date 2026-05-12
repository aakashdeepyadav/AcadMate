package com.acadmate.core.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val repository: SecurityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityStatus())
    val uiState = _uiState.asStateFlow()

    fun runSecurityCheck() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, checks = emptyList())
            
            repository.runSecurityChecks().collect { result ->
                val currentChecks = _uiState.value.checks.toMutableList()
                currentChecks.add(result)
                
                val isSafe = currentChecks.all { 
                    it is SecurityCheckResult.AllClear 
                }
                
                _uiState.value = _uiState.value.copy(
                    checks = currentChecks,
                    isSafe = isSafe
                )
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
