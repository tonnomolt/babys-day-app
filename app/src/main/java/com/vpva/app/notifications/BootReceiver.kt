package com.vpva.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vpva.app.data.PreferencesRepository
import com.vpva.app.domain.ScheduleCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalTime

/**
 * Palauttaa ajastetut notifikaatiot uudelleenkäynnistyksen jälkeen.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        runBlocking {
            val repo = PreferencesRepository(context)
            val wakeTime = repo.wakeTimeFlow.first() ?: return@runBlocking
            val config = repo.configFlow.first()
            val events = ScheduleCalculator.calculate(
                LocalTime.of(wakeTime.hour, wakeTime.minute), config
            )
            NotificationScheduler.scheduleAll(context, events)
        }
    }
}
