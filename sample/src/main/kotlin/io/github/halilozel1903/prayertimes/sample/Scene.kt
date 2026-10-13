package io.github.halilozel1903.prayertimes.sample

/**
 * Screenshot scenes, picked with the `scene` intent extra by `scripts/screenshots.sh`. Each opens a fixed
 * province, language and screen, and loads today's times live from api.aladhan.com like a normal launch.
 * Nothing is persisted: the user's own city and language are left alone.
 */
enum class Scene(
    val key: String,
    val plate: Int,
    /** The per-app language the scene runs in, set through [AppLanguage] like the in-app switch. */
    val language: AppLanguage,
    val tab: Tab,
    val pickerOpen: Boolean = false,
    /** How many live loads must finish before the scene is complete. */
    val loads: Int = 1,
) {
    TodayTr("today-tr", plate = 34, language = AppLanguage.Turkish, tab = Tab.Today),
    TodayEn("today-en", plate = 35, language = AppLanguage.English, tab = Tab.Today),
    CityPickerTr("city-picker-tr", plate = 34, language = AppLanguage.Turkish, tab = Tab.Today, pickerOpen = true),
    Month("month", plate = 6, language = AppLanguage.Turkish, tab = Tab.Month),
    Qibla("qibla", plate = 6, language = AppLanguage.English, tab = Tab.Qibla),
    Tablet("tablet", plate = 16, language = AppLanguage.English, tab = Tab.Today),
    ;

    companion object {
        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}

enum class Tab { Today, Month, Qibla, Settings }
