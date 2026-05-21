package com.acadmate.ai.lecture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.theme.rememberAcadMateHapticFeedback
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureNotesScreen(
    onBackClick: () -> Unit,
    viewModel: LectureNotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableSubjects by viewModel.availableSubjects.collectAsState()
    val haptic = rememberAcadMateHapticFeedback()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var selectedUnit by remember { mutableStateOf<String?>(null) }

    val subjects = remember(availableSubjects) { availableSubjects.map { it.subjectName } }
    val units = remember(selectedSubject, availableSubjects) {
        if (selectedSubject == null) emptyList()
        else availableSubjects.find { it.subjectName == selectedSubject }?.units?.map { it.title } ?: emptyList()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is LectureUiState.Success -> haptic.success()
            is LectureUiState.Error -> haptic.error()
            is LectureUiState.Recording -> haptic.heavy()
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Lecture Summarizer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState !is LectureUiState.Idle) {
                        IconButton(onClick = { viewModel.reset() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Context Selectors (Hidden by default, can be toggled)
                var showContextSetup by remember { mutableStateOf(false) }

                if (uiState is LectureUiState.Idle) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedSubject != null) "Topic: $selectedSubject" else "General Lecture",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { showContextSetup = !showContextSetup }) {
                                Text(if (showContextSetup) "Hide Setup" else "Change Topic")
                            }
                        }
                        
                        AnimatedVisibility(visible = showContextSetup) {
                            Column {
                                // Subject Chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(subjects) { subject ->
                                        FilterChip(
                                            selected = selectedSubject == subject,
                                            onClick = { 
                                                selectedSubject = if (selectedSubject == subject) null else subject 
                                                selectedUnit = null
                                            },
                                            label = { Text(subject) }
                                        )
                                    }
                                }

                                // Unit Chips
                                AnimatedVisibility(visible = units.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier.padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(units) { unit ->
                                            FilterChip(
                                                selected = selectedUnit == unit,
                                                onClick = { selectedUnit = if (selectedUnit == unit) null else unit },
                                                label = { Text(unit) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp).graphicsLayer(alpha = 0.3f))
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = uiState,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "LectureUIState"
                    ) { state ->
                        when (state) {
                            is LectureUiState.Idle -> {
                                IdleContent(onStart = { 
                                    val permission = android.Manifest.permission.RECORD_AUDIO
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        viewModel.startRecording()
                                    } else {
                                        permissionLauncher.launch(permission)
                                    }
                                })
                            }
                            is LectureUiState.Recording -> {
                                RecordingContent(onStop = { 
                                    viewModel.stopRecordingAndProcess(
                                        selectedSubject ?: "Current Lecture",
                                        selectedUnit
                                    ) 
                                })
                            }
                            is LectureUiState.Processing -> {
                                ProcessingContent()
                            }
                            is LectureUiState.Success -> {
                                SuccessContent(
                                    notes = state.notes, 
                                    onSave = { viewModel.saveNotes(state.notes, selectedSubject ?: "GENERAL") }
                                )
                            }
                            is LectureUiState.Error -> {
                                ErrorContent(message = state.message, onRetry = { viewModel.startRecording() })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleContent(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Ready to Record", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "AcadMate uses Gemini 3 Flash to transform your lecture audio into structured study material.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        AcadMateButton(
            text = "Start AI Recording",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RecordingContent(onStop: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            val infiniteTransition = rememberInfiniteTransition(label = "wave")
            
            repeat(3) { index ->
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, delayMillis = index * 400),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "scale"
                )
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, delayMillis = index * 400),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "alpha"
                )
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
            
            Icon(
                Icons.Default.Mic, 
                null, 
                modifier = Modifier.size(48.dp), 
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Listening...", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Recording audio and preparing for AI processing...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        IconButton(
            onClick = onStop,
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.errorContainer, shape = CircleShape)
        ) {
            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun ProcessingContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Gemini is analyzing...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Generating structured notes, formulas, and summaries.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SuccessContent(notes: String, onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Lecture Summary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        MarkdownText(
            markdown = notes,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
        AcadMateButton(
            text = "Save to Productivity Hub",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Text(message, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
