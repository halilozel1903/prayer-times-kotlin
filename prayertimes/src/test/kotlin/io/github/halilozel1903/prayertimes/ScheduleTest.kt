package io.github.halilozel1903.prayertimes

import kotlinx.coroutines.test.runTest
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Next prayer and countdown, on Ankara's real times for 12 to 14 October 2026. */
class ScheduleTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private lateinit var october: List<PrayerTimes>
    private lateinit var schedule: PrayerSchedule

    @BeforeTest
    fun load() = runTest {
        october = clientWith(RecordingTransport.fixture("calendar-ankara-2026-10.json"))
            .calendar(TurkeyProvinces.byPlate(6), YearMonth.of(2026, 10))
        // 13 Oct: İmsak 05:27, Güneş 06:50, Öğle 12:40, İkindi 15:48, Akşam 18:19, Yatsı 19:37
        // 14 Oct: İmsak 05:28, Güneş 06:51, ...
        schedule = PrayerSchedule(listOf(october[13], october[11], october[12]))   // unsorted on purpose
    }

    private fun at(day: Int, hour: Int, minute: Int, second: Int = 0): Instant =
        LocalDateTime.of(2026, 10, day, hour, minute, second).atZone(zone).toInstant()

    @Test
    fun `days are sorted`() {
        assertEquals(listOf(12, 13, 14), schedule.days.map { it.date.dayOfMonth })
        assertEquals(18, schedule.times.size)
    }

    @Test
    fun `afternoon - next is Maghrib`() {
        val next = assertNotNull(schedule.next(at(13, 16, 0)))
        assertEquals(Prayer.Maghrib, next.next.prayer)
        assertEquals(LocalDateTime.of(2026, 10, 13, 18, 19), next.next.time.toLocalDateTime())
        assertEquals(Prayer.Asr, next.current?.prayer)
        assertEquals(Duration.ofHours(2).plusMinutes(19), next.remaining)
        assertEquals("02:19:00", next.countdown)
        // 12 of 151 minutes from Asr to Maghrib have passed.
        assertEquals(12f / 151f, next.progress!!, 0.001f)
    }

    @Test
    fun `seconds count down`() {
        val next = assertNotNull(schedule.next(at(13, 12, 39, 15)))
        assertEquals(Prayer.Dhuhr, next.next.prayer)
        assertEquals("00:00:45", next.countdown)
    }

    @Test
    fun `after Isha the next is tomorrow's Fajr`() {
        val next = assertNotNull(schedule.next(at(13, 21, 30)))
        assertEquals(Prayer.Fajr, next.next.prayer)
        assertEquals(LocalDate.of(2026, 10, 14), next.next.time.toLocalDate())
        assertEquals(LocalDateTime.of(2026, 10, 14, 5, 28), next.next.time.toLocalDateTime())
        assertEquals(Prayer.Isha, next.current?.prayer)
        assertEquals("07:58:00", next.countdown)
    }

    @Test
    fun `before Fajr the current time is last night's Isha`() {
        val next = assertNotNull(schedule.next(at(13, 3, 0)))
        assertEquals(Prayer.Fajr, next.next.prayer)
        assertEquals(LocalDateTime.of(2026, 10, 13, 5, 27), next.next.time.toLocalDateTime())
        assertEquals(Prayer.Isha, next.current?.prayer)
        assertEquals(LocalDate.of(2026, 10, 12), next.current?.time?.toLocalDate())
    }

    @Test
    fun `a time that starts now counts as current`() {
        val next = assertNotNull(schedule.next(at(13, 18, 19)))
        assertEquals(Prayer.Isha, next.next.prayer)
        assertEquals(Prayer.Maghrib, next.current?.prayer)
        assertEquals(Prayer.Maghrib, schedule.current(at(13, 18, 19))?.prayer)
    }

    @Test
    fun `sunrise can be left out`() {
        val prayersOnly = PrayerSchedule(listOf(october[12], october[13]), includeSunrise = false)
        val next = assertNotNull(prayersOnly.next(at(13, 6, 0)))
        assertEquals(Prayer.Dhuhr, next.next.prayer)
        assertEquals(Prayer.Fajr, next.current?.prayer)
        assertEquals(Prayer.Sunrise, schedule.next(at(13, 6, 0))?.next?.prayer)
    }

    @Test
    fun `past the end of the schedule there is no next`() {
        assertNull(schedule.next(at(14, 23, 0)))
        assertNull(schedule.current(at(12, 1, 0)))
        val first = assertNotNull(schedule.next(at(12, 1, 0)))
        assertNull(first.current)
        assertNull(first.progress)
    }

    @Test
    fun `day lookup and nextAfter on one day`() {
        assertEquals(LocalDate.of(2026, 10, 13), schedule.dayOf(at(13, 23, 59))?.date)
        assertNull(schedule.dayOf(at(20, 12, 0)))
        assertEquals(Prayer.Asr, october[12].nextAfter(at(13, 13, 0))?.prayer)
        assertNull(october[12].nextAfter(at(13, 20, 0)))
    }

    @Test
    fun `countdown never goes negative and pads hours`() {
        assertEquals("00:00:00", formatCountdown(Duration.ofSeconds(-5)))
        assertEquals("10:05:09", formatCountdown(Duration.ofSeconds(10 * 3600 + 5 * 60 + 9)))
    }

    @Test
    fun `prayer names in both languages`() {
        assertEquals(listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı"), Prayer.entries.map { it.turkishName })
        assertEquals(listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"), Prayer.entries.map { it.englishName })
        assertEquals("Akşam", Prayer.Maghrib.displayName("tr-TR"))
        assertEquals("Maghrib", Prayer.Maghrib.displayName("en"))
        assertTrue(Prayer.prayers.none { it == Prayer.Sunrise })
    }
}
