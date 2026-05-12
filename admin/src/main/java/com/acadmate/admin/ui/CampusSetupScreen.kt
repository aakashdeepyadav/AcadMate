package com.acadmate.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acadmate.core.model.CampusConfig
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.LocalSpacing
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusSetupScreen(
    onBackClick: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var config by remember { mutableStateOf(CampusConfig()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val doc = firestore.collection("institution").document("config").get().await()
            if (doc.exists()) {
                config = doc.toObject(CampusConfig::class.java) ?: CampusConfig()
            }
        } catch (e: Exception) { } finally {
            isLoading = false
        }
    }

    fun saveConfig() {
        if (config.latitude == 0.0 || config.longitude == 0.0) {
            scope.launch { snackbarHostState.showSnackbar("Invalid coordinates") }
            return
        }
        
        scope.launch {
            try {
                firestore.collection("institution").document("config").set(config).await()
                snackbarHostState.showSnackbar("Campus configuration updated")
                // Add log entry
                val log = hashMapOf(
                    "title" to "Campus Config Updated",
                    "timestamp" to System.currentTimeMillis(),
                    "type" to "INSTITUTION_UPDATED",
                    "description" to "Coordinates: ${config.latitude}, ${config.longitude} (Radius: ${config.radiusMeters}m)"
                )
                firestore.collection("admin_logs").add(log)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Update failed: ${e.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Campus Geofence Setup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        MeshBackground {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(LocalSpacing.current.md),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "Set the high-precision geofence boundary for your institution. Students must be within this radius to mark attendance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AcadMateCard(variant = CardVariant.Elevated) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Center Coordinates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            
                            AcadMateTextField(
                                value = config.latitude.toString(),
                                onValueChange = { config = config.copy(latitude = it.toDoubleOrNull() ?: 0.0) },
                                label = "Latitude",
                                placeholder = "e.g. 12.9716"
                            )
                            
                            AcadMateTextField(
                                value = config.longitude.toString(),
                                onValueChange = { config = config.copy(longitude = it.toDoubleOrNull() ?: 0.0) },
                                label = "Longitude",
                                placeholder = "e.g. 77.5946"
                            )
                        }
                    }

                    AcadMateCard(variant = CardVariant.Elevated) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Proximity Policy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            
                            AcadMateTextField(
                                value = if (config.radiusMeters == 0f) "" else config.radiusMeters.toString(),
                                onValueChange = { config = config.copy(radiusMeters = it.toFloatOrNull() ?: 0f) },
                                label = "Detection Radius (Meters)",
                                placeholder = "Recommended: 150-300"
                            )
                            
                            Text(
                                "Students outside this circle will be blocked from marking attendance.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    AcadMateButton(
                        text = "Apply Changes",
                        onClick = { saveConfig() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
