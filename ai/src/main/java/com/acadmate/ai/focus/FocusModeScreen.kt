package com.acadmate.ai.focus

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.LocalSpacing
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(onBackClick: () -> Unit) {
    var isEnabled by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableIntStateOf(25) }
    var timeLeft by remember { mutableIntStateOf(1500) } // Seconds
    var isPaused by remember { mutableStateOf(true) }
    
    LaunchedEffect(isEnabled, isPaused) {
        if (isEnabled && !isPaused && timeLeft > 0) {
            while (timeLeft > 0 && isEnabled && !isPaused) {
                delay(1000)
                timeLeft--
            }
            if (timeLeft == 0) {
                isEnabled = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deep Focus", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        MeshBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Timer Progress Circle
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
                    val progress = if (isEnabled) timeLeft / (selectedMinutes * 60f) else 1f
                    
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 14.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", timeLeft / 60, timeLeft % 60),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 64.sp,
                                letterSpacing = (-2).sp
                            )
                        )
                        Text(
                            text = if (isEnabled && !isPaused) "SESSION ACTIVE" else if (isEnabled) "PAUSED" else "READY TO FOCUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEnabled && !isPaused) MaterialTheme.colorScheme.primary else Color.Gray,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                if (!isEnabled) {
                    // Session Configuration
                    AcadMateCard(variant = CardVariant.Flat) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Set Session Duration", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(15, 25, 45, 60).forEach { mins ->
                                    FilterChip(
                                        selected = selectedMinutes == mins,
                                        onClick = { 
                                            selectedMinutes = mins
                                            timeLeft = mins * 60
                                        },
                                        label = { Text("${mins}m") },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    AcadMateButton(
                        text = "Enter Flow State",
                        onClick = { 
                            isEnabled = true
                            isPaused = false
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        icon = Icons.Default.Bolt
                    )
                } else {
                    // Active Session Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                isEnabled = false
                                timeLeft = selectedMinutes * 60
                                isPaused = true
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Quit Session")
                        }
                        
                        AcadMateButton(
                            text = if (isPaused) "Resume" else "Pause",
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier.weight(1f).height(56.dp),
                            icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    // Motivation Quote
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatQuote, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Focus on being productive instead of busy.",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    "AcadMate blocks non-essential alerts during focus sessions to maximize your learning efficiency.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
