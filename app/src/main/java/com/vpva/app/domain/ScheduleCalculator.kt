package com.vpva.app.domain

import java.time.LocalTime

/**
 * Laskee päivän tapahtumat herätysajan ja konfiguraation perusteella.
 */
object ScheduleCalculator {

    fun calculate(wakeUpTime: LocalTime, config: ScheduleConfig): List<BabyEvent> {
        val normalized = config.normalized()
        val events = mutableListOf<BabyEvent>()
        val bedtime = LocalTime.of(normalized.bedtimeHour, normalized.bedtimeMinute)

        // Herätys
        events.add(BabyEvent(EventType.WAKE_UP, wakeUpTime))

        // Maidot: herätysajasta alkaen tasaisin välein
        var milkTime = wakeUpTime.plusMinutes(normalized.milkIntervalMinutes.toLong())
        while (milkTime.isBefore(bedtime) && milkTime.isAfter(wakeUpTime)) {
            events.add(BabyEvent(EventType.MILK, milkTime))
            milkTime = milkTime.plusMinutes(normalized.milkIntervalMinutes.toLong())
            if (milkTime.isBefore(wakeUpTime)) break
        }

        // Ruuat: herätysajasta alkaen tasaisin välein
        var foodTime = wakeUpTime.plusMinutes(normalized.foodIntervalMinutes.toLong())
        while (foodTime.isBefore(bedtime) && foodTime.isAfter(wakeUpTime)) {
            events.add(BabyEvent(EventType.FOOD, foodTime))
            foodTime = foodTime.plusMinutes(normalized.foodIntervalMinutes.toLong())
            if (foodTime.isBefore(wakeUpTime)) break
        }

        // Päiväunet: jokaisella oma offset ja kesto
        for (i in 0 until normalized.napsPerDay) {
            val offset = normalized.napStartOffsets[i]
            val duration = normalized.napDurations[i]
            val napStart = wakeUpTime.plusMinutes(offset.toLong())

            if (napStart.isAfter(bedtime) || napStart.isBefore(wakeUpTime)) break

            val napEnd = napStart.plusMinutes(duration.toLong())
            events.add(BabyEvent(EventType.NAP_START, napStart))
            if (napEnd.isBefore(bedtime) || napEnd == bedtime) {
                events.add(BabyEvent(EventType.NAP_END, napEnd))
            } else {
                events.add(BabyEvent(EventType.NAP_END, bedtime))
            }
        }

        // Nukkumaanmeno
        events.add(BabyEvent(EventType.BEDTIME, bedtime))

        return events.sortedBy { it.time }
    }
}
