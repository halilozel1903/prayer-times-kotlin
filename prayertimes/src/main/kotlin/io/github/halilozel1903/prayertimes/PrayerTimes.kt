package io.github.halilozel1903.prayertimes

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** One time of the day. */
public data class PrayerTime(val prayer: Prayer, val time: ZonedDateTime) {
    public val instant: Instant get() = time.toInstant()
}

/**
 * The times of one day at one place, in the place's own time zone ([zone], from Aladhan's `meta.timezone`).
 *
 * [midnight], [firstThird] and [lastThird] are the night after this day's Maghrib, so [midnight] and [lastThird]
 * usually fall on the next calendar date.
 */
public data class PrayerTimes(
    val date: LocalDate,
    val zone: ZoneId,
    val fajr: ZonedDateTime,
    val sunrise: ZonedDateTime,
    val dhuhr: ZonedDateTime,
    val asr: ZonedDateTime,
    val maghrib: ZonedDateTime,
    val isha: ZonedDateTime,
    /** Aladhan's `Imsak`: a fixed 10 minutes before Fajr. Diyanet's İmsak is [fajr]. */
    val imsak: ZonedDateTime,
    val sunset: ZonedDateTime,
    val midnight: ZonedDateTime?,
    val firstThird: ZonedDateTime?,
    val lastThird: ZonedDateTime?,
    val hijri: HijriDate,
    val meta: CalculationMeta,
) {
    /** The time of [prayer]. */
    public operator fun get(prayer: Prayer): ZonedDateTime = when (prayer) {
        Prayer.Fajr -> fajr
        Prayer.Sunrise -> sunrise
        Prayer.Dhuhr -> dhuhr
        Prayer.Asr -> asr
        Prayer.Maghrib -> maghrib
        Prayer.Isha -> isha
    }

    /** The six times in order, from Fajr (İmsak) to Isha (Yatsı). */
    public val times: List<PrayerTime> get() = Prayer.entries.map { PrayerTime(it, get(it)) }

    /** The five prayers in order, without sunrise. */
    public val prayers: List<PrayerTime> get() = Prayer.prayers.map { PrayerTime(it, get(it)) }

    /** The next time after [now] on this day, or `null` after Isha. See [PrayerSchedule] for a countdown across days. */
    public fun nextAfter(now: Instant, includeSunrise: Boolean = true): PrayerTime? =
        times.firstOrNull { (includeSunrise || it.prayer.isPrayer) && it.instant.isAfter(now) }
}

/** How Aladhan computed the times: Aladhan's `meta` object. */
public data class CalculationMeta(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val methodId: Int,
    val methodName: String,
    /** `"STANDARD"` or `"HANAFI"`. */
    val school: String,
    /** Minute offsets Aladhan applied (the method's defaults plus `tune`), keyed by Aladhan's names. */
    val offsets: Map<String, Int>,
)
