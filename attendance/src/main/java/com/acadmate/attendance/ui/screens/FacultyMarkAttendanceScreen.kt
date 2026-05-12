package com.acadmate.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.attendance.domain.FacultyAttendanceViewModel
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.components.MeshBackground
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Canvas
import com.acadmate.designsystem.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyMarkAttendanceScreen(
    classId: String,
    onBackClick: () -> Unit,
    viewModel: FacultyAttendanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBroadcasting = uiState.isSessionActive
    var currentSubject by remember { mutableStateOf(classId) }
    var isEditingSubject by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(600) } // 10 minutes in seconds

    LaunchedEffect(classId) {
        if (!isBroadcasting) {
            currentSubject = classId
        }
    }

    LaunchedEffect(isBroadcasting) {
        if (isBroadcasting) {
            timeLeft = 600
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (isBroadcasting) "Session Live: $currentSubject" else "Start Session") },
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
        MeshBackground(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Decorative "Poster" Elements for Faculty side
            FacultyDecorativeElements()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isBroadcasting) {
                    Text(
                        text = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Remaining Time",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                AcadMateCard(
                    variant = CardVariant.Elevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isBroadcasting) {
                            if (isEditingSubject) {
                                OutlinedTextField(
                                    value = currentSubject,
                                    onValueChange = { currentSubject = it },
                                    label = { Text("Enter Subject Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                TextButton(onClick = { isEditingSubject = false }) {
                                    Text("Done")
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = currentSubject,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { isEditingSubject = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Subject", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Icon(
                            imageVector = if (isBroadcasting) Icons.Default.Bluetooth else Icons.Default.Podcasts,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = if (isBroadcasting) "Broadcasting Session..." else "Ready to Start Session",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (isBroadcasting) 
                                "Students can now mark their attendance for $currentSubject." 
                                else "Click the button below to start broadcasting the attendance beacon for $currentSubject.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                AcadMateButton(
                    text = if (isBroadcasting) "Stop Session" else "Start Session",
                    onClick = { 
                        if (isBroadcasting) viewModel.stopAttendanceSession() 
                        else viewModel.startAttendanceSession(currentSubject) 
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isBroadcasting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Session Token: ATT_SESSION_$currentSubject",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FacultyDecorativeElements() {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.04f)) {
        // Abstract geometric pattern
        val size = 300.dp.toPx()
        drawRect(
            color = primaryColor,
            topLeft = androidx.compose.ui.geometry.Offset(-size/2, -size/2),
            size = androidx.compose.ui.geometry.Size(size, size)
        )
        
        drawRect(
            color = primaryColor,
            topLeft = androidx.compose.ui.geometry.Offset(this.size.width - size/2, this.size.height - size/2),
            size = androidx.compose.ui.geometry.Size(size, size)
        )
    }
}
