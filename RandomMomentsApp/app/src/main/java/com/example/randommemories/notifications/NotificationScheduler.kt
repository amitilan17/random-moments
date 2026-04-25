package com.example.randommemories.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi


object NotificationScheduler {

    @RequiresApi(Build.VERSION_CODES.M)
    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Random delay: between 2 and 4 days (in milliseconds)
        val minDelayMs = 2 * 24 * 60 * 60 * 1000L
        val maxDelayMs = 4 * 24 * 60 * 60 * 1000L
        val randomDelay = (minDelayMs + (Math.random() * (maxDelayMs - minDelayMs))).toLong()

        val triggerAt = System.currentTimeMillis() + randomDelay

        // Use exact alarm for precision
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                // Permission not granted, request it from user
                val permIntent =
                    Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                permIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(permIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }
}
