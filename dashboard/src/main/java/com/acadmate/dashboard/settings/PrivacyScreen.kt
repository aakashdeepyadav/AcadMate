package com.acadmate.dashboard.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Data Usage and Privacy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                "At AcadMate, we take your privacy seriously. Your data is encrypted and securely stored in compliance with institutional guidelines.",
                style = MaterialTheme.typography.bodyLarge
            )
            
            HorizontalDivider()
            
            Text("Information Collection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "We collect minimal personal data including your Name, Enrollment/UID, and email for authentication and record-keeping purposes only.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text("Data Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Your authentication is backed by enterprise-grade security protocols. We do not sell or share your data with third parties.",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "• Camera: Required for QR-based attendance scanning.\n" +
                "• Notifications: Required for assignment deadlines and timetable updates.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
