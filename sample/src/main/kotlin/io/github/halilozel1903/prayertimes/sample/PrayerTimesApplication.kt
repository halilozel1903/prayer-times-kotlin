package io.github.halilozel1903.prayertimes.sample

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import io.github.halilozel1903.prayertimes.PrayerSchedule
import io.github.halilozel1903.prayertimes.PrayerTimes
import io.github.halilozel1903.prayertimes.PrayerTimesClient
import io.github.halilozel1903.prayertimes.Province
import io.github.halilozel1903.prayertimes.TurkeyProvinces
import io.ktor.client.engine.okhttp.OkHttp
import java.time.LocalDate
import java.time.YearMonth

class PrayerTimesApplication : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}

val Context.graph: AppGraph get() = (applicationContext as PrayerTimesApplication).graph

/** The app's shared objects: one client for the activity, the widget and the reminder receivers. */
class AppGraph(context: Context) {
    val client = PrayerTimesClient {
        engine = OkHttp.create()
        userAgent = "prayer-times-kotlin-sample/1.0.0 (+https://github.com/halilozel1903/prayer-times-kotlin)"
    }
    val repository = PrayerRepository(client)
    val settings = Settings(context)
}

/** Live data from Aladhan, always for the center of a province and in Turkey's time zone. */
class PrayerRepository(private val client: PrayerTimesClient) {
    /** Today and tomorrow, for the countdown. */
    suspend fun schedule(province: Province, date: LocalDate = today()): PrayerSchedule = client.schedule(province, date)

    suspend fun month(province: Province, month: YearMonth): List<PrayerTimes> = client.calendar(province, month)

    /** Aladhan's own Qibla direction, shown next to the offline computation as a cross-check. */
    suspend fun qibla(province: Province): Double = client.qibla(province.latitude, province.longitude)

    companion object {
        fun today(): LocalDate = LocalDate.now(TurkeyProvinces.zone)
    }
}

/** What the user chose. The language is not here: AppCompat keeps the per-app language. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var plate: Int
        get() = prefs.getInt(KEY_PLATE, 34).takeIf { it in 1..81 } ?: 34
        set(value) = prefs.edit { putInt(KEY_PLATE, value) }

    val province: Province get() = TurkeyProvinces.byPlate(plate)

    var remindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDERS, false)
        set(value) = prefs.edit { putBoolean(KEY_REMINDERS, value) }

    var minutesBefore: Int
        get() = prefs.getInt(KEY_MINUTES_BEFORE, 0)
        set(value) = prefs.edit { putInt(KEY_MINUTES_BEFORE, value) }

    private companion object {
        const val KEY_PLATE = "plate"
        const val KEY_REMINDERS = "reminders"
        const val KEY_MINUTES_BEFORE = "minutes_before"
    }
}
