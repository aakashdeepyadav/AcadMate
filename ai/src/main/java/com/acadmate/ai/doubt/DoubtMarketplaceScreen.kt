package com.acadmate.ai.doubt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.components.MeshBackground
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

data class Doubt(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val authorName: String = "",
    val authorId: String = "",
    val replies: Int = 0,
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubtMarketplaceScreen(onBackClick: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    
    var doubts by remember { mutableStateOf<List<Doubt>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun loadDoubts() {
        scope.launch {
            isLoading = true
            try {
                val snapshot = firestore.collection("doubts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                doubts = snapshot.toObjects(Doubt::class.java)
            } catch (e: Exception) { } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadDoubts() }

    if (showAddDialog) {
        AddDoubtDialog(
            onDismiss = { showAddDialog = false },
            onPost = { title, content ->
                scope.launch {
                    try {
                        val userId = auth.currentUser?.uid ?: return@launch
                        val userName = auth.currentUser?.displayName ?: "Student"
                        val doubt = Doubt(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            content = content,
                            authorName = userName,
                            authorId = userId,
                            timestamp = System.currentTimeMillis()
                        )
                        firestore.collection("doubts").document(doubt.id).set(doubt).await()
                        showAddDialog = false
                        loadDoubts()
                    } catch (e: Exception) { }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doubt Marketplace", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post Doubt")
            }
        }
    ) { padding ->
        MeshBackground {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (doubts.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("No doubts posted yet. Be the first!", color = Color.Gray)
                            }
                        }
                    }
                    items(doubts) { doubt ->
                        DoubtCard(doubt)
                    }
                }
            }
        }
    }
}

@Composable
fun DoubtCard(doubt: Doubt) {
    AcadMateCard(variant = CardVariant.Elevated) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(doubt.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(doubt.content, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("By ${doubt.authorName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "${doubt.replies} replies", 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AddDoubtDialog(
    onDismiss: () -> Unit,
    onPost: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        AcadMateCard(
            variant = CardVariant.Elevated,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentPadding = 20.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Ask a Question", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                AcadMateTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Subject/Topic Title",
                    placeholder = "e.g. Trouble with Recycler View"
                )
                
                AcadMateTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = "Detailed Description",
                    placeholder = "Explain what you're stuck on...",
                    modifier = Modifier.height(120.dp),
                    singleLine = false
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    AcadMateButton(
                        text = "Post",
                        onClick = { onPost(title, content) },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank() && content.isNotBlank()
                    )
                }
            }
        }
    }
}
