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
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.model.CampusConfig
import com.acadmate.designsystem.components.*
import com.acadmate.designsystem.theme.LocalSpacing
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusSetupScreen(
    onBackClick: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var config by remember { mutableStateOf(CampusConfig()) }
    var isLoading by remember { mutableStateOf(false) }

    val departments by viewModel.departments.collectAsState()
    val sections by viewModel.sections.collectAsState()
    
    var newDeptName by remember { mutableStateOf("") }
    var newSectionName by remember { mutableStateOf("") }

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
                // Merge existing list data when saving coordinates
                val data = hashMapOf(
                    "latitude" to config.latitude,
                    "longitude" to config.longitude,
                    "radiusMeters" to config.radiusMeters,
                    "departments" to departments,
                    "sections" to sections
                )
                firestore.collection("institution").document("config").set(data).await()
                snackbarHostState.showSnackbar("Campus configuration updated")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Update failed: ${e.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Institutional Setup", fontWeight = FontWeight.Bold) },
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
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Geofence Section
            Text("Geofence & Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AcadMateCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AcadMateTextField(
                        value = config.latitude.toString(),
                        onValueChange = { config = config.copy(latitude = it.toDoubleOrNull() ?: 0.0) },
                        label = "Latitude"
                    )
                    AcadMateTextField(
                        value = config.longitude.toString(),
                        onValueChange = { config = config.copy(longitude = it.toDoubleOrNull() ?: 0.0) },
                        label = "Longitude"
                    )
                    AcadMateTextField(
                        value = config.radiusMeters.toString(),
                        onValueChange = { config = config.copy(radiusMeters = it.toFloatOrNull() ?: 0f) },
                        label = "Radius (Meters)"
                    )
                    AcadMateButton(text = "Save Coordinates", onClick = { saveConfig() }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Departments Section
            Text("Departments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AcadMateCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AcadMateTextField(
                            value = newDeptName,
                            onValueChange = { newDeptName = it },
                            label = "New Department Name",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { 
                                if (newDeptName.isNotBlank()) {
                                    viewModel.addDepartment(newDeptName)
                                    newDeptName = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                        ) { Icon(Icons.Default.Add, null) }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    departments.forEach { dept ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dept)
                            IconButton(onClick = { viewModel.deleteDepartment(dept) }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Sections Section
            Text("Sections", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AcadMateCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AcadMateTextField(
                            value = newSectionName,
                            onValueChange = { newSectionName = it },
                            label = "New Section Name",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { 
                                if (newSectionName.isNotBlank()) {
                                    viewModel.addSection(newSectionName)
                                    newSectionName = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                        ) { Icon(Icons.Default.Add, null) }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    sections.forEach { section ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(section)
                            IconButton(onClick = { viewModel.deleteSection(section) }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}
