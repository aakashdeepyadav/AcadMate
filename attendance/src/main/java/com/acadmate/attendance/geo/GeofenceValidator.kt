package com.acadmate.attendance.geo

import android.annotation.SuppressLint
import android.content.Context
import com.acadmate.attendance.data.CampusBoundary
import com.acadmate.attendance.data.GeofenceResult
import com.acadmate.attendance.data.LatLng
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GeofenceValidator(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Default campus boundary (example - NIT Delhi)
    private val defaultCampusBoundary = CampusBoundary(
        points = listOf(
            LatLng(28.5921, 77.2064),
            LatLng(28.5925, 77.2070),
            LatLng(28.5920, 77.2075),
            LatLng(28.5915, 77.2070),
            LatLng(28.5921, 77.2064)
        )
    )

    @SuppressLint("MissingPermission")
    suspend fun validateLocation(boundary: CampusBoundary = defaultCampusBoundary): GeofenceResult {
        return suspendCancellableCoroutine { continuation ->
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(10000)
                .build()

            fusedLocationClient.getCurrentLocation(request, null).addOnSuccessListener { location ->
                if (location != null) {
                    val isInside = isPointInPolygon(
                        LatLng(location.latitude, location.longitude),
                        boundary.points
                    )
                    val result = if (isInside) {
                        GeofenceResult.InsideCampus
                    } else {
                        val distance = calculateNearestPointDistance(
                            LatLng(location.latitude, location.longitude),
                            boundary.points
                        )
                        GeofenceResult.OutsideCampus(distance)
                    }
                    continuation.resume(result)
                } else {
                    continuation.resume(GeofenceResult.Error("Location not available"))
                }
            }.addOnFailureListener { e ->
                continuation.resume(GeofenceResult.Error(e.message ?: "Location error"))
            }
        }
    }

    /**
     * Ray casting algorithm to check if point is inside polygon
     */
    private fun isPointInPolygon(point: LatLng, polygon: List<LatLng>): Boolean {
        var inside = false
        var j = polygon.size - 1

        for (i in polygon.indices) {
            val xi = polygon[i].latitude
            val yi = polygon[i].longitude
            val xj = polygon[j].latitude
            val yj = polygon[j].longitude

            val intersect = ((yi > point.longitude) != (yj > point.longitude)) &&
                    (point.latitude < (xj - xi) * (point.longitude - yi) / (yj - yi) + xi)

            if (intersect) inside = !inside
            j = i
        }

        return inside
    }

    /**
     * Calculate distance to nearest point in polygon
     */
    private fun calculateNearestPointDistance(point: LatLng, polygon: List<LatLng>): Double {
        var minDistance = Double.MAX_VALUE

        for (i in polygon.indices) {
            val j = (i + 1) % polygon.size
            val p1 = polygon[i]
            val p2 = polygon[j]

            val distance = distanceFromPointToLineSegment(point, p1, p2)
            minDistance = minOf(minDistance, distance)
        }

        return minDistance
    }

    /**
     * Calculate distance from point to line segment using Haversine formula
     */
    private fun distanceFromPointToLineSegment(point: LatLng, p1: LatLng, p2: LatLng): Double {
        val A = point.latitude - p1.latitude
        val B = point.longitude - p1.longitude
        val C = p2.latitude - p1.latitude
        val D = p2.longitude - p1.longitude

        val dot = A * C + B * D
        val lenSq = C * C + D * D
        var param = -1.0

        if (lenSq != 0.0) param = dot / lenSq

        val xx = if (param < 0) {
            p1.latitude
        } else if (param > 1) {
            p2.latitude
        } else {
            p1.latitude + param * C
        }

        val yy = if (param < 0) {
            p1.longitude
        } else if (param > 1) {
            p2.longitude
        } else {
            p1.longitude + param * D
        }

        return haversineDistance(point.latitude, point.longitude, xx, yy)
    }

    /**
     * Calculate great-circle distance between two points on Earth using Haversine formula
     * Returns distance in meters
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0  // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}

