package com.acadmate.core.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import androidx.core.net.toUri

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var onboardingDataStore: com.acadmate.core.datastore.OnboardingDataStore

    override fun onReceive(context: Context, intent: Intent?) {
        val message = intent?.getStringExtra("EXTRA_MESSAGE") ?: return
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "AcadMate Reminder"
        val id = intent.getStringExtra("EXTRA_ID") ?: return
        val type = intent.getStringExtra("EXTRA_TYPE") ?: "GENERIC"

        val settings = try {
            runBlocking {
                val vibe = onboardingDataStore.alarmVibrationEnabled.first()
                val vol = onboardingDataStore.alarmVolume.first()
                val tone = onboardingDataStore.alarmTone.first()
                Triple(vibe, vol, tone)
            }
        } catch (_: Exception) {
            Triple(true, 70, "Default")
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val ringtoneUri = if (settings.third == "Default") {
            android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
        } else {
            settings.third.toUri()
        }

        val channelId = when(type) {
            "CLASS" -> "CLASS_ALARM_CHANNEL"
            "ASSIGNMENT" -> "ASSIGNMENT_ALARM_CHANNEL"
            "NOTICE" -> "NOTICE_ALARM_CHANNEL"
            else -> "GENERIC_ALARM_CHANNEL"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = when(type) {
                "CLASS" -> "Class Alarms"
                "ASSIGNMENT" -> "Assignment Deadlines"
                "NOTICE" -> "Important Notices"
                else -> "General Reminders"
            }
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            // We recreate the channel if it's a CLASS alarm to update the sound if changed
            // Actually, once created, sound cannot be changed easily without deleting channel.
            // But for this purpose, let's ensure it's set correctly.
            val channel = NotificationChannel(channelId, name, importance).apply {
                enableVibration(settings.first)
                if (type == "CLASS") {
                    setSound(
                        ringtoneUri,
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (type == "CLASS") NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setSound(ringtoneUri)
            .setVibrate(if (settings.first) longArrayOf(0, 500, 200, 500) else null)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id.hashCode(), notification)
    }
}
