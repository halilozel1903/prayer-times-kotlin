package io.github.halilozel1903.prayertimes.transport

/**
 * Fetches the body of an Aladhan url. The client uses a Ktor based transport by default; supply your own through
 * `PrayerTimesConfig.transport` to serve responses from files in tests, from a database or from another HTTP stack.
 *
 * Return the body for any HTTP status: Aladhan reports errors such as an unknown city in the JSON body
 * (`{"code":400,"status":"BAD_REQUEST","data":"..."}`), and the client turns those into
 * [io.github.halilozel1903.prayertimes.PrayerTimesException.Api]. Throw for network failures; they are reported as
 * [io.github.halilozel1903.prayertimes.PrayerTimesException.Network].
 */
public fun interface PrayerTimesTransport {
    public suspend fun get(url: String): String
}
