package io.github.halilozel1903.prayertimes.sample.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.halilozel1903.prayertimes.PrayerSchedule
import io.github.halilozel1903.prayertimes.Province
import io.github.halilozel1903.prayertimes.sample.MainActivity
import io.github.halilozel1903.prayertimes.sample.PrayerRepository
import io.github.halilozel1903.prayertimes.sample.R
import io.github.halilozel1903.prayertimes.sample.clock
import io.github.halilozel1903.prayertimes.sample.graph
import io.github.halilozel1903.prayertimes.sample.label
import io.github.halilozel1903.prayertimes.sample.reminders.inAppLanguage
import java.time.Instant

/**
 * A home screen widget: the saved province, its next prayer and today's six times, loaded live from Aladhan when
 * the widget updates (every 30 minutes, when the city changes and at each reminder). Tapping it opens the app,
 * which has the live countdown.
 */
class PrayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val province = context.graph.settings.province
        val schedule = runCatching { context.graph.repository.schedule(province, PrayerRepository.today()) }.getOrNull()
        val strings = context.inAppLanguage()
        provideContent {
            GlanceTheme {
                WidgetContent(strings, province, schedule, Instant.now())
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) = PrayerWidget().updateAll(context)
    }
}

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()
}

@Composable
private fun WidgetContent(strings: Context, province: Province, schedule: PrayerSchedule?, now: Instant) {
    Column(
        GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            province.name,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold),
        )
        val next = schedule?.next(now)
        if (schedule == null || next == null) {
            Text(strings.getString(R.string.widget_error), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
        } else {
            Text(
                "${strings.getString(R.string.next_prayer)}: ${strings.getString(next.next.prayer.label)} ${next.next.time.clock()}",
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            )
            Spacer(GlanceModifier.height(10.dp))
            val today = schedule.dayOf(now) ?: schedule.days.first()
            Row(GlanceModifier.fillMaxWidth()) {
                today.times.forEach { time ->
                    val isNext = time == next.next
                    Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            strings.getString(time.prayer.label),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                            maxLines = 1,
                        )
                        Text(
                            time.time.clock(),
                            style = TextStyle(
                                color = if (isNext) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                            ),
                        )
                    }
                }
            }
        }
    }
}
