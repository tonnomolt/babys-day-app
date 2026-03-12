package com.vpva.app.domain

/**
 * Konffattavat asetukset (minuuteissa).
 */
data class ScheduleConfig(
    val awakeWindowMinutes: Int = 120,    // Valveillaoloaika ennen päiväunia
    val napDurationMinutes: Int = 90,     // Päiväunien pituus
    val milkIntervalMinutes: Int = 180,   // Maitoväli
    val foodIntervalMinutes: Int = 240,   // Ruokaväli (ensimmäinen ruoka herätysajasta)
    val napsPerDay: Int = 2,              // Päiväunien määrä
    val bedtimeHour: Int = 20,            // Nukkumaanmeno (klo)
    val bedtimeMinute: Int = 0
)
