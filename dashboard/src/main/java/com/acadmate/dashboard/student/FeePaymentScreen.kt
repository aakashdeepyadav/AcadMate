package com.acadmate.dashboard.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
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
import com.acadmate.designsystem.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeePaymentScreen(
    viewModel: FeeViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val fees by viewModel.fees.collectAsState()
    val totalBalance by viewModel.totalOutstanding.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fee Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FeeSummaryCard(totalBalance)
            }

            item {
                Text("Fee Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }

            items(fees) { fee ->
                FeeItemCard(fee)
            }

            item {
                AcadMateButton(
                    text = "Pay Outstanding Fees",
                    onClick = { /* Integrating Payment Gateway (Razorpay/Stripe) coming soon */ },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
            
            item {
                Text(
                    "Secure payment gateway integration is coming soon. Please visit the accounts office for immediate payments.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FeeSummaryCard(balance: String) {
    AcadMateCard(variant = CardVariant.Gradient, gradientColors = listOf(SoftBlue, Color(0xFF6366F1))) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Outstanding Balance", color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelMedium)
            Text(balance, color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryInfo("Next Due", "Nov 15, 2023")
                SummaryInfo("Status", if (balance == "₹0") "Fully Paid" else "Pending")
            }
        }
    }
}

@Composable
fun SummaryInfo(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(0.6f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

data class FeeItem(val title: String, val desc: String, val amount: String, val status: String)

@Composable
fun FeeItemCard(fee: FeeItem) {
    AcadMateCard(variant = CardVariant.Flat) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (fee.status == "PAID") Icons.Default.Receipt else Icons.Default.Payments,
                contentDescription = null,
                tint = if (fee.status == "PAID") Color(0xFF10B981) else WarningAmber
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fee.title, fontWeight = FontWeight.Bold)
                Text(fee.desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(fee.amount, fontWeight = FontWeight.Black)
                Text(fee.status, style = MaterialTheme.typography.labelSmall, color = if (fee.status == "PAID") Color(0xFF10B981) else WarningAmber)
            }
        }
    }
}
