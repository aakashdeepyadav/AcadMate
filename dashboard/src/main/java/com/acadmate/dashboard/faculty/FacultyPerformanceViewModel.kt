package com.acadmate.dashboard.faculty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class FacultyPerformanceViewModel @Inject constructor() : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _atRiskStudents = MutableStateFlow<List<RiskStudent>>(emptyList())
    val atRiskStudents: StateFlow<List<RiskStudent>> = _atRiskStudents

    init {
        calculateRiskStudents()
    }

    private fun calculateRiskStudents() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // In a real app, this would query attendance & grades
                // For now, we simulate finding students with < 75% attendance for this faculty's subjects
                val attendanceSnapshot = firestore.collection("attendance")
                    .whereEqualTo("facultyId", userId)
                    .get().await()
                
                // Logic to group by student and calculate percentage...
                // Mocking the result based on actual user query
                val students = firestore.collection("users")
                    .whereEqualTo("role", "STUDENT")
                    .limit(5)
                    .get().await()
                
                _atRiskStudents.value = students.documents.mapIndexed { index, doc ->
                    val name = doc.getString("name") ?: "Student"
                    val attendance = (60 + index * 5).toFloat() // Mocked percentage
                    RiskStudent(
                        id = doc.id,
                        name = name,
                        attendance = attendance,
                        remark = if (attendance < 75) "Low attendance alert" else "Borderline attendance"
                    )
                }.filter { it.attendance < 80 }
            } catch (e: Exception) {}
        }
    }
}
