package com.acadmate.assignments.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.acadmate.core.model.Resource
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.LocalSpacing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceManagerScreen(
    isFaculty: Boolean,
    onBackClick: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    
    var resources by remember { mutableStateOf<List<Resource>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    fun loadResources() {
        scope.launch {
            isLoading = true
            try {
                val snapshot = firestore.collection("resources")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                resources = snapshot.toObjects(Resource::class.java)
            } catch (_: Exception) { } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadResources() }

    if (showUploadDialog) {
        UploadResourceDialog(
            onDismiss = { showUploadDialog = false },
            isUploading = isUploading,
            onUpload = { title, subject, uri ->
                scope.launch {
                    try {
                        isUploading = true
                        val userId = auth.currentUser?.uid ?: return@launch
                        val fileName = "resources/${UUID.randomUUID()}.pdf"
                        val ref = FirebaseStorage.getInstance().reference.child(fileName)
                        ref.putFile(uri).await()
                        val url = ref.downloadUrl.await().toString()
                        
                        val resource = Resource(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            subjectCode = subject,
                            fileUrl = url,
                            uploadedBy = userId,
                            facultyName = auth.currentUser?.displayName ?: "Professor"
                        )
                        firestore.collection("resources").document(resource.id).set(resource).await()
                        showUploadDialog = false
                        loadResources()
                    } catch (_: Exception) { } finally {
                        isUploading = false
                    }
                }
            }
        )
    }

    MeshBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Academic Resources", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { loadResources() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                if (isFaculty) {
                    ExtendedFloatingActionButton(
                        onClick = { showUploadDialog = true },
                        icon = { Icon(Icons.Default.CloudUpload, null) },
                        text = { Text("Upload Study Material") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                }
            }
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (resources.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                                    Spacer(Modifier.height(16.dp))
                                    Text("No materials uploaded yet", fontWeight = FontWeight.Bold)
                                    Text("Check back later for lecture notes and PDFs.", color = Color.Gray)
                                }
                            }
                        }
                    }
                    items(resources) { resource ->
                        ResourceItem(resource)
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceItem(resource: Resource) {
    AcadMateCard(variant = CardVariant.Flat) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(resource.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text("${resource.subjectCode} • By ${resource.facultyName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { /* Logic to open URL or Download */ }) {
                Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun UploadResourceDialog(
    onDismiss: () -> Unit,
    isUploading: Boolean,
    onUpload: (String, String, Uri) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Content", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Share lecture notes or study materials with students.", style = MaterialTheme.typography.bodySmall)
                
                AcadMateTextField(value = title, onValueChange = { title = it }, label = "Title (e.g. Unit 1 Notes)")
                AcadMateTextField(value = subject, onValueChange = { subject = it }, label = "Subject Code (e.g. CSE302)")
                
                OutlinedButton(
                    onClick = { launcher.launch("application/pdf") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    enabled = !isUploading
                ) {
                    Icon(if (selectedUri == null) Icons.Default.AttachFile else Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedUri == null) "Select PDF Document" else "File Attached")
                }
            }
        },
        confirmButton = {
            AcadMateButton(
                text = "Push to Students",
                onClick = { selectedUri?.let { onUpload(title, subject, it) } },
                enabled = title.isNotBlank() && subject.isNotBlank() && selectedUri != null && !isUploading,
                loading = isUploading
            )
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isUploading) { Text("Cancel") } }
    )
}
