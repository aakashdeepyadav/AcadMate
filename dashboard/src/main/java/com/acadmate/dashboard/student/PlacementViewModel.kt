package com.acadmate.dashboard.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PlacementViewModel @Inject constructor() : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _opportunities = MutableStateFlow<List<JobOpportunity>>(emptyList())
    val opportunities: StateFlow<List<JobOpportunity>> = _opportunities

    init {
        loadOpportunities()
    }

    fun loadOpportunities() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("placements")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()
                
                _opportunities.value = snapshot.documents.map { doc ->
                    JobOpportunity(
                        id = doc.id,
                        company = doc.getString("company") ?: "",
                        title = doc.getString("title") ?: "",
                        type = doc.getString("type") ?: "Job",
                        location = doc.getString("location") ?: "",
                        packageInfo = doc.getString("packageInfo") ?: "",
                        status = doc.getString("status") ?: "Active"
                    )
                }.ifEmpty { 
                    listOf(
                        JobOpportunity("1", "Google", "Software Engineer", "Job", "Mountain View, CA", "₹45 LPA", "Active"),
                        JobOpportunity("2", "Microsoft", "UX Design Intern", "Internship", "Hyderabad, India", "₹80k /mo", "Active")
                    )
                }
            } catch (e: Exception) {}
        }
    }
}
