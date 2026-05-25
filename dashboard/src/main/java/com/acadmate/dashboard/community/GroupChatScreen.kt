package com.acadmate.dashboard.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadmate.core.model.*
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.SoftBlue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    onBackClick: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    
    var group by remember { mutableStateOf<CommunityGroup?>(null) }
    var messages by remember { mutableStateOf<List<CommunityMessage>>(emptyList()) }
    var userRole by remember { mutableStateOf<UserRole?>(null) }
    var isCr by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    // Fetch user details & group
    LaunchedEffect(groupId) {
        firestore.collection("community_groups").document(groupId).get().addOnSuccessListener {
            group = it.toObject(CommunityGroup::class.java)
            isCr = group?.crIds?.contains(userId) == true
        }
        
        firestore.collection("users").document(userId).get().addOnSuccessListener {
            userRole = UserRole.fromString(it.getString("role"))
        }

        firestore.collection("community_messages")
            .whereEqualTo("groupId", groupId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                messages = snapshot?.toObjects(CommunityMessage::class.java) ?: emptyList()
            }
    }

    val canModerate = userRole == UserRole.ADMIN || userRole == UserRole.FACULTY || isCr

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.subject ?: "Loading...", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { msg ->
                    val isOwn = msg.senderId == userId
                    
                    val shouldShow = when (msg.status) {
                        MessageStatus.APPROVED -> true
                        MessageStatus.PENDING -> canModerate || isOwn
                        MessageStatus.REJECTED -> canModerate || isOwn
                    }

                    if (shouldShow) {
                        MessageBubble(
                            message = msg,
                            isOwn = isOwn,
                            canModerate = canModerate,
                            onAction = { status ->
                                firestore.collection("community_messages").document(msg.id).update("status", status)
                            }
                        )
                    }
                }
            }

            ChatInputArea(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    val status = if (userRole == UserRole.ADMIN || userRole == UserRole.FACULTY || isCr) 
                        MessageStatus.APPROVED else MessageStatus.PENDING
                    
                    val msgId = firestore.collection("community_messages").document().id
                    val msg = CommunityMessage(
                        id = msgId,
                        groupId = groupId,
                        senderId = userId,
                        senderName = auth.currentUser?.displayName ?: "Student",
                        senderRole = userRole?.name ?: "STUDENT",
                        text = inputText,
                        status = status
                    )
                    firestore.collection("community_messages").document(msgId).set(msg)
                    inputText = ""
                }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: CommunityMessage,
    isOwn: Boolean,
    canModerate: Boolean,
    onAction: (MessageStatus) -> Unit
) {
    val isPending = message.status == MessageStatus.PENDING
    val isRejected = message.status == MessageStatus.REJECTED
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = when {
                isOwn && isRejected -> Color.Gray
                isOwn -> SoftBlue
                isRejected -> Color.LightGray.copy(alpha = 0.5f)
                else -> Color(0xFFF1F1F1)
            },
            shape = RoundedCornerShape(
                topStart = 16.dp, 
                topEnd = 16.dp, 
                bottomStart = if (isOwn) 16.dp else 0.dp, 
                bottomEnd = if (isOwn) 0.dp else 16.dp
            ),
            modifier = Modifier.alpha(if (isRejected) 0.6f else 1f)
        ) {
            Column(Modifier.padding(12.dp)) {
                if (!isOwn) {
                    Text(
                        text = "${message.senderName} (${message.senderRole})", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold, 
                        color = if (isRejected) Color.Gray else SoftBlue
                    )
                }
                Text(
                    text = if (isRejected) "This message was rejected" else message.text, 
                    color = if (isOwn && !isRejected) Color.White else Color.Black,
                    style = if (isRejected) MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) else MaterialTheme.typography.bodyMedium
                )
                
                if (isPending) {
                    Text("PENDING APPROVAL", style = MaterialTheme.typography.labelSmall, color = if (isOwn) Color.White.copy(0.7f) else Color.Gray)
                }
                if (isRejected && isOwn) {
                    Text("REJECTED BY MODERATOR", style = MaterialTheme.typography.labelSmall, color = Color.Red.copy(alpha = 0.8f))
                }
            }
        }
        
        if (isPending && canModerate && !isOwn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = { onAction(MessageStatus.REJECTED) }) { Text("Reject", color = Color.Red, fontSize = 12.sp) }
                Button(onClick = { onAction(MessageStatus.APPROVED) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { 
                    Text("Approve", fontSize = 12.sp) 
                }
            }
        }
    }
}

@Composable
fun ChatInputArea(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(onClick = onSend, enabled = value.isNotBlank()) {
                Icon(Icons.Default.Send, null, tint = SoftBlue)
            }
        }
    }
}
