package com.acadmate.core.security

sealed class SecurityCheckResult {
    object AllClear : SecurityCheckResult()
    object MockLocationDetected : SecurityCheckResult()
    object DeviceMismatch : SecurityCheckResult()
    object WindowExpired : SecurityCheckResult()
    object RootedDevice : SecurityCheckResult()
    object DeveloperOptionsActive : SecurityCheckResult()
    data class Error(val message: String) : SecurityCheckResult()
}

data class SecurityStatus(
    val isSafe: Boolean = false,
    val checks: List<SecurityCheckResult> = emptyList(),
    val isLoading: Boolean = false
)
