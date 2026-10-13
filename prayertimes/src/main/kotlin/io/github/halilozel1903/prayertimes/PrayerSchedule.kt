package io.github.halilozel1903.prayertimes

import java.time.Duration
import java.time.Instant

/**
 * The next time and how long until it, at one moment.
 *
 * @property next the next time, for example Maghrib (Akşam).
 * @property current the time that started most recently, for example Asr (İkindi). Before the day's Fajr this is
 * the previous evening's Isha; `null` when the schedule does not reach back that far.
 * @property remaining from now until [next].
 * @property progress how much of the span from [current] to [next] has passed, `0.0` to `1.0`, or `null` without
 * a [current].
 */
public data class NextPrayer(
    val next: PrayerTime,
    val current: PrayerTime?,
    val remaining: Duration,
    val progress: Float?,
) {
    /** [remaining] as `HH:MM:SS`, for example `02:13:05`. Never negative. */
    public val countdown: String get() = formatCountdown(remaining)
}

/**
 * Consecutive days of [PrayerTimes] that answer "which time is next, and how long until then" at any moment.
 * Give it at least today and tomorrow (see [PrayerTimesClient.schedule]) so that after Isha the answer is
 * tomorrow's Fajr (İmsak); add yesterday to know the current time before Fajr.
 *
 * ```kotlin
 * val next = schedule.next(Instant.now())
 * "${next.next.prayer.turkishName} ${next.countdown}"   // "Akşam 02:13:05"
 * ```
 */
public class PrayerSchedule(days: List<PrayerTimes>, private val includeSunrise: Boolean = true) {

    /** The days, sorted by date. */
    public val days: List<PrayerTimes> = days.sortedBy { it.date }

    /** Every time of every day, in order. */
    public val times: List<PrayerTime> = this.days.flatMap { day ->
        day.times.filter { includeSunrise || it.prayer.isPrayer }
    }

    init {
        require(days.isNotEmpty()) { "A schedule needs at least one day" }
    }

    /** The day [instant] falls on in the schedule's time zone, or `null` when it is not in the schedule. */
    public fun dayOf(instant: Instant): PrayerTimes? {
        val date = instant.atZone(days.first().zone).toLocalDate()
        return days.firstOrNull { it.date == date }
    }

    /**
     * The next time after [now], or `null` when [now] is past the last time of the schedule (fetch more days).
     * A time exactly at [now] counts as started, not as next.
     */
    public fun next(now: Instant): NextPrayer? {
        val index = times.indexOfFirst { it.instant.isAfter(now) }
        if (index < 0) return null
        val next = times[index]
        val current = times.getOrNull(index - 1)
        val remaining = Duration.between(now, next.instant)
        val progress = current?.let {
            val span = Duration.between(it.instant, next.instant).toMillis().toFloat()
            if (span <= 0f) null else (1f - remaining.toMillis() / span).coerceIn(0f, 1f)
        }
        return NextPrayer(next, current, remaining, progress)
    }

    /** The time that started most recently at [now], or `null` before the first time of the schedule. */
    public fun current(now: Instant): PrayerTime? = times.lastOrNull { !it.instant.isAfter(now) }
}

internal fun formatCountdown(duration: Duration): String {
    val seconds = duration.seconds.coerceAtLeast(0)
    return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}
