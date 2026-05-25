package com.acadmate.dashboard.faculty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.model.LectureLog
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureLogScreen(
    viewModel: LectureLogViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val logs by viewModel.logs.collectAsState()

    if (showAddDialog) {
        AddLectureLogDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { topic, unit, notes ->
                viewModel.addLog(topic, unit, notes)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lecture Logs") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Log")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Log")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Teaching History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(logs) { log ->
                LectureLogItem(log)
            }
        }
    }
}

@Composable
fun LectureLogItem(log: LectureLog) {
    AcadMateCard(variant = CardVariant.Elevated) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(log.unitTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(log.topicCovered, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(log.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(log.date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AddLectureLogDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var topic by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        AcadMateCard(variant = CardVariant.Elevated, contentPadding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Log Lecture Progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                AcadMateTextField(value = topic, onValueChange = { topic = it }, label = "Topic Covered", placeholder = "e.g. Lambda Expressions")
                AcadMateTextField(value = unit, onValueChange = { unit = it }, label = "Unit/Module", placeholder = "e.g. Unit 3")
                AcadMateTextField(value = notes, onValueChange = { notes = it }, label = "Session Notes", placeholder = "Brief summary of the lecture...", modifier = Modifier.height(100.dp), singleLine = false)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    AcadMateButton(
                        text = "Save Log",
                        onClick = { onAdd(topic, unit, notes) },
                        modifier = Modifier.weight(1f),
                        enabled = topic.isNotBlank() && unit.isNotBlank()
                    )
                }
            }
        }
    }
}
