package io.github.halilozel1903.prayertimes

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The direction of the Kaaba, computed offline as the initial bearing of the great circle (the shortest path on the
 * globe). It agrees with Aladhan's `/qibla` endpoint: about 151.6° from true north in İstanbul, 160.2° in Ankara.
 *
 * The bearing is from **true** north. A phone compass measures magnetic north; add the local magnetic declination
 * (Android: `GeomagneticField(lat, lng, alt, time).declination`) to a compass heading before comparing.
 */
public object Qibla {
    /** The Kaaba in Mecca. */
    public const val KAABA_LATITUDE: Double = 21.422487
    public const val KAABA_LONGITUDE: Double = 39.826206

    private const val EARTH_RADIUS_KM = 6371.0088

    /** The Qibla bearing from a point, in degrees clockwise from true north, `0.0 <= result < 360.0`. */
    public fun bearing(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - longitude)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** The Qibla bearing from the center of [province]. */
    public fun bearing(province: Province): Double = bearing(province.latitude, province.longitude)

    /** The great circle distance to the Kaaba, in kilometers. */
    public fun distanceKm(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val dPhi = phi2 - phi1
        val dLambda = Math.toRadians(KAABA_LONGITUDE - longitude)
        val a = sin(dPhi / 2) * sin(dPhi / 2) + cos(phi1) * cos(phi2) * sin(dLambda / 2) * sin(dLambda / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    /**
     * Where to point relative to the phone's heading: rotate an arrow by this many degrees (clockwise) when the top
     * of the phone faces [headingDegrees] from true north.
     */
    public fun relativeBearing(qiblaBearing: Double, headingDegrees: Double): Double =
        ((qiblaBearing - headingDegrees) % 360.0 + 360.0) % 360.0

    /** The 16-point compass direction of a bearing: `N`, `NNE`, ... `SSE`, ... */
    public fun compassPoint(bearing: Double): String {
        val points = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        return points[(((bearing % 360 + 360) % 360) / 22.5 + 0.5).toInt() % 16]
    }
}
