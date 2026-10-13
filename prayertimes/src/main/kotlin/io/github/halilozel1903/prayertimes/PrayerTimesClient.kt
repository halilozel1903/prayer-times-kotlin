package io.github.halilozel1903.prayertimes

import io.github.halilozel1903.prayertimes.internal.ApiDate
import io.github.halilozel1903.prayertimes.internal.DayDto
import io.github.halilozel1903.prayertimes.internal.EnvelopeDto
import io.github.halilozel1903.prayertimes.internal.QiblaDto
import io.github.halilozel1903.prayertimes.internal.toPrayerTimes
import io.github.halilozel1903.prayertimes.transport.KtorTransport
import io.github.halilozel1903.prayertimes.transport.PrayerTimesTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.URLEncoder
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

/**
 * A coroutine client for the free [Aladhan prayer times API](https://aladhan.com/prayer-times-api).
 *
 * ```kotlin
 * val client = PrayerTimesClient()                        // Diyanet method (13) by default
 * val istanbul = TurkeyProvinces.byPlate(34)
 * val today = client.timings(istanbul)                    // PrayerTimes with java.time values
 * val schedule = client.schedule(istanbul)                // today and tomorrow, for a countdown
 * schedule.next(Instant.now())                            // NextPrayer(Maghrib at 18:34, remaining 2h 13m)
 * ```
 *
 * Every function is main-safe and throws a [PrayerTimesException] on failure. Create one client, share it, and
 * close it when you no longer need it.
 */
public class PrayerTimesClient(configure: PrayerTimesConfig.() -> Unit = {}) : AutoCloseable {

    private val config = PrayerTimesConfig().apply(configure)

    /** The API root, without a trailing slash. */
    public val baseUrl: String = config.baseUrl.trimEnd('/')

    /** The calculation method sent with every request. */
    public val method: CalculationMethod get() = config.method

