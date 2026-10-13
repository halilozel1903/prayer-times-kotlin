package io.github.halilozel1903.prayertimes.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.prayertimes.Prayer
import io.github.halilozel1903.prayertimes.PrayerTimes
import io.github.halilozel1903.prayertimes.Province
import io.github.halilozel1903.prayertimes.TurkeyProvinces
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Saves the shown month across rotation and process death as `[year, month]`. */
private val YearMonthSaver = listSaver<MutableState<YearMonth>, Int>(
    save = { listOf(it.value.year, it.value.monthValue) },
    restore = { mutableStateOf(YearMonth.of(it[0], it[1])) },
)

/** A month of times for [province], one row per day, today highlighted. */
@Composable
fun MonthScreen(province: Province, repository: PrayerRepository) {
    var month by rememberSaveable(saver = YearMonthSaver) { mutableStateOf(YearMonth.now(TurkeyProvinces.zone)) }
    var reload by remember { mutableIntStateOf(0) }
    val state by produceState<Load<List<PrayerTimes>>>(Load.Loading, province, month, reload) {
        value = Load.Loading
        value = load { repository.month(province, month) }
    }
    ReportLoad(state)
    val locale = appLocale()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    ChevronIcon(MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month).replaceFirstChar { it.titlecase(locale) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.month_caption, province.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    (state as? Load.Ready)?.value?.let { days ->
                        Text(
                            hijriSpan(days, locale.language),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    ChevronIcon(MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp), flip = true)
                }
            }

            when (val current = state) {
                Load.Loading -> LoadingState()
                is Load.Failed -> ErrorState(current, onRetry = { reload++ })
                is Load.Ready -> key(month) { MonthTable(current.value) }
            }
        }
    }
}

@Composable
private fun MonthTable(days: List<PrayerTimes>) {
    val locale = appLocale()
    val today = PrayerRepository.today()
    val todayIndex = days.indexOfFirst { it.date == today }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (todayIndex - 2).coerceAtLeast(0))
    val weekday = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            HeaderCell(stringResource(R.string.month_column_day), Modifier.width(DayColumn))
            Prayer.entries.forEach { prayer ->
                HeaderCell(stringResource(prayer.label), Modifier.weight(1f))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            items(days, key = { it.date.toEpochDay() }) { day ->
                val isToday = day.date == today
                val colors = MaterialTheme.colorScheme
                val content = if (isToday) colors.onPrimaryContainer else colors.onSurface
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                        .background(if (isToday) colors.primaryContainer else Color.Transparent, RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.width(DayColumn)) {
                        Text(
                            day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = content,
                        )
                        Text(
                            weekday.format(day.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) content else colors.onSurfaceVariant,
                        )
                    }
                    Prayer.entries.forEach { prayer ->
                        Text(
                            day[prayer].clock(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = content,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier,
    )
}

private val DayColumn = 44.dp

/** For example `Rebiülahir – Cemaziyelevvel 1448`. */
private fun hijriSpan(days: List<PrayerTimes>, language: String): String {
    val first = days.first().hijri
    val last = days.last().hijri
    return when {
        first.month == last.month -> "${first.month.displayName(language)} ${first.year}"
        first.year == last.year -> "${first.month.displayName(language)} – ${last.month.displayName(language)} ${last.year}"
        else -> "${first.month.displayName(language)} ${first.year} – ${last.month.displayName(language)} ${last.year}"
    }
}
