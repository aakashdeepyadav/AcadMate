package com.acadmate.attendance.geo

import android.annotation.SuppressLint
import android.content.Context
import com.acadmate.attendance.data.GeofenceResult
import com.acadmate.attendance.data.LatLng
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class GeofenceValidator(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("MissingPermission")
    suspend fun validateLocation(): GeofenceResult {
        return try {
            // 1. Fetch institution config from Firestore
            val configDoc = firestore.collection("institution").document("config").get().await()
            val centerLat = configDoc.getDouble("latitude") ?: 28.5921 // Fallback
            val centerLon = configDoc.getDouble("longitude") ?: 77.2064 // Fallback
            val radiusMeters = configDoc.getDouble("radiusMeters")?.toFloat() ?: 300f

            // 2. Get current location
            suspendCancellableCoroutine { continuation ->
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateAgeMillis(10000)
                    .build()

                fusedLocationClient.getCurrentLocation(request, null).addOnSuccessListener { location ->
                    if (location != null) {
                        val distance = haversineDistance(
                            location.latitude, 
                            location.longitude,
                            centerLat, 
                            centerLon
                        )
                        
                        val result = if (distance <= radiusMeters) {
                            GeofenceResult.InsideCampus
                        } else {
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
        } catch (e: Exception) {
            GeofenceResult.Error("Security Config Error: ${e.message}")
        }
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
