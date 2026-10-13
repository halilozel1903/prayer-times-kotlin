package io.github.halilozel1903.prayertimes

/**
 * An Aladhan calculation method, sent as `method=`. [Diyanet] (13) is the default of this library: the method of
 * the Presidency of Religious Affairs of Turkey (Diyanet İşleri Başkanlığı), with Fajr at 18° and Isha at 17°.
 *
 * Any method Aladhan knows can be used with its id, for example `CalculationMethod(15)`.
 */
@JvmInline
public value class CalculationMethod(public val id: Int) {
    public companion object {
        public val Karachi: CalculationMethod = CalculationMethod(1)
        public val Isna: CalculationMethod = CalculationMethod(2)
        public val MuslimWorldLeague: CalculationMethod = CalculationMethod(3)
        public val UmmAlQura: CalculationMethod = CalculationMethod(4)
        public val Egypt: CalculationMethod = CalculationMethod(5)
        public val Tehran: CalculationMethod = CalculationMethod(7)
        public val Gulf: CalculationMethod = CalculationMethod(8)
        public val Kuwait: CalculationMethod = CalculationMethod(9)
        public val Qatar: CalculationMethod = CalculationMethod(10)
        public val Singapore: CalculationMethod = CalculationMethod(11)
        public val France: CalculationMethod = CalculationMethod(12)

        /** Diyanet İşleri Başkanlığı, Turkey. The default. */
        public val Diyanet: CalculationMethod = CalculationMethod(13)
        public val Russia: CalculationMethod = CalculationMethod(14)
        public val Moonsighting: CalculationMethod = CalculationMethod(15)
        public val Dubai: CalculationMethod = CalculationMethod(16)
    }
}

/** The juristic school for Asr, sent as `school=`. */
public enum class School(public val id: Int) {
    /** Shafi'i, Maliki and Hanbali: Asr when a shadow equals its object's length. Aladhan's default. */
    Standard(0),

    /** Hanafi: Asr when a shadow is twice its object's length (later). */
    Hanafi(1),
}

/**
 * Minute offsets that Aladhan adds to its computed times, sent as `tune=`. Use them to match a mosque or an
 * official table that differs by a minute or two.
 *
 * ```kotlin
 * Tune(fajr = 1, dhuhr = -1)
 * ```
 */
public data class Tune(
    val imsak: Int = 0,
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val sunset: Int = 0,
    val isha: Int = 0,
    val midnight: Int = 0,
) {
    /** `true` when every offset is zero, and nothing needs to be sent. */
    public val isZero: Boolean get() = this == None

    /** Aladhan's format: Imsak, Fajr, Sunrise, Dhuhr, Asr, Maghrib, Sunset, Isha, Midnight. */
    public fun toQueryValue(): String =
        listOf(imsak, fajr, sunrise, dhuhr, asr, maghrib, sunset, isha, midnight).joinToString(",")

    public companion object {
        public val None: Tune = Tune()

        /** Offsets for the five prayers and sunrise, keyed by [Prayer]. */
        public fun of(offsets: Map<Prayer, Int>): Tune = Tune(
            fajr = offsets[Prayer.Fajr] ?: 0,
            sunrise = offsets[Prayer.Sunrise] ?: 0,
            dhuhr = offsets[Prayer.Dhuhr] ?: 0,
            asr = offsets[Prayer.Asr] ?: 0,
            maghrib = offsets[Prayer.Maghrib] ?: 0,
            isha = offsets[Prayer.Isha] ?: 0,
        )
    }
}
