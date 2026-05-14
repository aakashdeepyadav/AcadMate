package com.acadmate.attendance.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.attendance.domain.AttendanceViewModel
import com.acadmate.core.security.SecurityCheckResult
import com.acadmate.core.security.SecurityViewModel
import com.acadmate.core.ui.SecurityBlockingScreen

@Composable
fun AttendanceSecurityWrapper(
    classId: String,
    onNavigateBack: () -> Unit,
    onGoToProfile: () -> Unit = {},
    attendanceViewModel: AttendanceViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel()
) {
    val securityState by securityViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        securityViewModel.runSecurityCheck()
    }

    if (securityState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (securityState.isSafe) {
        MarkAttendanceScreen(
            viewModel = attendanceViewModel,
            subject = classId,
            onNavigateBack = onNavigateBack,
            onGoToProfile = onGoToProfile
        )
    } else {
        val firstViolation = securityState.checks.firstOrNull { it !is SecurityCheckResult.AllClear }
        if (firstViolation != null) {
            SecurityBlockingScreen(
                result = firstViolation,
                onRetry = { securityViewModel.runSecurityCheck() }
            )
        } else {
            // Should not happen if isSafe is false and checks are done
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
