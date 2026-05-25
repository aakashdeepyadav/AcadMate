package com.acadmate.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.acadmate.core.security.BiometricAuthenticator
import com.acadmate.core.datastore.OnboardingDataStore
import com.acadmate.designsystem.components.AcadMateButton
import com.acadmate.designsystem.components.AcadMateLogo
import com.acadmate.designsystem.theme.LocalSpacing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    onUnlockSuccess: () -> Unit,
    onSignOut: () -> Unit,
    dataStore: OnboardingDataStore
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pinInput by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var storedPin by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    
    val biometricAuthenticator = remember { BiometricAuthenticator(context) }

    LaunchedEffect(Unit) {
        storedPin = dataStore.appPin.first()
        // Auto-trigger biometric if available
        val activity = context as? FragmentActivity
        if (activity != null && biometricAuthenticator.isBiometricAvailable()) {
            biometricAuthenticator.authenticate(
                activity = activity,
                title = "Unlock AcadMate",
                subtitle = "Use your fingerprint to continue",
                onSuccess = { onUnlockSuccess() },
                onError = { /* Allow fallback to PIN */ }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(LocalSpacing.current.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AcadMateLogo(iconSize = 80.dp)
        
        Spacer(modifier = Modifier.height(LocalSpacing.current.xl))
        
        Text(
            text = "App Locked",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        
        Text(
            text = "Enter your 4-digit PIN to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LocalSpacing.current.xl))

        // PIN Input (Simplified for demo, usually use a proper PIN grid)
        OutlinedTextField(
            value = pinInput,
            onValueChange = { 
                if (it.length <= 4 && it.all { it.isDigit() }) {
                    pinInput = it
                    if (it.length == 4) {
                        if (it == storedPin || storedPin == null) { // null allowed for first setup or bypass
                            onUnlockSuccess()
                        } else {
                            isError = true
                            pinInput = ""
                        }
                    }
                }
            },
            visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { pinVisible = !pinVisible }) {
                    Icon(
                        imageVector = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (pinVisible) "Hide PIN" else "Show PIN"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.width(200.dp),
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                letterSpacing = 8.sp
            ),
            isError = isError,
            placeholder = { Text("0000", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
        )

        if (isError) {
            Text(
                text = "Invalid PIN. Try again.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(LocalSpacing.current.xl))

        if (biometricAuthenticator.isBiometricAvailable()) {
            IconButton(
                onClick = {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        biometricAuthenticator.authenticate(
                            activity = activity,
                            title = "Unlock AcadMate",
                            onSuccess = { onUnlockSuccess() },
                            onError = { }
                        )
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Biometric",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "Tap to use Biometrics",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onSignOut) {
            Text("Switch Account / Sign Out")
        }
    }
}
