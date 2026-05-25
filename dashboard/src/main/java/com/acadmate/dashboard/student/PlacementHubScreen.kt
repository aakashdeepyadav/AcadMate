package com.acadmate.dashboard.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.SoftBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementHubScreen(
    viewModel: PlacementViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Jobs", "Internships")
    val opportunities by viewModel.opportunities.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Career & Placement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = filters.indexOf(selectedFilter),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            val filteredOps = opportunities.filter { 
                if (selectedFilter == "All") true 
                else it.type == selectedFilter.removeSuffix("s") 
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredOps.isEmpty()) {
                    item { Text("No current opportunities found", color = Color.Gray) }
                }
                items(filteredOps) { job ->
                    JobCard(job)
                }
            }
        }
    }
}

@Composable
fun JobCard(job: JobOpportunity) {
    AcadMateCard(variant = CardVariant.Elevated) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Business, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(job.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(job.company, style = MaterialTheme.typography.bodySmall, color = SoftBlue)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DetailItem(Icons.Default.LocationOn, job.location)
                DetailItem(Icons.Default.Work, job.type)
                DetailItem(Icons.Default.CurrencyRupee, job.packageInfo)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            AcadMateButton(
                text = "View Details & Apply",
                onClick = { /* Apply */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

data class JobOpportunity(
    val id: String,
    val company: String,
    val title: String,
    val type: String,
    val location: String,
    val packageInfo: String,
    val status: String
)
