package com.vpva.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vpva.app.data.PreferencesRepository
import com.vpva.app.domain.BabyEvent
import com.vpva.app.domain.EventType
import com.vpva.app.domain.ScheduleCalculator
import com.vpva.app.domain.ScheduleConfig
import com.vpva.app.notifications.NotificationScheduler
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    val wakeTime by repo.wakeTimeFlow.collectAsState(initial = null)
    val config by repo.configFlow.collectAsState(initial = ScheduleConfig())

    var showTimePicker by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf<List<BabyEvent>>(emptyList()) }

    // Request notification permission (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Recalculate when wake time or config changes
    LaunchedEffect(wakeTime, config) {
        wakeTime?.let { wt ->
            events = ScheduleCalculator.calculate(
                LocalTime.of(wt.hour, wt.minute), config
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🍼 Vauvan Päivä") },
                actions = {
                    TextButton(onClick = onNavigateToSettings) {
                        Text("⚙️ Asetukset")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Wake time button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Vauva heräsi",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (wakeTime != null) {
                        Text(
                            "${wakeTime!!.hour}:${wakeTime!!.minute.toString().padStart(2, '0')}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { showTimePicker = true }) {
                        Text(if (wakeTime != null) "Vaihda herätysaika" else "☀️ Aseta herätysaika")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Schedule
            if (events.isNotEmpty()) {
                Text(
                    "Päivän aikataulu",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(events) { event ->
                        EventRow(event)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    "Aseta herätysaika aloittaaksesi ☀️",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = wakeTime?.hour ?: 7,
            initialMinute = wakeTime?.minute ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerState.hour
                    val m = timePickerState.minute
                    scope.launch {
                        repo.saveWakeTime(h, m)
                        val newEvents = ScheduleCalculator.calculate(
                            LocalTime.of(h, m), config
                        )
                        events = newEvents
                        NotificationScheduler.scheduleAll(context, newEvents)
                    }
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Peruuta") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@Composable
fun EventRow(event: BabyEvent) {
    val now = LocalTime.now()
    val isPast = event.time.isBefore(now)
    val alpha = if (isPast) 0.5f else 1.0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.type) {
                EventType.NAP_START, EventType.NAP_END -> MaterialTheme.colorScheme.secondaryContainer
                EventType.MILK -> MaterialTheme.colorScheme.tertiaryContainer
                EventType.FOOD -> MaterialTheme.colorScheme.surfaceVariant
                EventType.BEDTIME -> MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.1f)
                EventType.WAKE_UP -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                event.type.emoji,
                fontSize = 24.sp,
                modifier = Modifier.alpha(alpha)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "${event.time.hour}:${event.time.minute.toString().padStart(2, '0')}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alpha)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                event.type.label,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}
