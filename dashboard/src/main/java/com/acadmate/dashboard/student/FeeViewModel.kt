package com.acadmate.dashboard.student

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
class FeeViewModel @Inject constructor() : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _fees = MutableStateFlow<List<FeeItem>>(emptyList())
    val fees: StateFlow<List<FeeItem>> = _fees

    private val _totalOutstanding = MutableStateFlow("₹0")
    val totalOutstanding: StateFlow<String> = _totalOutstanding

    init {
        loadFees()
    }

    fun loadFees() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("student_fees")
                    .whereEqualTo("studentId", userId)
                    .get().await()
                
                val items = snapshot.documents.map { doc ->
                    FeeItem(
                        title = doc.getString("title") ?: "",
                        desc = doc.getString("desc") ?: "",
                        amount = doc.getString("amount") ?: "₹0",
                        status = doc.getString("status") ?: "PENDING"
                    )
                }.ifEmpty { 
                    listOf(
                        FeeItem("Tuition Fee", "Semester 6", "₹85,000", "PAID"),
                        FeeItem("Hostel Fee", "Annual 2023-24", "₹45,000", "PENDING")
                    )
                }
                
                _fees.value = items
                
                val outstanding = items.filter { it.status == "PENDING" }
                    .sumOf { it.amount.replace("₹", "").replace(",", "").toIntOrNull() ?: 0 }
                _totalOutstanding.value = "₹${java.text.DecimalFormat("#,###").format(outstanding)}"
            } catch (e: Exception) {}
        }
    }
}
