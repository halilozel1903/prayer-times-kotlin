package io.github.halilozel1903.prayertimes

/**
 * The six daily times shown in Turkish prayer calendars: the five prayers and sunrise.
 *
 * [turkishName] follows Diyanet's calendar. Note that Diyanet's **İmsak** is the start of Fajr, which is Aladhan's
 * `Fajr` with method 13; Aladhan's own `Imsak` field (a fixed 10 minutes earlier) is available as
 * [PrayerTimes.imsak] but is not Diyanet's İmsak.
 */
public enum class Prayer(
    /** The key in Aladhan's `timings` object. */
    public val apiName: String,
    public val englishName: String,
    public val turkishName: String,
) {
    Fajr("Fajr", "Fajr", "İmsak"),
    Sunrise("Sunrise", "Sunrise", "Güneş"),
    Dhuhr("Dhuhr", "Dhuhr", "Öğle"),
    Asr("Asr", "Asr", "İkindi"),
    Maghrib("Maghrib", "Maghrib", "Akşam"),
    Isha("Isha", "Isha", "Yatsı"),
    ;

    /** `false` for [Sunrise], which is shown in calendars but is not a prayer. */
    public val isPrayer: Boolean get() = this != Sunrise

    /** The name in [languageTag]'s language: Turkish for `tr`, English otherwise. */
    public fun displayName(languageTag: String): String =
        if (languageTag.substringBefore('-').substringBefore('_').equals("tr", ignoreCase = true)) turkishName else englishName

    public companion object {
        /** The five prayers, without sunrise. */
        public val prayers: List<Prayer> = entries.filter { it.isPrayer }

        public fun fromApiName(name: String): Prayer? = entries.firstOrNull { it.apiName.equals(name, ignoreCase = true) }
    }
}
