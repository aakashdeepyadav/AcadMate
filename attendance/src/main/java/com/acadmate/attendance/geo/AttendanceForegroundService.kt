package com.acadmate.attendance.geo

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.acadmate.attendance.data.GeofenceResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AttendanceForegroundService : Service() {

    @Inject
    lateinit var geofenceValidator: GeofenceValidator

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pingJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastReauthTime = System.currentTimeMillis()

    companion object {
        const val CHANNEL_ID = "attendance_service_channel"
        const val NOTIFICATION_ID = 1001
        const val REAUTH_NOTIFICATION_ID = 1002
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_REAUTH_COMPLETE = "ACTION_REAUTH_COMPLETE"
        const val EXTRA_SUBJECT = "EXTRA_SUBJECT"
        const val REAUTH_INTERVAL = 30 * 60 * 1000L // 30 minutes
        const val ACTION_REAUTH_REQUIRED = "com.acadmate.attendance.ACTION_REAUTH_REQUIRED"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AcadMate:AttendancePingLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "Class"
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
                startForegroundService(subject)
                startPinging(subject, userId)
            }
            ACTION_STOP -> {
                stopPinging()
                stopSelf()
            }
            ACTION_REAUTH_COMPLETE -> {
                lastReauthTime = System.currentTimeMillis()
                val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "Class"
                updateNotification(subject, "Re-authentication successful")
            }
        }
        return START_STICKY
    }

    private fun startForegroundService(subject: String) {
        val notification = createNotification("Verifying presence for $subject...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Attendance Active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun startPinging(subject: String, userId: String) {
        pingJob?.cancel()
        pingJob = serviceScope.launch {
            while (isActive) {
                try {
                    wakeLock?.acquire(10 * 1000L)
                    val result = geofenceValidator.validateLocation()
                    
                    val statusText = when (result) {
                        is GeofenceResult.InsideCampus -> "Status: Safe inside campus"
                        is GeofenceResult.OutsideCampus -> "Warning: Outside campus (${String.format(Locale.getDefault(), "%.0f", result.distance)}m)"
                        is GeofenceResult.Error -> "Status: Location unavailable"
                    }
                    
                    if (System.currentTimeMillis() - lastReauthTime > REAUTH_INTERVAL) {
                        requestBiometricReauth(subject)
                    } else {
                        updateNotification(subject, statusText)
                    }

                    if (result is GeofenceResult.OutsideCampus) {
                        reportAnomaly(userId, subject, "Left campus during class: ${result.distance}m away")
                    }
                } catch (e: Exception) { } 
                finally {
                    if (wakeLock?.isHeld == true) wakeLock?.release()
                }
                delay(300000) // 5 minutes
            }
        }
    }

    private fun requestBiometricReauth(subject: String) {
        val intent = Intent(ACTION_REAUTH_REQUIRED).apply {
            putExtra(EXTRA_SUBJECT, subject)
            setPackage(packageName)
        }
        sendBroadcast(intent)

        val notificationManager = getSystemService(NotificationManager::class.java)
        val reauthNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Re-authentication Required")
            .setContentText("Please verify your presence for $subject")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(REAUTH_NOTIFICATION_ID, reauthNotification)
    }

    private fun reportAnomaly(userId: String, subject: String, reason: String) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val anomalyData = hashMapOf(
            "userId" to userId,
            "subject" to subject,
            "reason" to reason,
            "timestamp" to System.currentTimeMillis(),
            "type" to "GEOFENCE_VIOLATION"
        )
        
        serviceScope.launch {
            try {
                firestore.collection("active_sessions")
                    .document(subject)
                    .collection("anomalies")
                    .document(userId)
                    .set(anomalyData)
            } catch (e: Exception) { }
        }
    }

    private fun updateNotification(subject: String, statusText: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification("$subject - $statusText"))
    }

    private fun stopPinging() {
        pingJob?.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Attendance Verification",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors location during active lectures"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPinging()
        serviceScope.cancel()
        super.onDestroy()
    }
}
