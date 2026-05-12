package com.acadmate.attendance.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acadmate.attendance.data.AttendanceUiState
import com.acadmate.attendance.domain.AttendanceViewModel
import com.acadmate.attendance.ui.components.AnimatedMapPinDrop
import com.acadmate.attendance.ui.components.AttendanceStatusBanner
import com.acadmate.attendance.ui.components.BleRadarAnimation
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.ButtonVariant
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.LocalSpacing
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.acadmate.attendance.ui.components.CameraPreview
import com.acadmate.attendance.ui.components.FaceOvalOverlay
import com.acadmate.attendance.ui.components.ProgressIndicator
import com.acadmate.attendance.ui.components.AcousticWaveAnimation
import com.acadmate.designsystem.components.MeshBackground
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Canvas
import com.acadmate.attendance.data.LivenessResult
import com.acadmate.attendance.face.FaceLivenessDetector
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.acadmate.designsystem.theme.rememberAcadMateHapticFeedback
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.fragment.app.FragmentActivity
import com.acadmate.core.security.BiometricAuthenticator
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.attendance.geo.AttendanceForegroundService

@Composable
fun MarkAttendanceScreen(
    viewModel: AttendanceViewModel,
    subject: String = "Class",
    faculty: String = "Faculty",
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    val haptic = rememberAcadMateHapticFeedback()

    // Biometric re-auth handling
    val biometricAuthenticator = remember { BiometricAuthenticator(context) }
    
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AttendanceForegroundService.ACTION_REAUTH_REQUIRED) {
                    val subjectName = intent.getStringExtra(AttendanceForegroundService.EXTRA_SUBJECT) ?: subject
                    val activity = (context as? FragmentActivity) ?: (context?.let { 
                        var ctx = it
                        while (ctx is android.content.ContextWrapper) {
                            if (ctx is FragmentActivity) break
                            ctx = ctx.baseContext
                        }
                        ctx as? FragmentActivity
                    })
                    
                    activity?.let {
                        biometricAuthenticator.authenticate(
                            activity = it,
                            title = "Presence Verification",
                            subtitle = "Confirm you are still in $subjectName",
                            onSuccess = {
                                viewModel.onReauthComplete(it, subjectName)
                            },
                            onError = { error ->
                                // Handle failure - maybe report anomaly
                            }
                        )
                    }
                }
            }
        }
        val filter = IntentFilter(AttendanceForegroundService.ACTION_REAUTH_REQUIRED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    var permissionsGranted by remember {
        mutableStateOf(
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            viewModel.startAttendanceFlow(subject, faculty, context)
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            AttendanceUiState.Loading -> currentStep = 1
            AttendanceUiState.VerifyingAcoustic -> {
                currentStep = 1
                haptic.success()
            }
            AttendanceUiState.VerifyingIdentity -> {
                currentStep = 2
                haptic.success()
            }
            AttendanceUiState.VerifyingLocation -> {
                currentStep = 3
                haptic.success()
            }
            is AttendanceUiState.Verified -> {
                currentStep = 4
                haptic.heavy()
            }
            is AttendanceUiState.Failed -> haptic.error()
            else -> {}
        }
    }

    Scaffold { paddingValues ->
        MeshBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Decorative "Poster" Elements
            DecorativePosterElements()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top progress indicator
                ProgressIndicator(
                    totalSteps = 4,
                    currentStep = currentStep,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Main content area
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) { state ->
                    when (state) {
                        AttendanceUiState.Idle -> {
                            IdleScreen(onStart = {
                                haptic.success()
                                // Set state and launch permissions together
                                viewModel.startAttendanceFlow(subject, faculty, context)
                                launcher.launch(permissionsToRequest)
                            })
                        }

                        AttendanceUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        AttendanceUiState.VerifyingAcoustic -> {
                            StepAcousticVerification()
                        }

                        AttendanceUiState.VerifyingIdentity -> {
                            Step2FaceDetection(
                                detector = viewModel.faceDetector,
                                onResult = { /* Handled by Flow in VM */ }
                            )
                        }

                        AttendanceUiState.VerifyingLocation -> {
                            Step3GeoChecking()
                        }

                        is AttendanceUiState.Verified -> {
                            Step4VerificationSuccess(
                                state = state,
                                onContinue = onNavigateBack
                            )
                        }

                        is AttendanceUiState.Failed -> {
                            ErrorScreen(
                                reason = state.reason,
                                onRetry = {
                                    viewModel.reset()
                                }
                            )
                        }

                        AttendanceUiState.AlreadyMarked -> {
                            AlreadyMarkedScreen(onNavigateBack = onNavigateBack)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun DecorativePosterElements() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.05f)) {
        // Large background circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor, Color.Transparent),
                center = center.copy(x = size.width * 0.8f, y = size.height * 0.2f),
                radius = size.width * 0.6f
            )
        )
        
        // Bottom accent circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor, Color.Transparent),
                center = center.copy(x = size.width * 0.2f, y = size.height * 0.8f),
                radius = size.width * 0.5f
            )
        )
    }
}

