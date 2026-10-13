package io.github.halilozel1903.prayertimes.internal

import io.github.halilozel1903.prayertimes.CalculationMeta
import io.github.halilozel1903.prayertimes.HijriDate
import io.github.halilozel1903.prayertimes.HijriMonth
import io.github.halilozel1903.prayertimes.PrayerTimes
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal val ApiDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

/** Maps one day of Aladhan's response to [PrayerTimes]. */
internal fun DayDto.toPrayerTimes(): PrayerTimes {
    val zone = ZoneId.of(meta.timezone)
    val day = LocalDate.parse(date.gregorian.date, ApiDate)

    fun time(key: String): ZonedDateTime? = timings[key]?.let { parseTime(it, day, zone) }
    fun required(key: String): ZonedDateTime = time(key) ?: throw IllegalStateException("No $key in timings")

    val sunset = required("Sunset")
    // The night times belong to the night after this day's sunset; those after midnight move to the next date.
    fun night(key: String): ZonedDateTime? = time(key)?.let { if (it.isBefore(sunset)) it.plusDays(1) else it }

    return PrayerTimes(
        date = day,
        zone = zone,
        fajr = required("Fajr"),
        sunrise = required("Sunrise"),
        dhuhr = required("Dhuhr"),
        asr = required("Asr"),
        maghrib = required("Maghrib"),
        isha = required("Isha"),
        imsak = time("Imsak") ?: required("Fajr").minusMinutes(10),
        sunset = sunset,
        midnight = night("Midnight"),
        firstThird = night("Firstthird"),
        lastThird = night("Lastthird"),
        hijri = HijriDate(
            day = date.hijri.day.trim().toInt(),
            month = HijriMonth.of(date.hijri.month.number),
            year = date.hijri.year.trim().toInt(),
            monthNameTransliterated = date.hijri.month.en,
            monthNameArabic = date.hijri.month.ar,
            weekdayArabic = date.hijri.weekday?.ar.orEmpty(),
            holidays = date.hijri.holidays,
        ),
        meta = CalculationMeta(
            latitude = meta.latitude,
            longitude = meta.longitude,
            timezone = meta.timezone,
            methodId = meta.method?.id ?: -1,
            methodName = meta.method?.name.orEmpty(),
            school = meta.school,
            offsets = meta.offset,
        ),
    )
}

/**
 * Reads one of Aladhan's time strings on [day] in [zone]. Accepts the default `"05:42"`, the calendar's
 * `"05:42 (+03)"` and the ISO 8601 form `"2026-10-13T05:42:00+03:00"` (`iso8601=true`).
 */
internal fun parseTime(value: String, day: LocalDate, zone: ZoneId): ZonedDateTime {
    val text = value.trim()
    if (text.length > 10 && text[4] == '-' && 'T' in text) {
        return OffsetDateTime.parse(text).atZoneSameInstant(zone)
    }
    val clock = text.substringBefore(' ')
    val (hour, minute) = clock.split(':').let { it[0].toInt() to it[1].toInt() }
    return ZonedDateTime.of(day, LocalTime.of(hour, minute), zone)
}
