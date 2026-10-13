package io.github.halilozel1903.prayertimes

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/** Text helpers for Turkish names, independent of the device's locale. */
public object TurkishText {
    /** The Turkish locale (`tr-TR`). */
    public val locale: Locale = Locale.forLanguageTag("tr-TR")

    private val marks = Regex("\\p{M}+")

    /**
     * Folds [text] for searching: lower case, Turkish letters to their Latin base (ç→c, ğ→g, ı/İ/I→i, ö→o, ş→s,
     * ü→u, â→a, î→i, û→u) and surrounding spaces trimmed. `"İSTANBUL"`, `"istanbul"` and `"Istanbul"` all fold to
     * `"istanbul"`; `"Şanlıurfa"` and `"sanliurfa"` to `"sanliurfa"`.
     *
     * Works the same in every default locale: plain [String.lowercase] turns `"I"` into `"ı"` on a Turkish device and
     * `"İ"` into `"i̇"` (with a combining dot) everywhere else.
     */
    public fun fold(text: String): String {
        val builder = StringBuilder(text.length)
        for (char in text) {
            when (char) {
                'ı', 'I', 'İ' -> builder.append('i')
                else -> builder.append(char)
            }
        }
        val decomposed = Normalizer.normalize(builder, Normalizer.Form.NFD)
        return marks.replace(decomposed, "").lowercase(Locale.ROOT).trim()
    }

    /** Turkish alphabetical order: Ç after C, I before İ, Ş after S, and so on. */
    public val collator: Comparator<String> = Collator.getInstance(locale).let { collator ->
        Comparator { a, b -> synchronized(collator) { collator.compare(a, b) } }
    }

    /** Upper case with Turkish rules: `"izmir"` becomes `"İZMİR"`, `"ığdır"` becomes `"IĞDIR"`. */
    public fun uppercase(text: String): String = text.uppercase(locale)
}
