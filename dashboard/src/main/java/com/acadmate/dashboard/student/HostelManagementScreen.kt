package com.acadmate.dashboard.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Restaurant
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
import com.acadmate.designsystem.theme.AccentEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostelManagementScreen(
    viewModel: HostelViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mess Menu", "Leave Request", "My Room")
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hostel & Mess", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> MessMenuSection()
                    1 -> HostelLeaveSection(uiState.leaves, onApply = { r, f, t -> viewModel.applyLeave(r, f, t) })
                    2 -> MyRoomSection(uiState.roomInfo)
                }
            }
        }
    }
}

@Composable
fun MessMenuSection() {
    val menu = listOf(
        MessItem("Breakfast", "07:30 - 09:00", "Aloo Paratha, Curd, Tea, Bread Jam", Icons.Default.Restaurant),
        MessItem("Lunch", "12:30 - 14:00", "Veg Biryani, Raita, Salad, Gulab Jamun", Icons.Default.Restaurant),
        MessItem("Snacks", "17:00 - 18:00", "Samosa, Masala Chai", Icons.Default.Restaurant),
        MessItem("Dinner", "19:30 - 21:00", "Paneer Butter Masala, Roti, Rice, Dal Fry", Icons.Default.Restaurant)
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Today's Menu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("Thursday, Oct 26", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        
        items(menu) { item ->
            AcadMateCard(variant = CardVariant.Flat) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(item.icon, null, tint = SoftBlue)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(item.meal, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(item.time, style = MaterialTheme.typography.labelSmall, color = SoftBlue)
                        Spacer(Modifier.height(4.dp))
                        Text(item.items, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun HostelLeaveSection(
    leaves: List<HostelLeave>,
    onApply: (String, String, String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Apply for Outpass", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        
        AcadMateCard(variant = CardVariant.Elevated) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason for Leave") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fromDate, onValueChange = { fromDate = it }, label = { Text("From Date") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = toDate, onValueChange = { toDate = it }, label = { Text("To Date") }, modifier = Modifier.weight(1f))
                }
                AcadMateButton(
                    text = "Submit Request", 
                    onClick = { 
                        onApply(reason, fromDate, toDate)
                        reason = ""; fromDate = ""; toDate = ""
                    }, 
                    modifier = Modifier.fillMaxWidth(),
                    enabled = reason.isNotBlank() && fromDate.isNotBlank()
                )
            }
        }
        
        Text("Previous Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (leaves.isEmpty()) {
            Text("No leave history", color = Color.Gray)
        }
        
        leaves.forEach { leave ->
            AcadMateCard(variant = CardVariant.Flat, modifier = Modifier.padding(bottom = 8.dp)) {
                ListItem(
                    headlineContent = { Text(leave.reason) },
                    supportingContent = { Text("${leave.startDate} - ${leave.endDate}") },
                    trailingContent = { 
                        Text(
                            text = leave.status, 
                            color = if (leave.status == "APPROVED") AccentEmerald else if (leave.status == "REJECTED") Color.Red else SoftBlue,
                            fontWeight = FontWeight.Bold
                        ) 
                    }
                )
            }
        }
    }
}

@Composable
fun MyRoomSection(roomInfo: String) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Room Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        
        AcadMateCard(variant = CardVariant.Gradient, gradientColors = listOf(SoftBlue, Color(0xFF3B82F6))) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bed, null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text(roomInfo, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Type: Double Occupancy (AC)", color = Color.White.copy(0.8f))
            }
        }
        
        Text("Quick Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ServiceAction("Cleaning", Icons.Default.Bed, Modifier.weight(1f))
            ServiceAction("Repairs", Icons.Default.Build, Modifier.weight(1f))
        }
    }
}

@Composable
fun ServiceAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    AcadMateCard(modifier = modifier, variant = CardVariant.Flat) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = SoftBlue)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

data class MessItem(val meal: String, val time: String, val items: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
