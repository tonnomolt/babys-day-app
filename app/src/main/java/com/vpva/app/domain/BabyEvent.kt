package com.vpva.app.domain

import java.time.LocalTime

enum class EventType(val emoji: String, val label: String) {
    WAKE_UP("☀️", "Heräsi"),
    MILK("🍼", "Maito"),
    FOOD("🥣", "Ruoka"),
    NAP_START("😴", "Päiväunet alkaa"),
    NAP_END("⏰", "Päiväunet loppuu"),
    BEDTIME("🌙", "Nukkumaanmeno")
}

data class BabyEvent(
    val type: EventType,
    val time: LocalTime,
    val notified: Boolean = false
)
