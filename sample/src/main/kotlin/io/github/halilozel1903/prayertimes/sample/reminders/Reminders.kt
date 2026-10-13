package io.github.halilozel1903.prayertimes.sample.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.halilozel1903.prayertimes.TurkeyProvinces
import io.github.halilozel1903.prayertimes.android.PrayerReminder
import io.github.halilozel1903.prayertimes.android.PrayerReminderScheduler
import io.github.halilozel1903.prayertimes.sample.MainActivity
import io.github.halilozel1903.prayertimes.sample.R
import io.github.halilozel1903.prayertimes.sample.clock
import io.github.halilozel1903.prayertimes.sample.graph
import io.github.halilozel1903.prayertimes.sample.label
import io.github.halilozel1903.prayertimes.sample.widget.PrayerWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Glue between the settings, live times from Aladhan and [PrayerReminderScheduler]. */
object Reminders {
    private const val CHANNEL_ID = "prayer_reminders"

    /**
     * Schedules the next reminder of each prayer for the saved province from a fresh schedule (today and tomorrow),
     * or cancels them all when reminders are off. Throws `PrayerTimesException` when Aladhan cannot be reached.
     */
    suspend fun refresh(context: Context): List<PrayerReminder> {
        val graph = context.graph
        val scheduler = PrayerReminderScheduler(context, ReminderReceiver::class.java)
        if (!graph.settings.remindersEnabled) {
            scheduler.cancelAll()
            return emptyList()
        }
        val province = graph.settings.province
        val schedule = graph.repository.schedule(province)
        return scheduler.schedule(schedule, minutesBefore = graph.settings.minutesBefore, label = province.name)
    }

    fun notify(context: Context, reminder: PrayerReminder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val localized = context.inAppLanguage()
        ensureChannel(localized)
        val name = localized.getString(reminder.prayer.label)
        val title = if (reminder.minutesBefore > 0) {
            localized.getString(R.string.notification_title_before, name, reminder.minutesBefore)
        } else {
            localized.getString(R.string.notification_title, name)
        }
        val text = localized.getString(
            R.string.notification_text,
            reminder.label ?: localized.getString(R.string.app_name),
            reminder.time.atZone(TurkeyProvinces.zone).clock(),
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(reminder.prayer.ordinal, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_HIGH,
        )
        manager.createNotificationChannel(channel)
    }
}

/**
 * A context in the language chosen in the app. On Android 13+ the system applies the per-app language to every
 * context already; below that only AppCompat activities get it, so receivers and the widget ask AppCompat.
 */
fun Context.inAppLanguage(): Context {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return this
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()))
    return createConfigurationContext(configuration)
}

/** Runs [block] off the main thread and keeps the receiver alive until it is done. */
fun BroadcastReceiver.doAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** Shows the reminder, then schedules the next one and refreshes the widget. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminder = PrayerReminderScheduler.reminderOf(intent) ?: return
        Reminders.notify(context, reminder)
        doAsync {
            runCatching { Reminders.refresh(context) }
            runCatching { PrayerWidget.refresh(context) }
        }
    }
}

/** Alarms do not survive a reboot, a time zone change or a change of the exact alarm permission: schedule again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in Actions) return
        doAsync {
            runCatching { Reminders.refresh(context) }
            runCatching { PrayerWidget.refresh(context) }
        }
    }
}

private val Actions = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_TIMEZONE_CHANGED,
    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
)
