package com.acadmate.ai.lecture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.theme.rememberAcadMateHapticFeedback
import com.airbnb.lottie.compose.*
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureNotesScreen(
    onBackClick: () -> Unit,
    viewModel: LectureNotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = rememberAcadMateHapticFeedback()
    val context = androidx.compose.ui.platform.LocalContext.current

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
                        RecordingContent(onStop = { viewModel.stopRecordingAndProcess() })
                    }
                    is LectureUiState.Processing -> {
                        ProcessingContent()
                    }
                    is LectureUiState.Success -> {
                        SuccessContent(notes = state.notes, onSave = { viewModel.saveNotes(state.notes, "AI_SUMMARY_${System.currentTimeMillis()}") })
                    }
                    is LectureUiState.Error -> {
                        ErrorContent(message = state.message, onRetry = { viewModel.startRecording() })
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
        // Here we could add a Lottie animation for sound waves
        Text("Listening...", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Recording audio and preparing for AI processing...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        IconButton(
            onClick = onStop,
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.errorContainer, shape = androidx.compose.foundation.shape.CircleShape)
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
