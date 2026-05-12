package com.acadmate.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acadmate.core.security.SecurityCheckResult

@Composable
fun SecurityBlockingScreen(
    result: SecurityCheckResult,
    onRetry: () -> Unit
) {
    val title = when (result) {
        is SecurityCheckResult.MockLocationDetected -> "Mock Location Detected"
        is SecurityCheckResult.RootedDevice -> "Device Security Compromised"
        is SecurityCheckResult.DeviceMismatch -> "Unauthorized Device"
        is SecurityCheckResult.DeveloperOptionsActive -> "Developer Options Enabled"
        else -> "Security Violation"
    }

    val description = when (result) {
        is SecurityCheckResult.MockLocationDetected -> "Please disable mock location apps or developer options to proceed with attendance."
        is SecurityCheckResult.RootedDevice -> "AcadMate cannot run on rooted devices for security reasons."
        is SecurityCheckResult.DeviceMismatch -> "This account is bound to another device. Please contact administration."
        is SecurityCheckResult.DeveloperOptionsActive -> "Please disable Developer Options in your system settings to continue."
        else -> "An unexpected security anomaly was detected."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Re-scan Security")
        }
    }
}
