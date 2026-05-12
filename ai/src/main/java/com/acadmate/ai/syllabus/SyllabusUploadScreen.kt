package com.acadmate.ai.syllabus

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acadmate.ai.syllabus.SyllabusUiState
import com.acadmate.ai.syllabus.SyllabusAiViewModel
import com.acadmate.core.model.PredefinedSyllabus
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.rememberAcadMateHapticFeedback
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusUploadScreen(
    viewModel: SyllabusAiViewModel,
    onBackClick: () -> Unit,
    onResultReady: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = rememberAcadMateHapticFeedback()
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            haptic.success()
            viewModel.uploadAndProcess(it) 
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is SyllabusUiState.Done) {
            haptic.heavy()
            onResultReady()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Syllabus Hub") },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState) {
                is SyllabusUiState.Idle -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Access: 6th Sem CSE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { /* Navigate to browser if needed, but we are often here for specific upload */ }) {
                            Text("View All")
                        }
                    }
                    
                    // Predefined subjects grid/list
                    PredefinedSyllabus.bTechCse6thSem.forEach { syllabus ->
                        AcadMateCard(
                            variant = CardVariant.Flat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            onClick = {
                                haptic.success()
                                viewModel.uploadAndProcess(Uri.parse("acadmate://syllabus/${syllabus.subjectCode}"))
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(syllabus.subjectCode.takeLast(3), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(syllabus.subjectName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("${syllabus.credits} Credits • ${syllabus.ltp}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Or Upload New",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    UploadZone(onUploadClick = { launcher.launch("application/pdf") })
                }
                is SyllabusUiState.Uploading -> {
                    val progress = (uiState as SyllabusUiState.Uploading).progress
                    Box(Modifier.height(400.dp), contentAlignment = Alignment.Center) {
                        UploadProgressState(progress)
                    }
                }
                is SyllabusUiState.Processing -> {
                    Box(Modifier.height(400.dp), contentAlignment = Alignment.Center) {
                        ProcessingState()
                    }
                }
                is SyllabusUiState.Error -> {
                    Box(Modifier.height(400.dp), contentAlignment = Alignment.Center) {
                        ErrorState((uiState as SyllabusUiState.Error).message) { viewModel.reset() }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun UploadZone(onUploadClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "upload_icon_anim")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onUploadClick() }
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .offset(y = floatAnim.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tap to upload Syllabus PDF",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Max size: 10MB",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun UploadProgressState(progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(100.dp),
            strokeWidth = 8.dp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Uploading... ${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
fun ProcessingState() {
    val steps = listOf("Extracting text", "Analyzing structure", "Generating notes")
    var currentStep by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (currentStep < steps.size - 1) {
            delay(1500)
            currentStep++
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AI is thinking...", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        
        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index < currentStep) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Green
                    )
                } else if (index == currentStep) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Color.LightGray)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (index <= currentStep) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Oops!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
        Text(text = message, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text("Try Again")
        }
    }
}
