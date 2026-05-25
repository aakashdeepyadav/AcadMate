package com.acadmate.ai.exam

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSetupScreen(
    viewModel: MockExamViewModel,
    onBackClick: () -> Unit
) {
    val availableSubjects by viewModel.availableSubjects.collectAsState()
    val availableQuizzes by viewModel.availableQuizzes.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isFaculty = userRole == com.acadmate.core.model.UserRole.FACULTY

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

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()
    val isGenerating = uiState is ExamUiState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isFaculty) "Quiz Management" else "Practice Quizzes") },
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
        ) {
            if (isFaculty) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Generate Quiz", modifier = Modifier.padding(16.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Live Results", modifier = Modifier.padding(16.dp))
                    }
                }
            }

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (!isFaculty) {
                        Text("Available Quizzes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search quiz by subject...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            shape = RoundedCornerShape(12.dp)
                        )

                        val filteredQuizzes = availableQuizzes.filter { it.subject.contains(searchQuery, ignoreCase = true) }
                        
                        if (filteredQuizzes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No quizzes published for this subject yet.", color = Color.Gray)
                            }
                        }

                        filteredQuizzes.forEach { quiz ->
                            Card(
                                onClick = { viewModel.startPublishedQuiz(quiz) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(quiz.title, fontWeight = FontWeight.Bold)
                                        Text("${quiz.subject} • By ${quiz.facultyName}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text("Self-Practice Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Text("Select Subject", style = MaterialTheme.typography.labelMedium)
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
                        Text("Difficulty: $difficultyLabel", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = difficulty,
                            onValueChange = { difficulty = it },
                            valueRange = 0f..3f,
                            steps = 2
                        )
                    }

                    Column {
                        Text("Number of Questions", style = MaterialTheme.typography.labelMedium)
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

                    if (!isFaculty) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Time Limit")
                            Switch(checked = timeLimitEnabled, onCheckedChange = { timeLimitEnabled = it })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.generateExam(
                                selectedSubject,
                                difficultyLabel,
                                questionCount,
                                if (timeLimitEnabled) 10 else null,
                                shouldPublish = isFaculty
                            )
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Generating Questions...")
                        } else {
                            Text(if (isFaculty) "Generate & Publish Quiz" else "Start Practice Quiz")
                        }
                    }
                }
            } else if (selectedTab == 1 && isFaculty) {
                // Live Results Tab for Faculty
                LiveQuizResultsList(viewModel.currentUserId)
            }
        }
    }
}

@Composable
fun LiveQuizResultsList(facultyId: String?) {
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    var results by remember { mutableStateOf<List<com.google.firebase.firestore.DocumentSnapshot>>(emptyList()) }
    
    LaunchedEffect(facultyId) {
        if (facultyId == null) return@LaunchedEffect
        
        firestore.collection("exam_results")
            .whereEqualTo("facultyId", facultyId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                results = snapshot?.documents ?: emptyList()
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (results.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No results submitted yet.", color = Color.Gray)
                }
            }
        }
        items(results) { doc ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(doc.getString("studentName")?.take(1)?.uppercase() ?: "?")
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.getString("studentName") ?: "Unknown Student", fontWeight = FontWeight.Bold)
                        val score = doc.getLong("score") ?: 0L
                        val total = doc.getLong("total") ?: 0L
                        val subject = doc.getString("subject") ?: "General"
                        Text("$subject • Score: $score/$total (${doc.getDouble("percentage")?.toInt() ?: 0}%)", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(doc.getLong("timestamp") ?: 0L)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
