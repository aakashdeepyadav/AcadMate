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

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val status: String = "Available",
    val dueDate: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor() : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books

    private val _myBooks = MutableStateFlow<List<Book>>(emptyList())
    val myBooks: StateFlow<List<Book>> = _myBooks

    init {
        loadBooks()
    }

    fun loadBooks() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("library_books").get().await()
                _books.value = snapshot.documents.map { doc ->
                    Book(
                        id = doc.id,
                        title = doc.getString("title") ?: "Unknown",
                        author = doc.getString("author") ?: "Unknown",
                        status = doc.getString("status") ?: "Available"
                    )
                }.ifEmpty { 
                    listOf(
                        Book("1", "Clean Code", "Robert C. Martin", "Available"),
                        Book("2", "The Pragmatic Programmer", "Andrew Hunt", "Borrowed"),
                        Book("3", "Kotlin in Action", "Dmitry Jemerov", "Available")
                    )
                }
                
                val userId = auth.currentUser?.uid ?: return@launch
                val mySnapshot = firestore.collection("issued_books")
                    .whereEqualTo("userId", userId)
                    .get().await()
                
                _myBooks.value = mySnapshot.documents.map { doc ->
                    Book(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        author = doc.getString("author") ?: "",
                        status = "Issued",
                        dueDate = doc.getString("dueDate")
                    )
                }
            } catch (e: Exception) {}
        }
    }
}
