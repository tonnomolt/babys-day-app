package com.vpva.app.domain

import java.time.LocalTime

/**
 * Laskee päivän tapahtumat herätysajan ja konfiguraation perusteella.
 */
object ScheduleCalculator {

    fun calculate(wakeUpTime: LocalTime, config: ScheduleConfig): List<BabyEvent> {
        val events = mutableListOf<BabyEvent>()
        val bedtime = LocalTime.of(config.bedtimeHour, config.bedtimeMinute)

        // Herätys
        events.add(BabyEvent(EventType.WAKE_UP, wakeUpTime))

        // Maidot: herätysajasta alkaen tasaisin välein
        var milkTime = wakeUpTime.plusMinutes(config.milkIntervalMinutes.toLong())
        while (milkTime.isBefore(bedtime) && milkTime.isAfter(wakeUpTime)) {
            events.add(BabyEvent(EventType.MILK, milkTime))
            milkTime = milkTime.plusMinutes(config.milkIntervalMinutes.toLong())
            // Turvacheck: jos menee yli keskiyön
            if (milkTime.isBefore(wakeUpTime)) break
        }

        // Ruuat: herätysajasta alkaen tasaisin välein
        var foodTime = wakeUpTime.plusMinutes(config.foodIntervalMinutes.toLong())
        while (foodTime.isBefore(bedtime) && foodTime.isAfter(wakeUpTime)) {
            events.add(BabyEvent(EventType.FOOD, foodTime))
            foodTime = foodTime.plusMinutes(config.foodIntervalMinutes.toLong())
            if (foodTime.isBefore(wakeUpTime)) break
        }

        // Päiväunet: valveillaoloajan jälkeen
        var napStart = wakeUpTime.plusMinutes(config.awakeWindowMinutes.toLong())
        for (i in 0 until config.napsPerDay) {
            if (napStart.isAfter(bedtime) || napStart.isBefore(wakeUpTime)) break
            val napEnd = napStart.plusMinutes(config.napDurationMinutes.toLong())
            events.add(BabyEvent(EventType.NAP_START, napStart))
            if (napEnd.isBefore(bedtime)) {
                events.add(BabyEvent(EventType.NAP_END, napEnd))
                // Seuraavat unet: valveillaoloajan päästä heräämisestä
                napStart = napEnd.plusMinutes(config.awakeWindowMinutes.toLong())
            } else {
                break
            }
        }

        // Nukkumaanmeno
        events.add(BabyEvent(EventType.BEDTIME, bedtime))

        return events.sortedBy { it.time }
    }
}
