package com.acadmate.dashboard.student

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
import com.acadmate.core.model.CommunityGroup
import com.acadmate.designsystem.components.*
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCommunityScreen(
    onBackClick: () -> Unit,
    onChatClick: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    var groups by remember { mutableStateOf<List<CommunityGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val loadGroups = {
        val userId = auth.currentUser?.uid ?: ""
        isLoading = true
        
        // Fetch user's section first
        firestore.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val userSection = userDoc.getString("section") ?: ""
            
            val query = if (userSection.isNotBlank()) {
                firestore.collection("community_groups").whereEqualTo("section", userSection)
            } else {
                firestore.collection("community_groups")
            }
            
            query.get().addOnSuccessListener { snapshot ->
                groups = snapshot.toObjects(CommunityGroup::class.java)
                isLoading = false
            }
        }.addOnFailureListener {
            // Fallback: fetch all if user data fails
            firestore.collection("community_groups").get().addOnSuccessListener { snapshot ->
                groups = snapshot.toObjects(CommunityGroup::class.java)
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadGroups() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class Communities") },
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
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(groups) { group ->
                    AcadMateCard(variant = CardVariant.Elevated, onClick = { onChatClick(group.id) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${group.section} • ${group.subject}", fontWeight = FontWeight.Bold)
                                Text("Moderated by: ${group.facultyName}", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
