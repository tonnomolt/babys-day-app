package com.vpva.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.vpva.app.domain.BabyEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object NotificationScheduler {

    fun scheduleAll(context: Context, events: List<BabyEvent>) {
        cancelAll(context, events.size)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalTime.now()
        val today = LocalDate.now()

        events.forEachIndexed { index, event ->
            // Älä ajasta menneitä tapahtumia
            if (event.time.isBefore(now)) return@forEachIndexed

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("title", "${event.type.emoji} ${event.type.label}")
                putExtra("message", "Klo ${event.time.hour}:${event.time.minute.toString().padStart(2, '0')}")
                putExtra("notification_id", index)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerMillis = today.atTime(event.time)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } catch (_: SecurityException) {
                // Exact alarm permission not granted
            }
        }
    }

    fun cancelAll(context: Context, count: Int = 20) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until count) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
