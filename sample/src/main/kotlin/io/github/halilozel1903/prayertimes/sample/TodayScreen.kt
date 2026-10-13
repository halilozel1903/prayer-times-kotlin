package io.github.halilozel1903.prayertimes.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.prayertimes.NextPrayer
import io.github.halilozel1903.prayertimes.PrayerSchedule
import io.github.halilozel1903.prayertimes.PrayerTime
import io.github.halilozel1903.prayertimes.Province
import io.github.halilozel1903.prayertimes.TurkeyProvinces
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Ticks every second, on the second. */
@Composable
fun rememberNow(): Instant {
    val now by produceState(Instant.now()) {
        while (true) {
            delay(1_000 - System.currentTimeMillis() % 1_000)
            value = Instant.now()
        }
    }
    return now
}

/** Today's six times in [province] with the next prayer and a live countdown. */
@Composable
fun TodayScreen(province: Province, repository: PrayerRepository, onChangeCity: (() -> Unit)?) {
    var reload by remember { mutableIntStateOf(0) }
    var date by remember { mutableStateOf(PrayerRepository.today()) }
    val state by produceState<Load<PrayerSchedule>>(Load.Loading, province, date, reload) {
        value = Load.Loading
        value = load { repository.schedule(province, date) }
    }
    ReportLoad(state)
    val now = rememberNow()
    // A new day: load its times.
    LaunchedEffect(now) {
        val today = now.atZone(TurkeyProvinces.zone).toLocalDate()
        if (today != date) date = today
    }

    when (val current = state) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(current, onRetry = { reload++ })
        is Load.Ready -> TodayContent(province, current.value, date, now, onChangeCity)
    }
}

@Composable
private fun TodayContent(
    province: Province,
    schedule: PrayerSchedule,
    date: LocalDate,
    now: Instant,
    onChangeCity: (() -> Unit)?,
) {
    val locale = appLocale()
    val today = schedule.days.firstOrNull { it.date == date } ?: schedule.days.first()
    val next = schedule.next(now)
    val current = schedule.current(now)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        province.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", locale).format(date),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        today.hijri.format(locale.language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onChangeCity != null) {
                    AssistChip(
                        onClick = onChangeCity,
                        label = { Text(stringResource(R.string.change_city)) },
                        leadingIcon = {
                            PinIcon(MaterialTheme.colorScheme.primary, Modifier.size(AssistChipDefaults.IconSize))
                        },
                    )
                }
            }

            if (next != null) CountdownCard(next, date)

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    today.times.forEach { time ->
                        PrayerRow(
                            time = time,
                            isCurrent = current == time,
                            isNext = next?.next == time,
                            isPast = !time.instant.isAfter(now) && current != time,
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.method_note, province.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CountdownCard(next: NextPrayer, today: LocalDate) {
    val name = stringResource(next.next.prayer.label)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.next_prayer), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrayerGlyph(next.next.prayer, MaterialTheme.colorScheme.onPrimary, Modifier.size(32.dp))
                Spacer(Modifier.size(12.dp))
                Text(name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(next.next.time.clock(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    if (next.next.time.toLocalDate() != today) {
                        Text(stringResource(R.string.tomorrow), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                next.countdown,
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(R.string.time_until, name), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { next.progress ?: 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
            )
            next.current?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.current_prayer, stringResource(it.prayer.label)),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun PrayerRow(time: PrayerTime, isCurrent: Boolean, isNext: Boolean, isPast: Boolean) {
    val colors = MaterialTheme.colorScheme
    val content = when {
        isCurrent -> colors.onSecondaryContainer
        isNext -> colors.primary
        isPast -> colors.onSurfaceVariant.copy(alpha = 0.7f)
        else -> colors.onSurface
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .background(if (isCurrent) colors.secondaryContainer else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PrayerGlyph(time.prayer, content, Modifier.size(22.dp))
        Spacer(Modifier.size(16.dp))
        Text(
            stringResource(time.prayer.label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isNext || isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = content,
            modifier = Modifier.weight(1f),
        )
        if (isCurrent) {
            Text(
                stringResource(R.string.now_badge),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSecondaryContainer,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        Text(
            time.time.clock(),
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = if (isNext || isCurrent) FontWeight.Bold else FontWeight.Medium,
            color = content,
        )
    }
}

/** One line for the picker: the selected province and its next prayer, loaded live. */
@Composable
fun SelectedSummary(province: Province, repository: PrayerRepository) {
    val state by produceState<Load<PrayerSchedule>>(Load.Loading, province) {
        value = load { repository.schedule(province) }
    }
    ReportLoad(state)
    val now = rememberNow()
    val text = when (val s = state) {
        is Load.Ready -> s.value.next(now)?.let {
            stringResource(R.string.selected_next, province.name, stringResource(it.next.prayer.label), it.next.time.clock())
        }
        else -> null
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PinIcon(MaterialTheme.colorScheme.onSecondaryContainer, Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        Text(
            text ?: province.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

