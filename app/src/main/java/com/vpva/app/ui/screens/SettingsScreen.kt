package com.vpva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vpva.app.data.PreferencesRepository
import com.vpva.app.domain.ScheduleConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    val config by repo.configFlow.collectAsState(initial = ScheduleConfig().normalized())
    val notificationsEnabled by repo.notificationsEnabledFlow.collectAsState(initial = true)
    val wakeTime by repo.wakeTimeFlow.collectAsState(initial = null)

    // Local mutable state
    var napsPerDay by remember(config) { mutableIntStateOf(config.napsPerDay) }
    var napDurations by remember(config) { mutableStateOf(config.napDurations.toMutableList()) }
    var napStartOffsets by remember(config) { mutableStateOf(config.napStartOffsets.toMutableList()) }
    var milkInterval by remember(config) { mutableIntStateOf(config.milkIntervalMinutes) }
    var foodInterval by remember(config) { mutableIntStateOf(config.foodIntervalMinutes) }
    var bedtimeHour by remember(config) { mutableIntStateOf(config.bedtimeHour) }
    var bedtimeMinute by remember(config) { mutableIntStateOf(config.bedtimeMinute) }

    val wakeHour = wakeTime?.hour ?: 7
    val wakeMinute = wakeTime?.minute ?: 0
    val totalDayMinutes = (bedtimeHour * 60 + bedtimeMinute) - (wakeHour * 60 + wakeMinute)

    // Keep lists in sync with napsPerDay
    fun syncNapLists(newCount: Int) {
        val durations = napDurations.toMutableList()
        val offsets = napStartOffsets.toMutableList()
        while (durations.size < newCount) durations.add(90)
        while (offsets.size < newCount) {
            val prevEnd = if (offsets.isNotEmpty())
                offsets.last() + durations[offsets.size - 1] + 60
            else 120
            offsets.add(prevEnd.coerceAtMost(totalDayMinutes - 30))
        }
        napDurations = durations.take(newCount).toMutableList()
        napStartOffsets = offsets.take(newCount).toMutableList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Asetukset") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Takaisin")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notifications toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔔 Ilmoitukset", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { repo.saveNotificationsEnabled(enabled) }
                    }
                )
            }

            HorizontalDivider()

            // === PÄIVÄUNET ===
            Text(
                "😴 Päiväunet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Nap count selector
            Text("Päiväunien määrä", style = MaterialTheme.typography.bodyMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                (1..6).forEach { count ->
                    SegmentedButton(
                        selected = napsPerDay == count,
                        onClick = {
                            napsPerDay = count
                            syncNapLists(count)
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = count - 1, count = 6
                        )
                    ) {
                        Text("$count")
                    }
                }
            }

            // Per-nap duration sliders
            for (i in 0 until napsPerDay) {
                SettingSlider(
                    label = "Uni ${i + 1} kesto",
                    value = napDurations.getOrElse(i) { 90 },
                    range = 15f..180f,
                    step = 15,
                    suffix = "min"
                ) { newVal ->
                    napDurations = napDurations.toMutableList().also { it[i] = newVal }
                }
            }

            HorizontalDivider()

            // === TIMELINE ===
            Text(
                "📅 Päivän aikajana",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Raahaa päiväunipalkkeja siirtääksesi niitä",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (totalDayMinutes > 0) {
                DayTimeline(
                    wakeHour = wakeHour,
                    wakeMinute = wakeMinute,
                    totalDayMinutes = totalDayMinutes,
                    napsPerDay = napsPerDay,
                    napDurations = napDurations,
                    napStartOffsets = napStartOffsets,
                    onOffsetChange = { index, newOffset ->
                        napStartOffsets = napStartOffsets.toMutableList().also { it[index] = newOffset }
                    }
                )
            }

            // Awake windows display
            val awakeWindows = buildAwakeWindowsList(
                totalDayMinutes, napsPerDay, napDurations, napStartOffsets
            )
            if (awakeWindows.isNotEmpty()) {
                Text(
                    "Valveillaoloikkunat",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                awakeWindows.forEachIndexed { i, mins ->
                    Text(
                        "  ${i + 1}. ${mins} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (mins < 60) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider()

            // === RUOKAILUT ===
            Text(
                "🍼 Ruokailut",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            SettingSlider(
                label = "🍼 Maitoväli",
                value = milkInterval,
                range = 60f..360f,
                step = 15,
                suffix = "min"
            ) { milkInterval = it }

            SettingSlider(
                label = "🥣 Ruokaväli",
                value = foodInterval,
                range = 120f..480f,
                step = 15,
                suffix = "min"
            ) { foodInterval = it }

            HorizontalDivider()

            // === NUKKUMAANMENO ===
            SettingSlider(
                label = "🌙 Nukkumaanmeno",
                value = bedtimeHour,
                range = 18f..23f,
                step = 1,
                suffix = ":${bedtimeMinute.toString().padStart(2, '0')}"
            ) { bedtimeHour = it }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        repo.saveConfig(
                            ScheduleConfig(
                                milkIntervalMinutes = milkInterval,
                                foodIntervalMinutes = foodInterval,
                                napsPerDay = napsPerDay,
                                napDurations = napDurations,
                                napStartOffsets = napStartOffsets,
                                bedtimeHour = bedtimeHour,
                                bedtimeMinute = bedtimeMinute
                            )
                        )
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💾 Tallenna")
            }
        }
    }
}

/**
 * Visuaalinen aikajana: herääminen → [valve] → [uni] → [valve] → ... → nukkumaanmeno
 * Unipalkkeja voi raahata horisontaalisesti.
 */
@Composable
fun DayTimeline(
    wakeHour: Int,
    wakeMinute: Int,
    totalDayMinutes: Int,
    napsPerDay: Int,
    napDurations: List<Int>,
    napStartOffsets: List<Int>,
    onOffsetChange: (Int, Int) -> Unit
) {
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${wakeHour}:${wakeMinute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall
            )
            // Midpoint
            val midMinutes = wakeHour * 60 + wakeMinute + totalDayMinutes / 2
            Text(
                "${midMinutes / 60}:${(midMinutes % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall
            )
            val endMinutes = wakeHour * 60 + wakeMinute + totalDayMinutes
            Text(
                "${endMinutes / 60}:${(endMinutes % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Timeline bar
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val barWidthPx = with(density) { maxWidth.toPx() }
            val pixelsPerMinute = if (totalDayMinutes > 0) barWidthPx / totalDayMinutes else 0f

            for (i in 0 until napsPerDay) {
                val offset = napStartOffsets.getOrElse(i) { 0 }
                val duration = napDurations.getOrElse(i) { 90 }
                val startFraction = offset.toFloat() / totalDayMinutes
                val widthFraction = duration.toFloat() / totalDayMinutes

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(widthFraction)
                        .offset(x = with(density) { (startFraction * barWidthPx).toDp() })
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        .pointerInput(i, totalDayMinutes) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                val minutesDelta = (dragAmount / pixelsPerMinute).toInt()
                                if (minutesDelta != 0) {
                                    val currentOffset = napStartOffsets.getOrElse(i) { 0 }
                                    val newOffset = (currentOffset + minutesDelta)
                                        .coerceIn(0, totalDayMinutes - duration)

                                    // Don't overlap with previous nap
                                    val minStart = if (i > 0) {
                                        napStartOffsets[i - 1] + napDurations[i - 1] + 5
                                    } else 0

                                    // Don't overlap with next nap
                                    val maxStart = if (i < napsPerDay - 1) {
                                        napStartOffsets.getOrElse(i + 1) { totalDayMinutes } - duration - 5
                                    } else totalDayMinutes - duration

                                    onOffsetChange(i, newOffset.coerceIn(minStart, maxStart))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Uni ${i + 1}\n${duration}min",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // Nap time labels under timeline
        Spacer(modifier = Modifier.height(4.dp))
        for (i in 0 until napsPerDay) {
            val offset = napStartOffsets.getOrElse(i) { 0 }
            val duration = napDurations.getOrElse(i) { 90 }
            val startMin = wakeHour * 60 + wakeMinute + offset
            val endMin = startMin + duration
            Text(
                "Uni ${i + 1}: ${startMin / 60}:${(startMin % 60).toString().padStart(2, '0')} – ${endMin / 60}:${(endMin % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Laskee valveillaoloikkunat (minuuteissa) unien väliin */
fun buildAwakeWindowsList(
    totalDayMinutes: Int,
    napsPerDay: Int,
    napDurations: List<Int>,
    napStartOffsets: List<Int>
): List<Int> {
    if (napsPerDay == 0) return listOf(totalDayMinutes)

    val windows = mutableListOf<Int>()

    // Before first nap
    windows.add(napStartOffsets.getOrElse(0) { 0 })

    // Between naps
    for (i in 0 until napsPerDay - 1) {
        val prevEnd = napStartOffsets[i] + napDurations.getOrElse(i) { 90 }
        val nextStart = napStartOffsets.getOrElse(i + 1) { totalDayMinutes }
        windows.add(nextStart - prevEnd)
    }

    // After last nap to bedtime
    val lastEnd = napStartOffsets[napsPerDay - 1] + napDurations.getOrElse(napsPerDay - 1) { 90 }
    windows.add(totalDayMinutes - lastEnd)

    return windows
}

@Composable
fun SettingSlider(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    step: Int,
    suffix: String,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                "$value $suffix",
                style = MaterialTheme.typography.titleMedium
            )
        }
        val steps = ((range.endInclusive - range.start) / step).toInt() - 1
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range,
            steps = steps.coerceAtLeast(0)
        )
    }
}
