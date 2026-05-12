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
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.model.Resource
import com.acadmate.designsystem.components.*
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

    fun loadResources() {
        scope.launch {
            isLoading = true
            try {
                val snapshot = firestore.collection("resources")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                resources = snapshot.toObjects(Resource::class.java)
            } catch (e: Exception) { } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadResources() }

    if (showUploadDialog) {
        UploadResourceDialog(
            onDismiss = { showUploadDialog = false },
            onUpload = { title, subject, uri ->
                scope.launch {
                    try {
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
                    } catch (e: Exception) { }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Materials", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isFaculty) {
                ExtendedFloatingActionButton(
                    onClick = { showUploadDialog = true },
                    icon = { Icon(Icons.Default.UploadFile, null) },
                    text = { Text("Upload Material") }
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
                items(resources) { resource ->
                    ResourceItem(resource)
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
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(resource.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${resource.subjectCode} • ${resource.facultyName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = { /* Logic to open URL */ }) {
                Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun UploadResourceDialog(
    onDismiss: () -> Unit,
    onUpload: (String, String, Uri) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Study Material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AcadMateTextField(value = title, onValueChange = { title = it }, label = "Title")
                AcadMateTextField(value = subject, onValueChange = { subject = it }, label = "Subject Code")
                
                OutlinedButton(onClick = { launcher.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AttachFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedUri == null) "Select PDF" else "File Selected")
                }
            }
        },
        confirmButton = {
            AcadMateButton(
                text = "Upload",
                onClick = { selectedUri?.let { onUpload(title, subject, it) } },
                enabled = title.isNotBlank() && subject.isNotBlank() && selectedUri != null
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
