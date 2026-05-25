package com.acadmate.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.acadmate.designsystem.theme.AcadMateTheme
import com.acadmate.R
import kotlinx.coroutines.delay

import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Entrance animations
    var startAnimation by remember { mutableStateOf(false) }
    
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800), // Slightly faster
        label = "alpha"
    )
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f, // Reduced jump
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is SplashUiState.NavigateToAuth -> {
                delay(1500L) // Reduced delay for better UX
                onNavigateToOnboarding()
            }
            is SplashUiState.NavigateToDashboard -> {
                delay(1500L)
                onNavigateToDashboard()
            }
            is SplashUiState.NavigateToAdmin -> {
                delay(1500L)
                onNavigateToAdmin()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scaleAnim)
                .alpha(alphaAnim)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(com.acadmate.designsystem.R.drawable.acadmate)
                    .size(512)
                    .build(),
                contentDescription = "AcadMate Logo",
                modifier = Modifier.size(180.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    AcadMateTheme {
        SplashScreen(
            onNavigateToOnboarding = {},
            onNavigateToDashboard = {},
            onNavigateToAdmin = {}
        )
    }
}
