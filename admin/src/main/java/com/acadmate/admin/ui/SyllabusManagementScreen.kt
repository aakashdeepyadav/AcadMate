package com.acadmate.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.model.SyllabusUnit
import com.acadmate.core.model.Subject
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusManagementScreen(
    onBackClick: () -> Unit,
    viewModel: SyllabusManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableSubjects by viewModel.subjects.collectAsState()
    val scope = rememberCoroutineScope()
    
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var subjectExpanded by remember { mutableStateOf(false) }
    
    var subjectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("") }
    var ltp by remember { mutableStateOf("") }
    
    data class MutableUnit(var title: String = "", var topicsRaw: String = "")
    var units by remember { mutableStateOf(listOf(MutableUnit("Unit 1", ""))) }

    LaunchedEffect(uiState) {
        if (uiState is SyllabusUiState.Success) {
            viewModel.resetState()
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Master Syllabus Setup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedSubject?.let { sub ->
                        val finalUnits = units.map { mu ->
                            SyllabusUnit(
                                title = mu.title,
                                topics = mu.topicsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                        }
                        viewModel.publishSyllabus(
                            subjectCode = sub.code,
                            subjectName = subjectName,
                            description = description,
                            credits = credits.toIntOrNull() ?: 0,
                            ltp = ltp,
                            units = finalUnits
                        )
                    }
                },
                icon = {
                    if (uiState is SyllabusUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                },
                text = {
                    Text("Save Syllabus", fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Select Master Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = !subjectExpanded }
                ) {
                    AcadMateTextField(
                        value = selectedSubject?.let { "${it.name} (${it.code})" } ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = "Subject from Database",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        availableSubjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text("${sub.name} (${sub.code})") },
                                onClick = {
                                    selectedSubject = sub
                                    subjectExpanded = false
                                    // Pre-fill existing data if any
                                    scope.launch {
                                        val existing = viewModel.getExistingSyllabus(sub.code)
                                        if (existing != null) {
                                            subjectName = existing.subjectName
                                            description = existing.description
                                            credits = existing.credits.toString()
                                            ltp = existing.ltp
                                            units = existing.units.map { 
                                                MutableUnit(it.title, it.topics.joinToString(", ")) 
                                            }
                                        } else {
                                            subjectName = sub.name
                                            description = ""
                                            credits = "4"
                                            ltp = "3-1-0"
                                            units = listOf(MutableUnit("Unit 1", ""))
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (selectedSubject != null) {
                item {
                    Text("Subject Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    AcadMateTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = "Display Name"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AcadMateTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Syllabus Description",
                        singleLine = false
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AcadMateTextField(
                            value = credits,
                            onValueChange = { credits = it },
                            label = "Credits",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        AcadMateTextField(
                            value = ltp,
                            onValueChange = { ltp = it },
                            label = "L-T-P Pattern",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Units & Topics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { units = units + MutableUnit("Unit ${units.size + 1}", "") }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Unit")
                        }
                    }
                }

                itemsIndexed(units) { index, unit ->
                    AcadMateCard(variant = CardVariant.Elevated) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("Unit ${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                if (units.size > 1) {
                                    IconButton(onClick = { 
                                        units = units.toMutableList().apply { removeAt(index) } 
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Unit", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AcadMateTextField(
                                value = unit.title,
                                onValueChange = { newTitle ->
                                    units = units.toMutableList().apply { this[index] = unit.copy(title = newTitle) }
                                },
                                label = "Unit Heading"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AcadMateTextField(
                                value = unit.topicsRaw,
                                onValueChange = { newTopics ->
                                    units = units.toMutableList().apply { this[index] = unit.copy(topicsRaw = newTopics) }
                                },
                                label = "Topics (Separated by comma)",
                                placeholder = "Topic 1, Topic 2, Topic 3",
                                singleLine = false,
                                modifier = Modifier.height(120.dp)
                            )
                        }
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Please select a subject to manage its syllabus", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
