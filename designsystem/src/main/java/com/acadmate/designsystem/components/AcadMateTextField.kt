package com.acadmate.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.acadmate.designsystem.theme.AcadMateTheme

@Composable
fun AcadMateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            placeholder = {
                if (placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            isError = isError,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            singleLine = singleLine,
            enabled = enabled,
            readOnly = readOnly,
            supportingText = supportingText ?: {
                if (isError && errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }
}
