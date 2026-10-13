package io.github.halilozel1903.prayertimes.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import io.github.halilozel1903.prayertimes.Prayer
import io.github.halilozel1903.prayertimes.PrayerSchedule
import io.github.halilozel1903.prayertimes.PrayerTime
import java.time.Instant

/** A reminder delivered to your receiver: [prayer] starts at [time], and the alarm rang [minutesBefore] earlier. */
public data class PrayerReminder(
    val prayer: Prayer,
    val time: Instant,
    val minutesBefore: Int,
    /** What you passed as `label` when scheduling, for example the city name. */
    val label: String?,
)

/**
 * Schedules one alarm for the next occurrence of each chosen prayer, delivered to your [receiver] as a broadcast.
 * Read it there with [PrayerReminderScheduler.reminderOf], show a notification, and schedule again from a fresh
 * [PrayerSchedule] so the following day is covered.
 *
 * ```kotlin
 * val scheduler = PrayerReminderScheduler(context, PrayerAlarmReceiver::class.java)
 * if (!scheduler.canScheduleExactAlarms()) startActivity(scheduler.exactAlarmSettingsIntent())
 * scheduler.schedule(client.schedule(istanbul), minutesBefore = 10, label = "İstanbul")
 * ```
 *
 * Alarms are exact (`setExactAndAllowWhileIdle`) when the app may schedule exact alarms, and fall back to
 * `setAndAllowWhileIdle` (Android may deliver them some minutes late) when it may not. Alarms do not survive a
 * reboot: reschedule from a `BOOT_COMPLETED` receiver.
 */
public class PrayerReminderScheduler(
    context: Context,
    private val receiver: Class<out BroadcastReceiver>,
) {
    private val context = context.applicationContext
    private val alarmManager: AlarmManager = requireNotNull(this.context.getSystemService()) { "No AlarmManager" }

    /** `true` when alarms can ring at the exact minute: always below Android 12, otherwise when the user allowed it. */
    public fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Opens the system screen where the user allows exact alarms for this app, or `null` below Android 12. */
    public fun exactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            null
        }

    /**
     * Schedules the next occurrence after [now] of each of [prayers] in [schedule], [minutesBefore] minutes early,
     * replacing reminders scheduled before. Prayers that are not chosen are cancelled. Returns the reminders set.
     */
    public fun schedule(
        schedule: PrayerSchedule,
        prayers: Set<Prayer> = Prayer.prayers.toSet(),
        minutesBefore: Int = 0,
        label: String? = null,
        now: Instant = Instant.now(),
    ): List<PrayerReminder> = schedule(schedule.times, prayers, minutesBefore, label, now)

    /** Like the [PrayerSchedule] overload, from any list of times. */
    public fun schedule(
        times: List<PrayerTime>,
        prayers: Set<Prayer> = Prayer.prayers.toSet(),
        minutesBefore: Int = 0,
        label: String? = null,
        now: Instant = Instant.now(),
    ): List<PrayerReminder> {
        require(minutesBefore >= 0) { "minutesBefore must not be negative" }
        val scheduled = mutableListOf<PrayerReminder>()
        for (prayer in Prayer.entries) {
            if (prayer !in prayers) {
                cancel(prayer)
                continue
            }
            val next = times
                .filter { it.prayer == prayer }
                .map { it.instant }
                .sorted()
                .firstOrNull { it.minusSeconds(minutesBefore * 60L).isAfter(now) }
            if (next == null) {
                cancel(prayer)
                continue
            }
            val reminder = PrayerReminder(prayer, next, minutesBefore, label)
            setAlarm(reminder)
            scheduled += reminder
        }
        return scheduled
    }

    /** Cancels the reminder of [prayer]. */
    public fun cancel(prayer: Prayer) {
        pendingIntent(prayer, null, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /** Cancels every reminder. */
    public fun cancelAll() {
        Prayer.entries.forEach(::cancel)
    }

    private fun setAlarm(reminder: PrayerReminder) {
        val triggerAt = reminder.time.toEpochMilli() - reminder.minutesBefore * 60_000L
        val operation = requireNotNull(pendingIntent(reminder.prayer, reminder, PendingIntent.FLAG_UPDATE_CURRENT))
        if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
                return
            } catch (_: SecurityException) {
                // The permission was revoked between the check and the call; use an inexact alarm.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
    }

    private fun pendingIntent(prayer: Prayer, reminder: PrayerReminder?, flags: Int): PendingIntent? {
        val intent = Intent(context, receiver)
            .setAction(ACTION_REMINDER)
            // One alarm per prayer: the data uri keeps the PendingIntents apart.
            .setData(Uri.parse("prayer-reminder://${prayer.apiName}"))
        if (reminder != null) {
            intent.putExtra(EXTRA_PRAYER, reminder.prayer.apiName)
                .putExtra(EXTRA_TIME, reminder.time.toEpochMilli())
                .putExtra(EXTRA_MINUTES_BEFORE, reminder.minutesBefore)
                .putExtra(EXTRA_LABEL, reminder.label)
        }
        return PendingIntent.getBroadcast(context, prayer.ordinal, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    public companion object {
        public const val ACTION_REMINDER: String = "io.github.halilozel1903.prayertimes.action.REMINDER"
        public const val EXTRA_PRAYER: String = "io.github.halilozel1903.prayertimes.extra.PRAYER"
        public const val EXTRA_TIME: String = "io.github.halilozel1903.prayertimes.extra.TIME"
        public const val EXTRA_MINUTES_BEFORE: String = "io.github.halilozel1903.prayertimes.extra.MINUTES_BEFORE"
        public const val EXTRA_LABEL: String = "io.github.halilozel1903.prayertimes.extra.LABEL"

        /** The reminder an alarm broadcast carries, or `null` for any other intent. */
        public fun reminderOf(intent: Intent): PrayerReminder? {
            if (intent.action != ACTION_REMINDER) return null
            val prayer = intent.getStringExtra(EXTRA_PRAYER)?.let(Prayer::fromApiName) ?: return null
            val time = intent.getLongExtra(EXTRA_TIME, -1L).takeIf { it >= 0 } ?: return null
            return PrayerReminder(
                prayer = prayer,
                time = Instant.ofEpochMilli(time),
                minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 0),
                label = intent.getStringExtra(EXTRA_LABEL),
            )
        }
    }
}
