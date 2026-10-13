package io.github.halilozel1903.prayertimes.sample

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * The in-app language switch. AppCompat stores the choice and applies it: through the system's per-app languages
 * (`LocaleManager`) on Android 13+, where it also appears in system Settings, and by itself below that
 * (`autoStoreLocales` in the manifest).
 */
enum class AppLanguage(val tag: String?) {
    System(null),
    Turkish("tr"),
    English("en"),
    ;

    companion object {
        fun current(): AppLanguage {
            val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return entries.firstOrNull { it.tag != null && tags.substringBefore(',').startsWith(it.tag) } ?: System
        }

        /** Switches the app's language; open activities are recreated in it. */
        fun apply(language: AppLanguage) {
            val locales = language.tag?.let { LocaleListCompat.forLanguageTags(it) } ?: LocaleListCompat.getEmptyLocaleList()
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
