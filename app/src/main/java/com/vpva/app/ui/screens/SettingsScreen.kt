package com.vpva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlin.math.abs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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

    // Mutable state
    var napsPerDay by remember(config) { mutableIntStateOf(config.napsPerDay) }
    var napDurations by remember(config) { mutableStateOf(config.napDurations) }
    var napStartOffsets by remember(config) { mutableStateOf(config.napStartOffsets) }
    var milksPerDay by remember(config) { mutableIntStateOf(config.milksPerDay) }
    var milkOffsets by remember(config) { mutableStateOf(config.milkOffsets) }
    var mealsPerDay by remember(config) { mutableIntStateOf(config.mealsPerDay) }
    var mealOffsets by remember(config) { mutableStateOf(config.mealOffsets) }
    var bedtimeHour by remember(config) { mutableIntStateOf(config.bedtimeHour) }
    var bedtimeMinute by remember(config) { mutableIntStateOf(config.bedtimeMinute) }

    val wakeHour = wakeTime?.hour ?: 7
    val wakeMinute = wakeTime?.minute ?: 0
    val totalDayMinutes = (bedtimeHour * 60 + bedtimeMinute) - (wakeHour * 60 + wakeMinute)

    fun syncList(current: List<Int>, newCount: Int, defaultGap: Int): List<Int> {
        val list = current.toMutableList()
        while (list.size < newCount) {
            val prev = if (list.isNotEmpty()) list.last() + defaultGap else 0
            list.add(prev.coerceAtMost(totalDayMinutes.coerceAtLeast(0)))
        }
        return list.take(newCount).sorted()
    }

    fun syncNaps(newCount: Int) {
        val durations = napDurations.toMutableList()
        while (durations.size < newCount) durations.add(90)
        napDurations = durations.take(newCount)
        napStartOffsets = syncList(napStartOffsets, newCount, 150)
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

            // ====== PÄIVÄUNET ======
            Text("😴 Päiväunet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            CountSelector(label = "Päiväunien määrä", count = napsPerDay, range = 1..6) {
                napsPerDay = it; syncNaps(it)
            }

            for (i in 0 until napsPerDay) {
                SettingSlider(
                    label = "Uni ${i + 1} kesto",
                    value = napDurations.getOrElse(i) { 90 },
                    range = 15f..180f, step = 15, suffix = "min"
                ) { v -> napDurations = napDurations.toMutableList().also { it[i] = v } }
            }

            HorizontalDivider()

            // ====== MAIDOT ======
            Text("🍼 Maidot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            CountSelector(label = "Maitoja päivässä", count = milksPerDay, range = 1..8) {
                milksPerDay = it; milkOffsets = syncList(milkOffsets, it, 120)
            }

            HorizontalDivider()

            // ====== RUUAT ======
            Text("🥣 Ruuat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            CountSelector(label = "Ruokailuja päivässä", count = mealsPerDay, range = 1..6) {
                mealsPerDay = it; mealOffsets = syncList(mealOffsets, it, 150)
            }

            HorizontalDivider()

            // ====== NUKKUMAANMENO ======
            SettingSlider(
                label = "🌙 Nukkumaanmeno",
                value = bedtimeHour, range = 18f..23f, step = 1,
                suffix = ":${bedtimeMinute.toString().padStart(2, '0')}"
            ) { bedtimeHour = it }

            HorizontalDivider()

            // ====== AIKAJANA ======
            Text("📅 Päivän aikajana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Raahaa palkkeja ja merkkejä siirtääksesi niitä aikajanalla",
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
                    milkOffsets = milkOffsets,
                    mealOffsets = mealOffsets,
                    onNapOffsetChange = { i, v ->
                        napStartOffsets = napStartOffsets.toMutableList().also { it[i] = v }
                    },
                    onMilkOffsetChange = { i, v ->
                        milkOffsets = milkOffsets.toMutableList().also { it[i] = v }
                    },
                    onMealOffsetChange = { i, v ->
                        mealOffsets = mealOffsets.toMutableList().also { it[i] = v }
                    }
                )
            }

            // Awake windows
            val awakeWindows = buildAwakeWindowsList(totalDayMinutes, napsPerDay, napDurations, napStartOffsets)
            if (awakeWindows.isNotEmpty()) {
                Text("Valveillaoloikkunat", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                awakeWindows.forEachIndexed { i, mins ->
                    Text(
                        "  ${i + 1}. $mins min",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (mins < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        repo.saveConfig(
                            ScheduleConfig(
                                napsPerDay = napsPerDay,
                                napDurations = napDurations,
                                napStartOffsets = napStartOffsets,
                                milksPerDay = milksPerDay,
                                milkOffsets = milkOffsets,
                                mealsPerDay = mealsPerDay,
                                mealOffsets = mealOffsets,
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

// ====== COMPONENTS ======

@Composable
fun CountSelector(label: String, count: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            range.forEach { n ->
                FilterChip(
                    selected = count == n,
                    onClick = { onChange(n) },
                    label = { Text("$n") },
                    modifier = Modifier.height(32.dp)
                )
            }
        }
    }
}

@Composable
fun DayTimeline(
    wakeHour: Int,
    wakeMinute: Int,
    totalDayMinutes: Int,
    napsPerDay: Int,
    napDurations: List<Int>,
    napStartOffsets: List<Int>,
    milkOffsets: List<Int>,
    mealOffsets: List<Int>,
    onNapOffsetChange: (Int, Int) -> Unit,
    onMilkOffsetChange: (Int, Int) -> Unit,
    onMealOffsetChange: (Int, Int) -> Unit
) {
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // Time labels
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            fun fmtTime(totalMin: Int) = "${totalMin / 60}:${(totalMin % 60).toString().padStart(2, '0')}"
            val wakeMin = wakeHour * 60 + wakeMinute
            Text(fmtTime(wakeMin), style = MaterialTheme.typography.labelSmall)
            Text(fmtTime(wakeMin + totalDayMinutes / 2), style = MaterialTheme.typography.labelSmall)
            Text(fmtTime(wakeMin + totalDayMinutes), style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Timeline
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val barWidthPx = with(density) { maxWidth.toPx() }
            val pxPerMin = if (totalDayMinutes > 0) barWidthPx / totalDayMinutes else 0f

            // Nap blocks (full height, background)
            for (i in 0 until napsPerDay) {
                val offset = napStartOffsets.getOrElse(i) { 0 }
                val duration = napDurations.getOrElse(i) { 90 }

                DraggableBlock(
                    offsetMinutes = offset,
                    widthMinutes = duration,
                    pxPerMin = pxPerMin,
                    totalDayMinutes = totalDayMinutes,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    label = "😴 ${duration}m",
                    height = 80.dp,
                    onOffsetChange = { onNapOffsetChange(i, it) }
                )
            }

            // Milk markers (top row)
            milkOffsets.forEachIndexed { i, offset ->
                DraggableMarker(
                    offsetMinutes = offset,
                    pxPerMin = pxPerMin,
                    totalDayMinutes = totalDayMinutes,
                    emoji = "🍼",
                    color = MaterialTheme.colorScheme.tertiary,
                    yOffset = 2.dp,
                    onOffsetChange = { onMilkOffsetChange(i, it) }
                )
            }

            // Meal markers (bottom row)
            mealOffsets.forEachIndexed { i, offset ->
                DraggableMarker(
                    offsetMinutes = offset,
                    pxPerMin = pxPerMin,
                    totalDayMinutes = totalDayMinutes,
                    emoji = "🥣",
                    color = MaterialTheme.colorScheme.secondary,
                    yOffset = 50.dp,
                    onOffsetChange = { onMealOffsetChange(i, it) }
                )
            }
        }

        // Legend
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem("😴 Uni", MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            LegendItem("🍼 Maito", MaterialTheme.colorScheme.tertiary)
            LegendItem("🥣 Ruoka", MaterialTheme.colorScheme.secondary)
        }

        // Time labels for each event
        Spacer(modifier = Modifier.height(8.dp))
        val wakeMin = wakeHour * 60 + wakeMinute
        for (i in 0 until napsPerDay) {
            val startMin = wakeMin + napStartOffsets.getOrElse(i) { 0 }
            val endMin = startMin + napDurations.getOrElse(i) { 90 }
            Text(
                "😴 Uni ${i + 1}: ${fmtClock(startMin)} – ${fmtClock(endMin)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        milkOffsets.forEachIndexed { i, offset ->
            Text("🍼 Maito ${i + 1}: ${fmtClock(wakeMin + offset)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary)
        }
        mealOffsets.forEachIndexed { i, offset ->
            Text("🥣 Ruoka ${i + 1}: ${fmtClock(wakeMin + offset)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun BoxWithConstraintsScope.DraggableBlock(
    offsetMinutes: Int,
    widthMinutes: Int,
    pxPerMin: Float,
    totalDayMinutes: Int,
    color: Color,
    label: String,
    height: Dp,
    onOffsetChange: (Int) -> Unit
) {
    val density = LocalDensity.current
    var dragAccum by remember { mutableFloatStateOf(0f) }

    val startDp = with(density) { (offsetMinutes * pxPerMin).toDp() }
    val widthDp = with(density) { (widthMinutes * pxPerMin).toDp() }

    Box(
        modifier = Modifier
            .height(height)
            .width(widthDp)
            .offset(x = startDp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .pointerInput(offsetMinutes, totalDayMinutes) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onDragEnd = { dragAccum = 0f },
                    onDragCancel = { dragAccum = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragAccum += amount
                        val snapPx = pxPerMin * 5
                        if (snapPx > 0 && abs(dragAccum) >= snapPx) {
                            val steps = (dragAccum / snapPx).toInt()
                            dragAccum -= steps * snapPx
                            val newOffset = (offsetMinutes + steps * 5)
                                .coerceIn(0, totalDayMinutes - widthMinutes)
                            onOffsetChange(newOffset)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun BoxWithConstraintsScope.DraggableMarker(
    offsetMinutes: Int,
    pxPerMin: Float,
    totalDayMinutes: Int,
    emoji: String,
    color: Color,
    yOffset: Dp,
    onOffsetChange: (Int) -> Unit
) {
    val density = LocalDensity.current
    var dragAccum by remember { mutableFloatStateOf(0f) }

    val xDp = with(density) { (offsetMinutes * pxPerMin).toDp() } - 12.dp

    Box(
        modifier = Modifier
            .offset(x = xDp.coerceAtLeast(0.dp), y = yOffset)
            .size(28.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.9f))
            .pointerInput(offsetMinutes, totalDayMinutes) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onDragEnd = { dragAccum = 0f },
                    onDragCancel = { dragAccum = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragAccum += amount
                        val snapPx = pxPerMin * 5
                        if (snapPx > 0 && abs(dragAccum) >= snapPx) {
                            val steps = (dragAccum / snapPx).toInt()
                            dragAccum -= steps * snapPx
                            val newOffset = (offsetMinutes + steps * 5)
                                .coerceIn(0, totalDayMinutes)
                            onOffsetChange(newOffset)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 14.sp)
    }
}

@Composable
fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

fun fmtClock(totalMinutes: Int): String =
    "${totalMinutes / 60}:${(totalMinutes % 60).toString().padStart(2, '0')}"

fun buildAwakeWindowsList(
    totalDayMinutes: Int, napsPerDay: Int, napDurations: List<Int>, napStartOffsets: List<Int>
): List<Int> {
    if (napsPerDay == 0) return listOf(totalDayMinutes)
    val windows = mutableListOf<Int>()
    windows.add(napStartOffsets.getOrElse(0) { 0 })
    for (i in 0 until napsPerDay - 1) {
        val prevEnd = napStartOffsets[i] + napDurations.getOrElse(i) { 90 }
        windows.add(napStartOffsets.getOrElse(i + 1) { totalDayMinutes } - prevEnd)
    }
    val lastEnd = napStartOffsets[napsPerDay - 1] + napDurations.getOrElse(napsPerDay - 1) { 90 }
    windows.add(totalDayMinutes - lastEnd)
    return windows
}

@Composable
fun SettingSlider(
    label: String, value: Int, range: ClosedFloatingPointRange<Float>,
    step: Int, suffix: String, onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text("$value $suffix", style = MaterialTheme.typography.titleMedium)
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
