package com.vpva.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class BabyDayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Vauvan Päivä",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Muistutukset vauvan päivärytmistä"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "baby_day_notifications"
    }
}
