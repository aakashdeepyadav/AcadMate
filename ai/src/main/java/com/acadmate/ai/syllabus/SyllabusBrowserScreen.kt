package com.acadmate.ai.syllabus

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acadmate.core.model.PredefinedSyllabus
import com.acadmate.core.model.SubjectSyllabus
import com.acadmate.core.model.SyllabusUnit
import com.acadmate.core.util.PdfExportUtils
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.components.AcadMateButton
import androidx.compose.material.icons.filled.Download

import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusBrowserScreen(
    onBackClick: () -> Unit,
    viewModel: SyllabusBrowserViewModel = hiltViewModel()
) {
    var selectedSubject by remember { mutableStateOf<SubjectSyllabus?>(null) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedSubject == null) "Syllabus Browser" else selectedSubject!!.subjectName) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedSubject != null) selectedSubject = null else onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState is BrowserUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState is BrowserUiState.Success) {
                val syllabuses = (uiState as BrowserUiState.Success).syllabuses
                if (syllabuses.isEmpty()) {
                    Text(
                        "No syllabi have been published by the administrator yet.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (selectedSubject == null) {
                    SubjectList(syllabuses, onSubjectSelect = { selectedSubject = it })
                } else {
                    UnitList(selectedSubject!!)
                }
            } else if (uiState is BrowserUiState.Error) {
                Text(
                    "Error: ${(uiState as BrowserUiState.Error).message}",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun SubjectList(syllabuses: List<SubjectSyllabus>, onSubjectSelect: (SubjectSyllabus) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Course Curriculum",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(syllabuses) { syllabus ->
            AcadMateCard(
                variant = CardVariant.Flat,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSubjectSelect(syllabus) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            syllabus.subjectCode.takeLast(3),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            syllabus.subjectName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${syllabus.credits} Credits • ${syllabus.ltp}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun UnitList(syllabus: SubjectSyllabus) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Course Overview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        syllabus.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AcadMateButton(
                        text = "Download Syllabus PDF",
                        onClick = { PdfExportUtils.generateSyllabusPdf(context, syllabus) },
                        icon = Icons.Default.Download,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Curriculum Units",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(syllabus.units) { unit ->
            UnitItemRow(unit)
        }
    }
}

@Composable
fun UnitItemRow(unit: SyllabusUnit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        unit.title.filter { it.isDigit() }.ifEmpty { "U" },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    unit.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp, start = 4.dp)) {
                    Text(
                        text = unit.topics.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 40.dp)
                    )
                }
            }
        }
    }
}
