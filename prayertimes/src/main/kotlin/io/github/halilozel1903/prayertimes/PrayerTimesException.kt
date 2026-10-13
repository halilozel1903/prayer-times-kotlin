package io.github.halilozel1903.prayertimes

/** Thrown by [PrayerTimesClient] when a request cannot be answered. */
public sealed class PrayerTimesException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The request did not reach Aladhan or timed out. */
    public class Network(public val url: String, cause: Throwable) :
        PrayerTimesException("Could not reach $url: ${cause.message ?: cause::class.simpleName}", cause)

    /** Aladhan answered with an HTTP error and no readable body. */
    public class Http(public val url: String, public val status: Int) :
        PrayerTimesException("HTTP $status for $url")

    /**
     * Aladhan answered with an error in its JSON envelope, for example `400` for an unknown city or invalid
     * coordinates. [reason] is Aladhan's own message.
     */
    public class Api(public val url: String, public val code: Int, public val reason: String) :
        PrayerTimesException("Aladhan error $code for $url: $reason")

    /** The response could not be read. */
    public class Decoding(public val url: String, cause: Throwable) :
        PrayerTimesException("Could not read the response of $url: ${cause.message}", cause)
}
