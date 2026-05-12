package com.acadmate.assignments.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.model.Assignment
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentListScreen(
    viewModel: AssignmentViewModel = hiltViewModel(),
    isFaculty: Boolean,
    onAddAssignmentClick: () -> Unit,
    onAssignmentClick: (String) -> Unit
) {
    val assignments by viewModel.assignments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Assignments", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            if (isFaculty) {
                FloatingActionButton(onClick = onAddAssignmentClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add Assignment")
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (assignments.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No assignments found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(assignments) { assignment ->
                    AssignmentItem(assignment, onClick = { onAssignmentClick(assignment.id) })
                }
            }
        }
    }
}

@Composable
fun AssignmentItem(assignment: Assignment, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = assignment.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(text = assignment.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(assignment.dueDate)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
