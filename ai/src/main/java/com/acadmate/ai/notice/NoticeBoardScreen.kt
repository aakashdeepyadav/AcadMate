package com.acadmate.ai.notice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class Notice(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeBoardScreen(onBackClick: () -> Unit) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var notices by remember { mutableStateOf<List<Notice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = firestore.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val fetchedNotices = snapshot.documents.map { doc ->
                val timestamp = doc.getLong("timestamp") ?: 0L
                val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
                Notice(
                    id = doc.id,
                    title = doc.getString("title") ?: "No Title",
                    date = dateStr,
                    content = doc.getString("content") ?: "",
                    timestamp = timestamp
                )
            }
            notices = fetchedNotices
        } catch (e: Exception) {
            // Error handling
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notice Board", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notices.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No notices posted yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notices) { notice ->
                    AcadMateCard(variant = CardVariant.Elevated) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(notice.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(notice.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(notice.content, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
