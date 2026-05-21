package com.acadmate.ai.doubt

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.LocalSpacing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
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

data class Reply(
    val id: String = "",
    val content: String = "",
    val authorName: String = "",
    val authorId: String = "",
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
    var selectedDoubt by remember { mutableStateOf<Doubt?>(null) }

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

    if (selectedDoubt != null) {
        DoubtDetailDialog(
            doubt = selectedDoubt!!,
            onDismiss = { selectedDoubt = null },
            onReplyPosted = { 
                selectedDoubt = null
                loadDoubts() 
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Ask Question") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        MeshBackground {
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (doubts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.QuestionAnswer, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No doubts yet.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Be the first to ask!", color = Color.Gray)
                                }
                            }
                        }
                    }
                    items(doubts) { doubt ->
                        DoubtCard(doubt, onClick = { selectedDoubt = doubt })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun DoubtCard(doubt: Doubt, onClick: () -> Unit) {
    AcadMateCard(variant = CardVariant.Elevated, onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(doubt.authorName.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(doubt.authorName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(doubt.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Text(doubt.title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(doubt.content, style = MaterialTheme.typography.bodySmall, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("${doubt.replies} replies", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun AddDoubtDialog(onDismiss: () -> Unit, onPost: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        AcadMateCard(variant = CardVariant.Elevated, modifier = Modifier.fillMaxWidth().padding(16.dp), contentPadding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Ask the Community", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                AcadMateTextField(value = title, onValueChange = { title = it }, label = "Topic Title")
                AcadMateTextField(value = content, onValueChange = { content = it }, label = "Describe your doubt", modifier = Modifier.height(120.dp), singleLine = false)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    AcadMateButton(text = "Post Doubt", onClick = { onPost(title, content) }, modifier = Modifier.weight(1f), enabled = title.isNotBlank() && content.isNotBlank())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubtDetailDialog(doubt: Doubt, onDismiss: () -> Unit, onReplyPosted: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    
    var replies by remember { mutableStateOf<List<Reply>>(emptyList()) }
    var replyText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }

    LaunchedEffect(doubt.id) {
        val snapshot = firestore.collection("doubts").document(doubt.id).collection("replies")
            .orderBy("timestamp", Query.Direction.ASCENDING).get().await()
        replies = snapshot.toObjects(Reply::class.java)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(vertical = 32.dp, horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Discussion", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }
                
                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    item {
                        DoubtHeader(doubt)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Replies", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                    }
                    
                    if (replies.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No replies yet. Start the conversation!", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    items(replies) { reply ->
                        ReplyItem(reply)
                    }
                }
                
                // Reply Input
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AcadMateTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = "Write a reply...",
                            label = "", // Optional label if needed
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    scope.launch {
                                        isPosting = true
                                        try {
                                            val userId = auth.currentUser?.uid ?: return@launch
                                            val userName = auth.currentUser?.displayName ?: "Student"
                                            val reply = Reply(
                                                id = UUID.randomUUID().toString(),
                                                content = replyText,
                                                authorName = userName,
                                                authorId = userId,
                                                timestamp = System.currentTimeMillis()
                                            )
                                            val doubtRef = firestore.collection("doubts").document(doubt.id)
                                            doubtRef.collection("replies").document(reply.id).set(reply).await()
                                            
                                            // Increment reply count
                                            firestore.runTransaction { transaction ->
                                                val snapshot = transaction.get(doubtRef)
                                                val currentReplies = snapshot.getLong("replies") ?: 0
                                                transaction.update(doubtRef, "replies", currentReplies + 1)
                                            }.await()
                                            
                                            replyText = ""
                                            onReplyPosted()
                                        } catch (e: Exception) { } finally {
                                            isPosting = false
                                        }
                                    }
                                }
                            },
                            enabled = !isPosting && replyText.isNotBlank(),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                        ) {
                            if (isPosting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            else Icon(Icons.AutoMirrored.Filled.Send, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoubtHeader(doubt: Doubt) {
    Column {
        Text(doubt.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(doubt.content, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Text(doubt.authorName.take(1), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(doubt.authorName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("•", color = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text(
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(doubt.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ReplyItem(reply: Reply) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(20.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Text(reply.authorName.take(1), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(reply.authorName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(reply.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        Spacer(Modifier.height(4.dp))
        AcadMateCard(variant = CardVariant.Flat, backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
            Text(reply.content, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
