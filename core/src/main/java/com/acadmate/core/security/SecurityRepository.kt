package com.acadmate.core.security

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepository @Inject constructor(
    private val proxyDetectionService: ProxyDetectionService
) {
    fun runSecurityChecks(): Flow<SecurityCheckResult> = flow {
        // 1. Check Developer Options
        if (proxyDetectionService.isDeveloperOptionsEnabled()) {
            emit(SecurityCheckResult.DeveloperOptionsActive)
        }

        // 2. Check Mock Location
        if (proxyDetectionService.isMockLocationEnabled()) {
            emit(SecurityCheckResult.MockLocationDetected)
        }

        // 3. Check Root
        if (proxyDetectionService.isRooted()) {
            emit(SecurityCheckResult.RootedDevice)
        }

        emit(SecurityCheckResult.AllClear)
    }
}
