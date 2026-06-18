package com.example.randommemories.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Calendar

object NotificationScheduler {

    private const val NIGHT_START_HOUR = 23
    private const val NIGHT_START_MINUTE = 30
    private const val NIGHT_END_HOUR = 8
    private const val NIGHT_END_MINUTE = 0

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

        var triggerAt: Long

        do {
            val randomDelay =
                (minDelayMs + (Math.random() * (maxDelayMs - minDelayMs))).toLong()
            triggerAt = System.currentTimeMillis() + randomDelay
        } while (isNightTime(triggerAt))

        saveNextScheduledTime(context, triggerAt)

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

        val now = System.currentTimeMillis()

        val originalMin = now + 2 * 60 * 60 * 1000L // +2h
        val originalMax = now + 6 * 60 * 60 * 1000L // +6h

        val validRanges = getNonNightRanges(originalMin, originalMax)

        val totalDuration = validRanges.sumOf { it.second - it.first }

        // Skip snooze if less than 30 minutes of valid daytime remain
        if (totalDuration < 30 * 60 * 1000L) {
            return
        }

        val randomOffset = (Math.random() * totalDuration).toLong()

        var accumulated = 0L
        var snoozeAt = validRanges.first().first

        for ((start, end) in validRanges) {
            val duration = end - start
            if (randomOffset < accumulated + duration) {
                snoozeAt = start + (randomOffset - accumulated)
                break
            }
            accumulated += duration
        }

        val nextScheduledAt = getNextScheduledTime(context)

        // Check when the next scheduled notification fires
        val bufferMs = 5 * 60 * 60 * 1000L
        if (nextScheduledAt != null && snoozeAt >= nextScheduledAt - bufferMs) {
            return // Too close to next scheduled notification, don't snooze
        }

        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002, // Use a different request code than scheduleNext (1001)
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeAt,
                    pendingIntent
                )
            } else {
                val permIntent =
                    Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                permIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(permIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozeAt,
                pendingIntent
            )
        }
    }

    private fun isNightTime(timestamp: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp

        val minutes =
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val nightStart = NIGHT_START_HOUR * 60 + NIGHT_START_MINUTE // 23:30
        val nightEnd = NIGHT_END_HOUR * 60 + NIGHT_END_MINUTE // 08:00

        return minutes >= nightStart || minutes < nightEnd
    }

    /**
     * Returns all daytime intervals inside [start,end].
     */
    private fun getNonNightRanges(start: Long, end: Long): List<Pair<Long, Long>> {
        val result = mutableListOf<Pair<Long, Long>>()

        var cursor = start

        while (cursor < end) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = cursor

            val dayStart = cal.clone() as Calendar
            dayStart.set(Calendar.HOUR_OF_DAY, NIGHT_END_HOUR)
            dayStart.set(Calendar.MINUTE, NIGHT_END_MINUTE)
            dayStart.set(Calendar.SECOND, 0)
            dayStart.set(Calendar.MILLISECOND, 0)

            val nightStart = cal.clone() as Calendar
            nightStart.set(Calendar.HOUR_OF_DAY, NIGHT_START_HOUR)
            nightStart.set(Calendar.MINUTE, NIGHT_START_MINUTE)
            nightStart.set(Calendar.SECOND, 0)
            nightStart.set(Calendar.MILLISECOND, 0)

            val rangeStart = maxOf(cursor, dayStart.timeInMillis)
            val rangeEnd = minOf(end, nightStart.timeInMillis)

            if (rangeEnd > rangeStart) {
                result.add(rangeStart to rangeEnd)
            }

            val nextDay = nightStart.clone() as Calendar
            nextDay.add(Calendar.DAY_OF_MONTH, 1)
            nextDay.set(Calendar.HOUR_OF_DAY, NIGHT_END_HOUR)
            nextDay.set(Calendar.MINUTE, NIGHT_END_MINUTE)

            cursor = nextDay.timeInMillis
        }

        return result
    }

    // Call this inside scheduleNext(), right after computing triggerAt:
    private fun saveNextScheduledTime(context: Context, triggerAt: Long) {
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