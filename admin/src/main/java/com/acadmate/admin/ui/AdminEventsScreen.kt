package com.acadmate.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.core.model.CampusEvent
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.AcadMateTextField
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEventsScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val events by viewModel.events.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, desc, start, end, location ->
                viewModel.createEvent(title, desc, start, end, location)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events Management") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No events scheduled", color = Color.Gray)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(LocalSpacing.current.md),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)
        ) {
            items(events) { event ->
                EventItem(event)
            }
        }
    }
}

@Composable
fun EventItem(event: CampusEvent) {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    AcadMateCard(variant = CardVariant.Elevated) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(event.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(event.description, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${df.format(Date(event.startDate))} - ${df.format(Date(event.endDate))}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(event.location, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Long, Long, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        AcadMateCard(variant = CardVariant.Elevated, contentPadding = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Schedule New Event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                AcadMateTextField(value = title, onValueChange = { title = it }, label = "Event Title")
                AcadMateTextField(value = desc, onValueChange = { desc = it }, label = "Description")
                AcadMateTextField(value = location, onValueChange = { location = it }, label = "Location")
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    AcadMateButton(
                        text = "Create",
                        onClick = { 
                            val now = System.currentTimeMillis()
                            onAdd(title, desc, now, now + 86400000, location) 
                        },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank() && location.isNotBlank()
                    )
                }
            }
        }
    }
}
