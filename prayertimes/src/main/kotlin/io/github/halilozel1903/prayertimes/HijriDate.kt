package io.github.halilozel1903.prayertimes

/** The Hijri (Islamic) date of a day, as Aladhan computes it. */
public data class HijriDate(
    val day: Int,
    val month: HijriMonth,
    val year: Int,
    /** Aladhan's transliterated month name, for example `"Jumādá al-ūlá"`. */
    val monthNameTransliterated: String,
    val monthNameArabic: String,
    val weekdayArabic: String,
    /** Holidays on this date, in Aladhan's English names. Often empty. */
    val holidays: List<String>,
) {
    /** For example `2 Cemaziyelevvel 1448` (`tr`) or `2 Jumada al-Ula 1448`. */
    public fun format(languageTag: String = "en"): String = "$day ${month.displayName(languageTag)} $year"

    override fun toString(): String = format("en")
}

/** The twelve Hijri months, with the spellings used in Turkey and common English ones. */
public enum class HijriMonth(public val number: Int, public val englishName: String, public val turkishName: String) {
    Muharram(1, "Muharram", "Muharrem"),
    Safar(2, "Safar", "Safer"),
    RabiAlAwwal(3, "Rabi al-Awwal", "Rebiülevvel"),
    RabiAlThani(4, "Rabi al-Thani", "Rebiülahir"),
    JumadaAlUla(5, "Jumada al-Ula", "Cemaziyelevvel"),
    JumadaAlAkhirah(6, "Jumada al-Akhirah", "Cemaziyelahir"),
    Rajab(7, "Rajab", "Recep"),
    Shaban(8, "Sha'ban", "Şaban"),
    Ramadan(9, "Ramadan", "Ramazan"),
    Shawwal(10, "Shawwal", "Şevval"),
    DhuAlQadah(11, "Dhu al-Qadah", "Zilkade"),
    DhuAlHijjah(12, "Dhu al-Hijjah", "Zilhicce"),
    ;

    public fun displayName(languageTag: String): String =
        if (languageTag.substringBefore('-').substringBefore('_').equals("tr", ignoreCase = true)) turkishName else englishName

    public companion object {
        public fun of(number: Int): HijriMonth =
            entries.firstOrNull { it.number == number } ?: throw IllegalArgumentException("Hijri month $number")
    }
}