    private val transport: PrayerTimesTransport = config.transport ?: KtorTransport(
        httpClient = config.httpClient,
        engine = config.engine,
        userAgent = config.userAgent,
        requestTimeoutMillis = config.requestTimeoutMillis,
        retries = config.retries,
    )
    private val cacheLock = Mutex()
    private val cache = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > config.cacheSize
    }

    // region Timings

    /** `GET /timings/{date}?latitude=&longitude=`: the times of [date] at a point. */
    public suspend fun timings(
        latitude: Double,
        longitude: Double,
        date: LocalDate = LocalDate.now(),
        timezone: ZoneId? = null,
    ): PrayerTimes {
        val url = url("timings/${date.format(ApiDate)}", coordinates(latitude, longitude, timezone))
        return day(url)
    }

    /** The times of [date] at the center of [province], in Turkey's time zone. */
    public suspend fun timings(province: Province, date: LocalDate = LocalDate.now(TurkeyProvinces.zone)): PrayerTimes =
        timings(province.latitude, province.longitude, date, TurkeyProvinces.zone)

    /**
     * `GET /timingsByCity/{date}?city=&country=`: the times of [date] in a city that Aladhan geocodes by name.
     * Prefer [timings] with coordinates when you have them (for Turkey, a [Province]); names are ambiguous.
     */
    public suspend fun timingsByCity(
        city: String,
        country: String = "Turkey",
        date: LocalDate = LocalDate.now(),
        state: String? = null,
    ): PrayerTimes {
        val url = url(
            "timingsByCity/${date.format(ApiDate)}",
            buildList<Pair<String, String>> {
                add("city" to city)
                add("country" to country)
                if (state != null) add("state" to state)
            },
        )
        return day(url)
    }

    /**
     * Today and tomorrow at a point, fetched at the same time, as a [PrayerSchedule] that answers "what is next and
     * how long until then" at any moment, including after Isha.
     */
    public suspend fun schedule(
        latitude: Double,
        longitude: Double,
        date: LocalDate = LocalDate.now(),
        timezone: ZoneId? = null,
    ): PrayerSchedule = coroutineScope {
        val today = async { timings(latitude, longitude, date, timezone) }
        val tomorrow = async { timings(latitude, longitude, date.plusDays(1), timezone) }
        PrayerSchedule(listOf(today.await(), tomorrow.await()))
    }

    /** Today and tomorrow at the center of [province]. */
    public suspend fun schedule(province: Province, date: LocalDate = LocalDate.now(TurkeyProvinces.zone)): PrayerSchedule =
        schedule(province.latitude, province.longitude, date, TurkeyProvinces.zone)

    // endregion

    // region Calendar

    /** `GET /calendar/{year}/{month}?latitude=&longitude=`: every day of [month] at a point. */
    public suspend fun calendar(
        latitude: Double,
        longitude: Double,
        month: YearMonth = YearMonth.now(),
        timezone: ZoneId? = null,
    ): List<PrayerTimes> {
        val url = url("calendar/${month.year}/${month.monthValue}", coordinates(latitude, longitude, timezone))
        return days(url)
    }

    /** Every day of [month] at the center of [province]. */
    public suspend fun calendar(province: Province, month: YearMonth = YearMonth.now(TurkeyProvinces.zone)): List<PrayerTimes> =
        calendar(province.latitude, province.longitude, month, TurkeyProvinces.zone)

    /** `GET /calendarByCity/{year}/{month}?city=&country=`: every day of [month] in a city geocoded by name. */
    public suspend fun calendarByCity(
        city: String,
        country: String = "Turkey",
        month: YearMonth = YearMonth.now(),
    ): List<PrayerTimes> {
        val url = url(
            "calendarByCity/${month.year}/${month.monthValue}",
            listOf<Pair<String, String>>("city" to city, "country" to country),
        )
        return days(url)
    }

    // endregion

    /**
     * `GET /qibla/{latitude}/{longitude}`: Aladhan's Qibla direction in degrees from true north. [Qibla.bearing]
     * computes the same value offline.
     */
    public suspend fun qibla(latitude: Double, longitude: Double): Double {
        val url = "$baseUrl/qibla/$latitude/$longitude"
        return decode(url, QiblaDto.serializer()).direction
    }

    /** Builds the url of a request, with the method, school and tune of this client. Useful for logging. */
    public fun url(path: String, params: List<Pair<String, String>> = emptyList()): String {
        val all = buildList<Pair<String, String>> {
            addAll(params)
            add("method" to config.method.id.toString())
            if (config.school != School.Standard) add("school" to config.school.id.toString())
            if (!config.tune.isZero) add("tune" to config.tune.toQueryValue())
        }
        return "$baseUrl/$path?" + all.joinToString("&") { (k, v) -> "$k=${encode(v)}" }
    }

    override fun close() {
        (transport as? AutoCloseable)?.close()
    }

    // region Internals

    private fun coordinates(latitude: Double, longitude: Double, timezone: ZoneId?): List<Pair<String, String>> {
        require(latitude in -90.0..90.0) { "latitude $latitude is outside -90..90" }
        require(longitude in -180.0..180.0) { "longitude $longitude is outside -180..180" }
        return buildList<Pair<String, String>> {
            add("latitude" to latitude.toString())
            add("longitude" to longitude.toString())
            if (timezone != null) add("timezonestring" to timezone.id)
        }
    }

    private suspend fun day(url: String): PrayerTimes = decode(url, DayDto.serializer()).let { dto ->
        mapOrThrow(url) { dto.toPrayerTimes() }
    }

    private suspend fun days(url: String): List<PrayerTimes> = decode(url, ListSerializer(DayDto.serializer())).let { list ->
        mapOrThrow(url) { list.map { it.toPrayerTimes() } }
    }

    private inline fun <T> mapOrThrow(url: String, block: () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw PrayerTimesException.Decoding(url, e)
    }

    private suspend fun <T> decode(url: String, serializer: DeserializationStrategy<T>): T {
        val body = fetch(url)
        val envelope = try {
            json.decodeFromString(EnvelopeDto.serializer(), body)
        } catch (e: Exception) {
            throw PrayerTimesException.Decoding(url, e)
        }
        val data = envelope.data
        if (envelope.code != 200 || data == null || data is JsonPrimitive) {
            val reason = (data as? JsonPrimitive)?.contentOrNull ?: envelope.status.ifEmpty { "No data" }
            throw PrayerTimesException.Api(url, envelope.code, reason)
        }
        val value = try {
            json.decodeFromJsonElement(serializer, data)
        } catch (e: Exception) {
            throw PrayerTimesException.Decoding(url, e)
        }
        // Only answers that could be read are cached, so a failure is retried on the next call.
        if (config.cacheSize > 0) cacheLock.withLock { cache[url] = body }
        return value
    }

    private suspend fun fetch(url: String): String {
        if (config.cacheSize > 0) cacheLock.withLock { cache[url] }?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                transport.get(url)
            } catch (e: PrayerTimesException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw PrayerTimesException.Network(url, e)
            }
        }
    }

    // Commas stay readable in `tune=0,2,0,...`; they need no escaping in a query.
    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20").replace("%2C", ",")

    // endregion

    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://api.aladhan.com/v1"

        internal val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}
