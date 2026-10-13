package io.github.halilozel1903.prayertimes

import io.github.halilozel1903.prayertimes.transport.PrayerTimesTransport

/**
 * Serves the JSON files in `src/test/resources/fixtures`. They hold the values Aladhan returned for İstanbul
 * (`timingsByCity`, 13 Oct 2026), Ankara (`calendar`, October 2026) and İstanbul's Qibla, with Aladhan's field
 * names, nesting and time formats (`"05:42"` for timings, `"05:15 (+03)"` in calendars).
 */
internal object Fixtures {
    fun read(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/$name")) { "Missing fixture $name" }.readText()
}

/** A transport that answers every request with [body] and records the urls it was asked for. */
internal class RecordingTransport(private val body: (String) -> String) : PrayerTimesTransport {
    val urls = mutableListOf<String>()

    override suspend fun get(url: String): String {
        urls += url
        return body(url)
    }

    companion object {
        fun fixture(name: String) = RecordingTransport { Fixtures.read(name) }
    }
}

internal fun clientWith(transport: PrayerTimesTransport, configure: PrayerTimesConfig.() -> Unit = {}) =
    PrayerTimesClient {
        this.transport = transport
        configure()
    }
