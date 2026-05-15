package com.acadmate.ai.exam

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSetupScreen(
    viewModel: MockExamViewModel,
    onBackClick: () -> Unit
) {
    val availableSubjects by viewModel.availableSubjects.collectAsState()
    val subjects = remember(availableSubjects) { 
        if (availableSubjects.isNotEmpty()) availableSubjects.map { it.subjectName }
        else com.acadmate.core.model.PredefinedSyllabus.bTechCse6thSem.map { it.subjectName } 
    }
    var selectedSubject by remember(subjects) { mutableStateOf(subjects.firstOrNull() ?: "General") }
    var difficulty by remember { mutableFloatStateOf(1f) }
    var questionCount by remember { mutableIntStateOf(10) }
    var timeLimitEnabled by remember { mutableStateOf(true) }
    val difficultyLabel = when (difficulty.toInt()) {
        0 -> "Easy"
        1 -> "Medium"
        2 -> "Hard"
        else -> "Mixed"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mock Exam Setup") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Select Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjects) { subject ->
                    FilterChip(
                        selected = selectedSubject == subject,
                        onClick = { selectedSubject = subject },
                        label = { Text(subject) }
                    )
                }
            }

            Column {
                Text("Difficulty: $difficultyLabel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Slider(
                    value = difficulty,
                    onValueChange = { difficulty = it },
                    valueRange = 0f..3f,
                    steps = 2
                )
            }

            Column {
                Text("Number of Questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf(10, 20, 30).forEach { count ->
                        ElevatedFilterChip(
                            selected = questionCount == count,
                            onClick = { questionCount = count },
                            label = { Text("$count") }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Time Limit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Switch(checked = timeLimitEnabled, onCheckedChange = { timeLimitEnabled = it })
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.generateExam(
                        selectedSubject,
                        difficultyLabel,
                        questionCount,
                        if (timeLimitEnabled) 10 else null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Generate AI Exam")
            }
        }
    }
}
