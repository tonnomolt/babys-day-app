package com.vpva.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vpva.app.data.PreferencesRepository
import com.vpva.app.domain.ScheduleConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    val config by repo.configFlow.collectAsState(initial = ScheduleConfig())

    var awakeWindow by remember(config) { mutableIntStateOf(config.awakeWindowMinutes) }
    var napDuration by remember(config) { mutableIntStateOf(config.napDurationMinutes) }
    var milkInterval by remember(config) { mutableIntStateOf(config.milkIntervalMinutes) }
    var foodInterval by remember(config) { mutableIntStateOf(config.foodIntervalMinutes) }
    var napsPerDay by remember(config) { mutableIntStateOf(config.napsPerDay) }
    var bedtimeHour by remember(config) { mutableIntStateOf(config.bedtimeHour) }
    var bedtimeMinute by remember(config) { mutableIntStateOf(config.bedtimeMinute) }

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
            SettingSlider(
                label = "☀️ Valveillaoloaika",
                value = awakeWindow,
                range = 60f..240f,
                step = 15,
                suffix = "min"
            ) { awakeWindow = it }

            SettingSlider(
                label = "😴 Päiväunien pituus",
                value = napDuration,
                range = 30f..180f,
                step = 15,
                suffix = "min"
            ) { napDuration = it }

            SettingSlider(
                label = "😴 Päiväunien määrä",
                value = napsPerDay,
                range = 0f..4f,
                step = 1,
                suffix = "kpl"
            ) { napsPerDay = it }

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
                                awakeWindowMinutes = awakeWindow,
                                napDurationMinutes = napDuration,
                                milkIntervalMinutes = milkInterval,
                                foodIntervalMinutes = foodInterval,
                                napsPerDay = napsPerDay,
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
