package com.acadmate.ai.focus

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.MeshBackground
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(onBackClick: () -> Unit) {
    var isEnabled by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(1500) } // 25 minutes in seconds
    var isPaused by remember { mutableStateOf(true) }
    
    LaunchedEffect(isEnabled, isPaused) {
        if (isEnabled && !isPaused && timeLeft > 0) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            isEnabled = false
        }
    }
    
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
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
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { if (isEnabled) timeLeft / 1500f else 1f },
                        modifier = Modifier.size(240.dp),
                        strokeWidth = 12.dp,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", timeLeft / 60, timeLeft % 60),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 48.sp
                            )
                        )
                        Text(
                            text = if (isEnabled && !isPaused) "STAY FOCUSED" else if (isEnabled) "PAUSED" else "READY",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    "Deep Focus Mode", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "AcadMate blocks all non-essential notifications when active to help you achieve flow state.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (!isEnabled) {
                    AcadMateButton(
                        text = "Start 25m Session",
                        onClick = { 
                            isEnabled = true
                            isPaused = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = { 
                                isEnabled = false
                                timeLeft = 1500
                                isPaused = true
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Quit")
                        }
                        
                        Button(
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (isPaused) "Resume" else "Pause")
                        }
                    }
                }
            }
        }
    }
}