@Composable
fun IdleScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LocalSpacing.current.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mark Attendance",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.md))

        Text(
            text = "This process will verify your location, face, and BLE beacon presence",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

        AcadMateButton(
            text = "Start Verification",
            onClick = onStart,
            variant = ButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun Step1BleScanning() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BleRadarAnimation(isScanning = true, modifier = Modifier.size(200.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Scanning for classroom beacon...",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Keep your device close to the classroom",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(16.dp))

        AttendanceStatusBanner(
            message = "Status: Scanning BLE beacon (RSSI > -70 dBm)",
            isError = false
        )
    }
}

@Composable
fun StepAcousticVerification() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AcousticWaveAnimation()

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verifying Acoustic Fingerprint",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Listening for ultrasonic presence token...\nLayer 3 Secure Verification",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        AttendanceStatusBanner(
            message = "Status: High-frequency token (18.5kHz) detection active",
            isError = false
        )
    }
}

@Composable
fun Step2FaceDetection(
    detector: FaceLivenessDetector,
    onResult: (LivenessResult) -> Unit
) {
    val livenessResult by detector.livenessResultFlow.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large
                )
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                analyzer = detector
            )
            FaceOvalOverlay(modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Face Liveness Detection",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (livenessResult is LivenessResult.Failed) (livenessResult as LivenessResult.Failed).reason
                   else "Look directly at the camera\nBlink naturally to confirm you're present",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = if (livenessResult is LivenessResult.Failed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        AttendanceStatusBanner(
            message = "Status: Detecting face - eyes open, centered, and natural movement",
            isError = livenessResult is LivenessResult.Failed
        )
    }
}

@Composable
fun Step3GeoChecking() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedMapPinDrop(isDropping = true, modifier = Modifier.size(200.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verifying Location...",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Checking if you're within campus boundaries",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(16.dp))

        AttendanceStatusBanner(
            message = "Status: Validating geofence using FusedLocationProvider",
            isError = false
        )
    }
}

@Composable
fun Step4VerificationSuccess(
    state: AttendanceUiState.Verified,
    onContinue: () -> Unit
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(com.acadmate.core.R.raw.success_checkmark)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(150.dp)
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

        Text(
            text = "Attendance Marked!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.md))

        // Details box
        AcadMateCard(
            modifier = Modifier.fillMaxWidth(),
            variant = CardVariant.Elevated
        ) {
            Column {
                DetailRow("Subject", state.subject)
                Spacer(modifier = Modifier.height(LocalSpacing.current.sm))
                DetailRow("Faculty", state.facultyName)
                Spacer(modifier = Modifier.height(LocalSpacing.current.sm))
                DetailRow("Time", "${state.timestamp.hour}:${state.timestamp.minute}")
                Spacer(modifier = Modifier.height(LocalSpacing.current.sm))
                DetailRow("Session ID", state.sessionId.takeLast(8))
            }
        }

        Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

        AcadMateButton(
            text = "Continue",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ErrorScreen(reason: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "❌",
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.md))

        Text(
            text = "Verification Failed",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.sm))

        AttendanceStatusBanner(
            message = reason,
            isError = true
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

        AcadMateButton(
            text = "Try Again",
            onClick = onRetry,
            variant = ButtonVariant.Danger,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AlreadyMarkedScreen(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.md))

        Text(
            text = "Already Marked Today",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.sm))

        Text(
            text = "Your attendance for today has already been recorded",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.lg))

        AcadMateButton(
            text = "Go Back",
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

