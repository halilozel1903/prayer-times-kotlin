package io.github.halilozel1903.prayertimes.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.halilozel1903.prayertimes.Province
import io.github.halilozel1903.prayertimes.TurkeyProvinces
import io.github.halilozel1903.prayertimes.android.PrayerReminder
import io.github.halilozel1903.prayertimes.android.PrayerReminderScheduler
import io.github.halilozel1903.prayertimes.sample.reminders.ReminderReceiver
import io.github.halilozel1903.prayertimes.sample.reminders.Reminders
import kotlinx.coroutines.launch

/** Language (Türkçe, English or the system's), prayer reminders and a note on the data. */
@Composable
fun SettingsScreen(province: Province, graph: AppGraph) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheduler = remember { PrayerReminderScheduler(context, ReminderReceiver::class.java) }
    var language by remember { mutableStateOf(AppLanguage.current()) }
    var enabled by remember { mutableStateOf(graph.settings.remindersEnabled) }
    var minutesBefore by remember { mutableIntStateOf(graph.settings.minutesBefore) }
    var canExact by remember { mutableStateOf(scheduler.canScheduleExactAlarms()) }
    var scheduled by remember { mutableStateOf<List<PrayerReminder>>(emptyList()) }

    fun refresh() {
        scope.launch { scheduled = runCatching { Reminders.refresh(context) }.getOrDefault(emptyList()) }
    }

    val exactAlarmSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        canExact = scheduler.canScheduleExactAlarms()
        refresh()
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled = granted
        graph.settings.remindersEnabled = granted
        refresh()
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section(stringResource(R.string.settings_language)) {
                AppLanguage.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                language = option
                                AppLanguage.apply(option)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = language == option, onClick = null, modifier = Modifier.padding(12.dp))
                        Text(
                            stringResource(
                                when (option) {
                                    AppLanguage.System -> R.string.language_system
                                    AppLanguage.Turkish -> R.string.language_turkish
                                    AppLanguage.English -> R.string.language_english
                                },
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            Section(stringResource(R.string.settings_reminders)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.reminders_description, province.name),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { on ->
                            val needsPermission = on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                            if (needsPermission) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                enabled = on
                                graph.settings.remindersEnabled = on
                                refresh()
                            }
                        },
                    )
                }
                if (enabled) {
                    Text(stringResource(R.string.reminders_offset), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 5, 10, 15).forEach { minutes ->
                            FilterChip(
                                selected = minutesBefore == minutes,
                                onClick = {
                                    minutesBefore = minutes
                                    graph.settings.minutesBefore = minutes
                                    refresh()
                                },
                                label = {
                                    Text(
                                        if (minutes == 0) {
                                            stringResource(R.string.reminders_at_time)
                                        } else {
                                            stringResource(R.string.reminders_minutes_before, minutes)
                                        },
                                    )
                                },
                            )
                        }
                    }
                    if (!canExact) {
                        Text(stringResource(R.string.reminders_exact_needed), style = MaterialTheme.typography.bodySmall)
                        FilledTonalButton(onClick = { scheduler.exactAlarmSettingsIntent()?.let { exactAlarmSettings.launch(it) } }) {
                            Text(stringResource(R.string.reminders_allow))
                        }
                    }
                    scheduled.minByOrNull { it.time }?.let { first ->
                        Text(
                            stringResource(
                                R.string.reminders_next,
                                stringResource(first.prayer.label),
                                first.time.atZone(TurkeyProvinces.zone).clock(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Section(stringResource(R.string.settings_about)) {
                Text(stringResource(R.string.settings_about_text), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
