package com.acadmate.attendance.data

import java.time.LocalDateTime

// UI State
sealed class AttendanceUiState {
    object Idle : AttendanceUiState()
    object Loading : AttendanceUiState()
    object ScanningBle : AttendanceUiState()
    object VerifyingAcoustic : AttendanceUiState()
    object ScanningQr : AttendanceUiState()
    object VerifyingIdentity : AttendanceUiState()
    object VerifyingLocation : AttendanceUiState()
    data class Verified(
        val sessionId: String,
        val subject: String,
        val facultyName: String,
        val timestamp: LocalDateTime
    ) : AttendanceUiState()
    data class Failed(val reason: String) : AttendanceUiState()
    object AlreadyMarked : AttendanceUiState()
}

// Attendance Data Model
data class AttendanceRecord(
    val id: String,
    val subject: String,
    val date: LocalDateTime,
    val markedAt: LocalDateTime,
    val faculty: String,
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

enum class AttendanceStatus {
    PRESENT, ABSENT, LATE, LEAVE
}

// BLE Scan Result
data class BleScanResult(
    val deviceName: String,
    val address: String,
    val rssi: Int,
    val distance: Double
)

// Face Liveness Result
sealed class LivenessResult {
    object Passed : LivenessResult()
    data class Failed(val reason: String) : LivenessResult()
}

// Face Detection data
data class FaceDetectionData(
    val eyesOpen: Boolean,
    val faceCentered: Boolean,
    val isLive: Boolean,
    val headPoseZ: Float = 0f,
    val blinkRate: Float = 0f
)

// Geo Location
data class LatLng(val latitude: Double, val longitude: Double)

// Campus Boundary (polygon points)
data class CampusBoundary(val points: List<LatLng>)

// Geofence Validation
sealed class GeofenceResult {
    object InsideCampus : GeofenceResult()
    data class OutsideCampus(val distance: Double) : GeofenceResult()
    data class Error(val message: String) : GeofenceResult()
}

// Monthly attendance summary
data class MonthlySummary(
    val month: Int,
    val year: Int,
    val totalClasses: Int,
    val classesAttended: Int,
    val attendancePercentage: Float
)

// Session (for history)
data class AttendanceSession(
    val id: String,
    val subject: String,
    val date: LocalDateTime,
    val startTime: String,
    val endTime: String,
    val faculty: String,
    val attended: Boolean,
    val markedAt: LocalDateTime? = null,
    val sessionStartTimeMillis: Long = 0,
    val windowMinutes: Int = 10
)

