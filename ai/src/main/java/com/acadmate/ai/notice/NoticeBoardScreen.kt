package com.acadmate.ai.notice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.LocalSpacing
import com.acadmate.core.db.NoticeEntity
import com.acadmate.attendance.domain.AttendanceViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeBoardScreen(
    onBackClick: () -> Unit,
    viewModel: NoticeBoardViewModel = hiltViewModel()
) {
    val notices by viewModel.notices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPosting by viewModel.isPosting.collectAsState()
    val attendanceViewModel: AttendanceViewModel = hiltViewModel()
    val userRole by attendanceViewModel.userRole.collectAsState()
    
    var showPostDialog by remember { mutableStateOf(false) }

    if (showPostDialog) {
        PostNoticeDialog(
            onDismiss = { showPostDialog = false },
            onPost = { title, content ->
                viewModel.postNotice(title, content)
                showPostDialog = false
            },
            isPosting = isPosting
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Institutional Notices", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (userRole == com.acadmate.core.model.UserRole.FACULTY || userRole == com.acadmate.core.model.UserRole.ADMIN) {
                FloatingActionButton(
                    onClick = { showPostDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Post Notice")
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        MeshBackground {
            if (isLoading && notices.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(LocalSpacing.current.md),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (notices.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Campaign, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No announcements yet.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Updates from your institution will appear here.", color = Color.Gray)
                                }
                            }
                        }
                    }
                    
                    items(notices) { notice ->
                        NoticeCard(notice)
                    }
                    
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
fun PostNoticeDialog(
    onDismiss: () -> Unit,
    onPost: (String, String) -> Unit,
    isPosting: Boolean
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post New Notice", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AcadMateTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Notice Title",
                    placeholder = "e.g. Technical Seminar"
                )
                AcadMateTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = "Content",
                    placeholder = "Detailed message...",
                    singleLine = false,
                    modifier = Modifier.height(150.dp)
                )
            }
        },
        confirmButton = {
            AcadMateButton(
                text = "Broadcast",
                onClick = { onPost(title, content) },
                enabled = title.isNotBlank() && content.isNotBlank() && !isPosting,
                loading = isPosting
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isPosting) { Text("Cancel") }
        }
    )
}

@Composable
fun NoticeCard(notice: NoticeEntity) {
    AcadMateCard(variant = CardVariant.Elevated) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(notice.date, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Icon(Icons.Default.PushPin, null, modifier = Modifier.size(16.dp), tint = Color.LightGray)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(notice.title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, letterSpacing = (-0.5).sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                notice.content, 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(14.dp), tint = Color(0xFF10B981))
                Spacer(Modifier.width(6.dp))
                Text("Verified Institutional Broadcast", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}
