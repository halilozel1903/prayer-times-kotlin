package io.github.halilozel1903.prayertimes

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClientTest {
    private val istanbulZone = ZoneId.of("Europe/Istanbul")

    @Test
    fun `timingsByCity parses Aladhan's response`() = runTest {
        val transport = RecordingTransport.fixture("timingsByCity-istanbul-2026-10-13.json")
        val day = clientWith(transport).timingsByCity("Istanbul", "Turkey", LocalDate.of(2026, 10, 13))

        assertEquals(
            "https://api.aladhan.com/v1/timingsByCity/13-10-2026?city=Istanbul&country=Turkey&method=13",
            transport.urls.single(),
        )
        assertEquals(LocalDate.of(2026, 10, 13), day.date)
        assertEquals(istanbulZone, day.zone)
        assertEquals(LocalDateTime.of(2026, 10, 13, 5, 42), day.fajr.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 10, 13, 7, 6), day.sunrise.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 10, 13, 12, 55), day.dhuhr.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 10, 13, 16, 2), day.asr.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 10, 13, 18, 34), day.maghrib.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 10, 13, 19, 53), day.isha.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 10, 13, 5, 32), day.imsak.toLocalDateTime())
        assertEquals("+03:00", day.fajr.offset.id)
        // Night times after midnight belong to the next date.
        assertEquals(LocalDateTime.of(2026, 10, 14, 0, 50), day.midnight?.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 10, 13, 22, 42), day.firstThird?.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 10, 14, 2, 58), day.lastThird?.toLocalDateTime())

        assertEquals(Prayer.entries, day.times.map { it.prayer })
        assertEquals(5, day.prayers.size)
        assertEquals(day.maghrib, day[Prayer.Maghrib])
        assertTrue(day.times.zipWithNext().all { (a, b) -> a.time.isBefore(b.time) })
    }

    @Test
    fun `hijri date and meta`() = runTest {
        val day = clientWith(RecordingTransport.fixture("timingsByCity-istanbul-2026-10-13.json"))
            .timingsByCity("Istanbul", date = LocalDate.of(2026, 10, 13))

        assertEquals(2, day.hijri.day)
        assertEquals(HijriMonth.JumadaAlUla, day.hijri.month)
        assertEquals(1448, day.hijri.year)
        assertEquals("Jumādá al-ūlá", day.hijri.monthNameTransliterated)
        assertEquals("2 Cemaziyelevvel 1448", day.hijri.format("tr"))
        assertEquals("2 Jumada al-Ula 1448", day.hijri.format("en-GB"))

        assertEquals(13, day.meta.methodId)
        assertEquals("Diyanet İşleri Başkanlığı, Turkey (experimental)", day.meta.methodName)
        assertEquals("STANDARD", day.meta.school)
        assertEquals(-7, day.meta.offsets["Sunrise"])
        assertEquals(7, day.meta.offsets["Maghrib"])
    }

    @Test
    fun `calendar parses a month with timezone suffixes`() = runTest {
        val transport = RecordingTransport.fixture("calendar-ankara-2026-10.json")
        val ankara = TurkeyProvinces.byPlate(6)
        val month = clientWith(transport).calendar(ankara, YearMonth.of(2026, 10))

        assertEquals(
            "https://api.aladhan.com/v1/calendar/2026/10?latitude=39.9334&longitude=32.8597" +
                "&timezonestring=Europe%2FIstanbul&method=13",
            transport.urls.single(),
        )
        assertEquals(31, month.size)
        assertEquals((1..31).toList(), month.map { it.date.dayOfMonth })
        assertTrue(month.all { it.date.monthValue == 10 && it.zone == istanbulZone })

        val thirteenth = month[12]
        assertEquals(LocalDateTime.of(2026, 10, 13, 5, 27), thirteenth.fajr.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 10, 13, 15, 48), thirteenth.asr.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 10, 13, 19, 37), thirteenth.isha.toLocalDateTime())

        // The Hijri month changes on 12 October.
        assertEquals(HijriMonth.RabiAlThani, month[10].hijri.month)
        assertEquals(30, month[10].hijri.day)
        assertEquals(HijriMonth.JumadaAlUla, month[11].hijri.month)
        assertEquals(1, month[11].hijri.day)
        assertEquals(1, month[2].hijri.holidays.size)

        // Days get shorter through October.
        assertTrue(month.first().maghrib.toLocalTime().isAfter(month.last().maghrib.toLocalTime()))
    }

    @Test
    fun `method, school and tune are sent`() = runTest {
        val transport = RecordingTransport.fixture("timingsByCity-istanbul-2026-10-13.json")
        val client = clientWith(transport) {
            method = CalculationMethod.MuslimWorldLeague
            school = School.Hanafi
            tune = Tune(fajr = 2, isha = -1)
        }
        client.timingsByCity("Kahramanmaraş", "Turkey", LocalDate.of(2026, 10, 13))

        assertEquals(
            "https://api.aladhan.com/v1/timingsByCity/13-10-2026?city=Kahramanmara%C5%9F&country=Turkey" +
                "&method=3&school=1&tune=0,2,0,0,0,0,0,-1,0",
            transport.urls.single(),
        )
    }

    @Test
    fun `timings by coordinates`() = runTest {
        val transport = RecordingTransport.fixture("timingsByCity-istanbul-2026-10-13.json")
        clientWith(transport).timings(41.0082, 28.9784, LocalDate.of(2026, 10, 13))
        assertEquals(
            "https://api.aladhan.com/v1/timings/13-10-2026?latitude=41.0082&longitude=28.9784&method=13",
            transport.urls.single(),
        )
        assertFailsWith<IllegalArgumentException> { clientWith(transport).timings(91.0, 0.0) }
    }

    @Test
    fun `responses are cached by url`() = runTest {
        val transport = RecordingTransport.fixture("timingsByCity-istanbul-2026-10-13.json")
        val client = clientWith(transport)
        val date = LocalDate.of(2026, 10, 13)
        client.timingsByCity("Istanbul", date = date)
        client.timingsByCity("Istanbul", date = date)
        assertEquals(1, transport.urls.size)
    }

    @Test
    fun `schedule fetches today and tomorrow`() = runTest {
        val transport = RecordingTransport.fixture("timingsByCity-istanbul-2026-10-13.json")
        val schedule = clientWith(transport).schedule(TurkeyProvinces.byPlate(34), LocalDate.of(2026, 10, 13))
        assertEquals(2, transport.urls.size)
        assertTrue(transport.urls.any { "/timings/13-10-2026?" in it })
        assertTrue(transport.urls.any { "/timings/14-10-2026?" in it })
        assertEquals(2, schedule.days.size)
    }

    @Test
    fun `qibla endpoint`() = runTest {
        val transport = RecordingTransport.fixture("qibla-istanbul.json")
        val direction = clientWith(transport).qibla(41.0082, 28.9784)
        assertEquals("https://api.aladhan.com/v1/qibla/41.0082/28.9784", transport.urls.single())
        assertEquals(151.6206517614259, direction, 1e-9)
    }

    @Test
    fun `an error in the envelope becomes an Api exception`() = runTest {
        val transport = RecordingTransport.fixture("error-bad-request.json")
        val client = clientWith(transport)
        val error = assertFailsWith<PrayerTimesException.Api> { client.timingsByCity("Nowhere", "Narnia") }
        assertEquals(400, error.code)
        assertEquals("Please specify a city and country.", error.reason)
        // Errors are not cached.
        assertFailsWith<PrayerTimesException.Api> { client.timingsByCity("Nowhere", "Narnia") }
        assertEquals(2, transport.urls.size)
    }

    @Test
    fun `invalid JSON becomes a Decoding exception`() = runTest {
        val client = clientWith(RecordingTransport { "<html>Bad gateway</html>" })
        assertFailsWith<PrayerTimesException.Decoding> { client.timingsByCity("Istanbul") }
    }

    @Test
    fun `transport failures become Network exceptions`() = runTest {
        val client = clientWith(RecordingTransport { throw java.io.IOException("offline") })
        assertFailsWith<PrayerTimesException.Network> { client.timingsByCity("Istanbul") }
    }

    @Test
    fun `the Ktor transport reads the JSON body of a 400`() = runTest {
        val engine = MockEngine { request ->
            if ("qibla" in request.url.encodedPath) {
                respond("Service unavailable", HttpStatusCode.ServiceUnavailable)
            } else {
                respond(
                    Fixtures.read("error-bad-request.json"),
                    HttpStatusCode.BadRequest,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val client = PrayerTimesClient {
            this.engine = engine
            retries = 0
        }
        assertFailsWith<PrayerTimesException.Api> { client.timingsByCity("Nowhere") }
        val http = assertFailsWith<PrayerTimesException.Http> { client.qibla(41.0, 29.0) }
        assertEquals(503, http.status)
        client.close()
    }

    @Test
    fun `the Ktor transport parses a successful response`() = runTest {
        val engine = MockEngine {
            respond(
                Fixtures.read("timingsByCity-istanbul-2026-10-13.json"),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = PrayerTimesClient { this.engine = engine }
        val day = client.timingsByCity("Istanbul", date = LocalDate.of(2026, 10, 13))
        assertEquals(18, day.maghrib.hour)
        val request = engine.requestHistory.single()
        assertTrue(request.headers[HttpHeaders.UserAgent]!!.startsWith("prayer-times-kotlin/"))
        client.close()
    }
}
