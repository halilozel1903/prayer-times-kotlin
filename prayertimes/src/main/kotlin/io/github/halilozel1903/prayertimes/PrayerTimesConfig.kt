package io.github.halilozel1903.prayertimes

import io.github.halilozel1903.prayertimes.transport.PrayerTimesTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

/** Settings for [PrayerTimesClient], set in its builder block. */
public class PrayerTimesConfig internal constructor() {
    /** The API root. */
    public var baseUrl: String = PrayerTimesClient.DEFAULT_BASE_URL

    /** The calculation method of every request. Diyanet İşleri Başkanlığı (13) by default. */
    public var method: CalculationMethod = CalculationMethod.Diyanet

    /** The juristic school for Asr. */
    public var school: School = School.Standard

    /** Minute offsets added to Aladhan's times. */
    public var tune: Tune = Tune.None

    /**
     * The Ktor engine. `null` uses CIO. Pass `OkHttp.create()` (add `io.ktor:ktor-client-okhttp`) on Android, or a
     * `MockEngine` in tests. The client does not close an engine you pass.
     */
    public var engine: HttpClientEngine? = null

    /** A ready Ktor client to use as is, instead of [engine]. [requestTimeoutMillis] and [retries] then do not apply. */
    public var httpClient: HttpClient? = null

    /** Replaces HTTP entirely, for example with responses from files in tests. */
    public var transport: PrayerTimesTransport? = null

    /** Responses kept in memory, by url. Times of a past or future day do not change, so repeated calls are free. 0 turns it off. */
    public var cacheSize: Int = 64

    /** Sent with every request. */
    public var userAgent: String = "prayer-times-kotlin/1.0.0 (+https://github.com/halilozel1903/prayer-times-kotlin)"

    /** Timeout of one request, in milliseconds. */
    public var requestTimeoutMillis: Long = 20_000

    /** Retries on network errors and 5xx responses, with exponential backoff. */
    public var retries: Int = 2
}
