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
        saveNextScheduledTime(context, triggerAt)

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

    @RequiresApi(Build.VERSION_CODES.M)
    fun scheduleSnooze(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        // Random snooze delay: between 2h and 6h
        val minSnoozeMs = 2 * 60 * 60 * 1000L
        val maxSnoozeMs = 6 * 60 * 60 * 1000L
        val snoozeDelay = (minSnoozeMs + (Math.random() * (maxSnoozeMs - minSnoozeMs))).toLong()
        val snoozeAt = System.currentTimeMillis() + snoozeDelay

        // Check when the next scheduled notification fires
        val nextScheduledAt = getNextScheduledTime(context)

        // If snooze would fire within 5h before the next notification, skip it
        val bufferMs = 5 * 60 * 60 * 1000L
        if (nextScheduledAt != null && snoozeAt >= nextScheduledAt - bufferMs) {
            return // Too close to next scheduled notification, don't snooze
        }

        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1002, intent, // Use a different request code than scheduleNext (1001)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeAt, pendingIntent)
            } else {
                val permIntent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                permIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(permIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeAt, pendingIntent)
        }
    }

    // Call this inside scheduleNext(), right after computing triggerAt:
    fun saveNextScheduledTime(context: Context, triggerAt: Long) {
        context.getSharedPreferences("notif_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("next_scheduled_at", triggerAt)
            .apply()
    }

    private fun getNextScheduledTime(context: Context): Long? {
        val value = context.getSharedPreferences("notif_prefs", Context.MODE_PRIVATE)
            .getLong("next_scheduled_at", -1L)
        return if (value == -1L) null else value
    }
}
