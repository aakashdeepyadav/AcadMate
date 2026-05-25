package com.acadmate.dashboard.faculty

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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.model.CommunityGroup
import com.acadmate.designsystem.components.*
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyCommunityScreen(
    onBackClick: () -> Unit,
    onChatClick: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    var groups by remember { mutableStateOf<List<CommunityGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedGroupForCr by remember { mutableStateOf<CommunityGroup?>(null) }

    val loadGroups = {
        val userId = auth.currentUser?.uid ?: ""
        isLoading = true
        firestore.collection("community_groups")
            .whereEqualTo("facultyId", userId)
            .get().addOnSuccessListener { snapshot ->
                groups = snapshot.toObjects(CommunityGroup::class.java)
                isLoading = false
            }
    }

    LaunchedEffect(Unit) { loadGroups() }

    if (selectedGroupForCr != null) {
        AssignCrDialog(
            group = selectedGroupForCr!!,
            onDismiss = { selectedGroupForCr = null },
            onAssign = { crId ->
                val currentCrs = selectedGroupForCr!!.crIds.toMutableList()
                if (currentCrs.size < 2) {
                    currentCrs.add(crId)
                    firestore.collection("community_groups").document(selectedGroupForCr!!.id)
                        .update("crIds", currentCrs).addOnSuccessListener {
                            selectedGroupForCr = null
                            loadGroups()
                        }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Class Groups") },
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
        } else if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Forum, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("No moderated groups assigned.", color = Color.Gray)
                    Text("Contact admin to create your class groups.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(groups) { group ->
                    AcadMateCard(variant = CardVariant.Elevated, onClick = { onChatClick(group.id) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${group.section} • ${group.subject}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("CRs: ${group.crIds.size}/2 Assigned", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { selectedGroupForCr = group }, enabled = group.crIds.size < 2) {
                                Text("Assign CR")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssignCrDialog(group: CommunityGroup, onDismiss: () -> Unit, onAssign: (String) -> Unit) {
    var crId by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        AcadMateCard(contentPadding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Assign CR for ${group.section}", fontWeight = FontWeight.Bold)
                AcadMateTextField(value = crId, onValueChange = { crId = it }, label = "Student Registration Number")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    AcadMateButton(text = "Assign", onClick = { onAssign(crId) }, modifier = Modifier.weight(1f), enabled = crId.isNotBlank())
                }
            }
        }
    }
}
