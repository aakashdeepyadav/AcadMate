package com.acadmate.dashboard.student

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.acadmate.core.util.QrCodeUtils
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.GradientPrimary
import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalIdCardScreen(
    viewModel: StudentHomeViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFullScreenQr by remember { mutableStateOf(false) }
    
    val qrBitmap = remember(uiState.studentEnrollment) {
        if (uiState.studentEnrollment.isNotBlank()) {
            QrCodeUtils.generateQrCode(uiState.studentEnrollment, 512)
        } else null
    }

    if (showFullScreenQr && qrBitmap != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullScreenQr = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { showFullScreenQr = false },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .size(320.dp)
                            .padding(16.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Full Screen QR",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = uiState.studentEnrollment,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "Tap anywhere to close",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Institutional ID", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AcadMateCard(
                variant = CardVariant.Gradient,
                gradientColors = GradientPrimary,
                cornerRadius = 32.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.65f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // College Header
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.School, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "ACADMATE INSTITUTE OF TECHNOLOGY",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Profile Image
                    Surface(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        if (uiState.profilePictureUrl != null) {
                            AsyncImage(
                                model = uiState.profilePictureUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    uiState.studentName.take(1),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        uiState.studentName.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        "STUDENT • ${uiState.studentDepartment}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "SECTION: ${uiState.studentSection}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "BATCH: 2022-26", // Dynamic batch could be added later
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        thickness = 0.5.dp,
                        color = Color.White.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // QR Code Area
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(130.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clickable { showFullScreenQr = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "ID QR",
                                    modifier = Modifier.size(110.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = "ID QR",
                                    modifier = Modifier.size(110.dp),
                                    tint = Color.Black.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            "REGISTRATION NUMBER",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            uiState.studentEnrollment,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AcadMateCard(variant = CardVariant.Flat) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(label = "Email", value = uiState.studentEmail)
                    InfoRow(label = "Phone", value = uiState.studentPhone)
                    InfoRow(label = "Blood Group", value = "O+ve") // Placeholder as not in DB yet
                    InfoRow(label = "Validity", value = "June 2026")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Scan this QR code at campus entry points, library, and for secure attendance verification.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
