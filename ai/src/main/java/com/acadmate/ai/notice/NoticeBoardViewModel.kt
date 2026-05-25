package com.acadmate.ai.notice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadmate.core.db.NoticeEntity
import com.acadmate.core.db.NoticeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoticeBoardViewModel @Inject constructor(
    private val noticeRepository: NoticeRepository
) : ViewModel() {

    val notices: StateFlow<List<NoticeEntity>> = noticeRepository.getAllNotices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val onboardingDataStore = com.acadmate.core.datastore.OnboardingDataStore(
        com.google.firebase.auth.FirebaseAuth.getInstance().app.applicationContext as android.content.Context
    )

    init {
        syncNotices()
    }

    fun postNotice(title: String, content: String, attachmentUrl: String? = null) {
        viewModelScope.launch {
            _isPosting.value = true
            try {
                noticeRepository.postNotice(title, content, attachmentUrl)
            } catch (e: Exception) {
            } finally {
                _isPosting.value = false
            }
        }
    }

    private fun syncNotices() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                noticeRepository.syncNotices()
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}
