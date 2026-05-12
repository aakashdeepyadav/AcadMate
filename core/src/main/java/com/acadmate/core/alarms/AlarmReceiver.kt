package com.acadmate.core.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val message = intent?.getStringExtra("EXTRA_MESSAGE") ?: return
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "AcadMate Reminder"
        val id = intent.getStringExtra("EXTRA_ID") ?: return
        val type = intent.getStringExtra("EXTRA_TYPE") ?: "GENERIC"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = when(type) {
            "CLASS" -> "CLASS_ALARM_CHANNEL"
            "ASSIGNMENT" -> "ASSIGNMENT_ALARM_CHANNEL"
            "NOTICE" -> "NOTICE_ALARM_CHANNEL"
            else -> "GENERIC_ALARM_CHANNEL"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = when(type) {
                "CLASS" -> "Class Reminders"
                "ASSIGNMENT" -> "Assignment Deadlines"
                "NOTICE" -> "Important Notices"
                else -> "General Reminders"
            }
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id.hashCode(), notification)
    }
}
