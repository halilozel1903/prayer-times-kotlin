package io.github.halilozel1903.prayertimes.sample

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import io.github.halilozel1903.prayertimes.Prayer
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

/** The localized name of a prayer: İmsak, Güneş, Öğle, İkindi, Akşam, Yatsı in Turkish. */
@get:StringRes
val Prayer.label: Int
    get() = when (this) {
        Prayer.Fajr -> R.string.prayer_fajr
        Prayer.Sunrise -> R.string.prayer_sunrise
        Prayer.Dhuhr -> R.string.prayer_dhuhr
        Prayer.Asr -> R.string.prayer_asr
        Prayer.Maghrib -> R.string.prayer_maghrib
        Prayer.Isha -> R.string.prayer_isha
    }

/** 24-hour clock, as on every Turkish prayer calendar. */
val ClockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

fun TemporalAccessor.clock(): String = ClockFormat.format(this)

/** The locale of the app's current language (the per-app language when one is set). */
@Composable
@ReadOnlyComposable
fun appLocale(): Locale = LocalConfiguration.current.locales.get(0) ?: Locale.getDefault()

fun Double.formatDegrees(locale: Locale): String = String.format(locale, "%.1f", this)
