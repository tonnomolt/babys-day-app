package com.vpva.app.domain

import java.time.LocalTime

object ScheduleCalculator {

    fun calculate(wakeUpTime: LocalTime, config: ScheduleConfig): List<BabyEvent> {
        val normalized = config.normalized()
        val events = mutableListOf<BabyEvent>()
        val bedtime = LocalTime.of(normalized.bedtimeHour, normalized.bedtimeMinute)

        events.add(BabyEvent(EventType.WAKE_UP, wakeUpTime))

        // Maidot
        for (offset in normalized.milkOffsets) {
            val t = wakeUpTime.plusMinutes(offset.toLong())
            if (t.isBefore(bedtime) && !t.isBefore(wakeUpTime)) {
                events.add(BabyEvent(EventType.MILK, t))
            }
        }

        // Ruuat
        for (offset in normalized.mealOffsets) {
            val t = wakeUpTime.plusMinutes(offset.toLong())
            if (t.isBefore(bedtime) && !t.isBefore(wakeUpTime)) {
                events.add(BabyEvent(EventType.FOOD, t))
            }
        }

        // Päiväunet
        for (i in 0 until normalized.napsPerDay) {
            val offset = normalized.napStartOffsets[i]
            val duration = normalized.napDurations[i]
            val napStart = wakeUpTime.plusMinutes(offset.toLong())

            if (napStart.isAfter(bedtime) || napStart.isBefore(wakeUpTime)) break

            val napEnd = napStart.plusMinutes(duration.toLong())
            events.add(BabyEvent(EventType.NAP_START, napStart))
            events.add(BabyEvent(EventType.NAP_END,
                if (napEnd.isBefore(bedtime) || napEnd == bedtime) napEnd else bedtime
            ))
        }

        events.add(BabyEvent(EventType.BEDTIME, bedtime))

        return events.sortedBy { it.time }
    }
}
