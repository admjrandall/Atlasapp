package com.jmr.atlas.integrations

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.jmr.atlas.AtlasApp
import com.jmr.atlas.MainActivity
import com.jmr.atlas.R
import com.jmr.atlas.data.Briefing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Morning briefing and task reminders. Alarms are inexact windows (no special alarm permission): the briefing within
 * 15 minutes of the chosen time, reminders within 5. Phone notifications mirror to a paired Galaxy Watch automatically;
 * the Done / Snooze / voice-reply actions are marked for the watch too. Nothing here needs the network.
 */
object Notify {
    const val CH_BRIEF = "briefing"
    const val CH_REMIND = "reminders"
    const val KEY_REPLY = "reply"
    private const val BRIEF_ID = 1

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_BRIEF, "Morning briefing", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "One summary each morning" })
        nm.createNotificationChannel(NotificationChannel(CH_REMIND, "Task reminders", NotificationManager.IMPORTANCE_HIGH).apply { description = "Reminders you set on tasks" })
    }

    fun canPost(ctx: Context) = android.os.Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun pi(ctx: Context, code: Int, i: Intent, mutable: Boolean = false) =
        PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE))
    private fun openApp(ctx: Context, code: Int, objectId: String? = null) = PendingIntent.getActivity(ctx, code,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).apply { objectId?.let { putExtra(MainActivity.EXTRA_OPEN, it) } },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    // ---------------------------------------------------------------- briefing
    fun nextBriefing(minutes: Int, weekdaysOnly: Boolean, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        var t = now.toLocalDate().atTime(minutes / 60, minutes % 60)
        while (!t.isAfter(now) || (weekdaysOnly && t.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))) t = t.plusDays(1)
        return t
    }

    fun scheduleBriefing(ctx: Context) {
        val s = (ctx.applicationContext as AtlasApp).repo.settings
        val am = ctx.getSystemService(AlarmManager::class.java)
        val p = pi(ctx, BRIEF_ID, Intent(ctx, Receiver::class.java).setAction(ACTION_BRIEF))
        am.cancel(p)
        if (!s.briefingOn) return
        val at = nextBriefing(s.briefingMinutes, s.briefingWeekdaysOnly).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setWindow(AlarmManager.RTC_WAKEUP, at, 15 * 60_000L, p)
    }

    fun postBriefing(ctx: Context) {
        val app = ctx.applicationContext as AtlasApp
        if (!canPost(ctx)) return
        val events = if (app.repo.settings.calendarInBriefing) app.calendars.instances(app.repo.settings.calendars, LocalDate.now(), 1) else emptyList()
        val b = Briefing.build(app.repo.snap, events)
        val lines = b.needsYou.take(4).map { "• ${it.task.title} — ${it.context}" } + b.today.take(2).map { "• ${if (it.allDay) "All day" else Briefing.time(it.begin)} ${it.title}" }
        val n = NotificationCompat.Builder(ctx, CH_BRIEF).setSmallIcon(R.drawable.ic_stat_atlas).setContentTitle("Good morning")
            .setContentText(b.headline).setStyle(NotificationCompat.BigTextStyle().bigText((listOf(b.headline) + lines).joinToString("\n")))
            .setContentIntent(openApp(ctx, BRIEF_ID)).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
        runCatching { NotificationManagerCompat.from(ctx).notify(BRIEF_ID, n) }
    }

    // ---------------------------------------------------------------- reminders
    private fun code(id: String) = 10_000 + (id.hashCode() and 0x3fffffff) % 1_000_000

    /** (Re)schedules every future reminder. Cheap: called after changes, debounced by the app. */
    fun scheduleReminders(ctx: Context) {
        val app = ctx.applicationContext as AtlasApp
        val am = ctx.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        val prefs = ctx.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val before = prefs.getStringSet("ids", emptySet())!!
        val tasks = app.repo.snap.ofType(com.jmr.atlas.model.ObjType.TASK).filter { !it.done && (it.remindAt ?: 0) > now }
        (before - tasks.map { it.id }.toSet()).forEach { am.cancel(pi(ctx, code(it), Intent(ctx, Receiver::class.java).setAction(ACTION_REMIND).putExtra(EXTRA_ID, it))) }
        tasks.forEach { t -> am.setWindow(AlarmManager.RTC_WAKEUP, t.remindAt!!, 5 * 60_000L, pi(ctx, code(t.id), Intent(ctx, Receiver::class.java).setAction(ACTION_REMIND).putExtra(EXTRA_ID, t.id))) }
        prefs.edit().putStringSet("ids", tasks.map { it.id }.toSet()).apply()
    }

    fun postReminder(ctx: Context, id: String) {
        val app = ctx.applicationContext as AtlasApp
        val t = app.repo.snap.objects[id]?.takeIf { !it.done && !it.trashed } ?: return
        if (!canPost(ctx)) return
        val c = code(id)
        val done = NotificationCompat.Action.Builder(R.drawable.ic_stat_atlas, "Done", pi(ctx, c + 1, Intent(ctx, Receiver::class.java).setAction(ACTION_DONE).putExtra(EXTRA_ID, id))).build()
        val snooze = NotificationCompat.Action.Builder(R.drawable.ic_stat_atlas, "Snooze 1 h", pi(ctx, c + 2, Intent(ctx, Receiver::class.java).setAction(ACTION_SNOOZE).putExtra(EXTRA_ID, id))).build()
        val reply = NotificationCompat.Action.Builder(R.drawable.ic_stat_atlas, "Add a note",
            pi(ctx, c + 3, Intent(ctx, Receiver::class.java).setAction(ACTION_REPLY).putExtra(EXTRA_ID, id), mutable = true))
            .addRemoteInput(RemoteInput.Builder(KEY_REPLY).setLabel("Note for ${t.title}").build()).setAllowGeneratedReplies(false).build()
        val ctxLine = app.repo.snap.neighbours(id, false).map { it.first }.firstOrNull { it.type == com.jmr.atlas.model.ObjType.PROJECT }?.title
        val n = NotificationCompat.Builder(ctx, CH_REMIND).setSmallIcon(R.drawable.ic_stat_atlas).setContentTitle(t.title)
            .setContentText(listOfNotNull(ctxLine, t.deadline?.let { "Deadline ${com.jmr.atlas.ui.components.dayDate(it)}" }).joinToString(" · ").ifBlank { "Reminder" })
            .setContentIntent(openApp(ctx, c, id)).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(done).addAction(snooze).addAction(reply)
            .extend(NotificationCompat.WearableExtender().addAction(done).addAction(snooze).addAction(reply))
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(c, n) }
    }

    const val ACTION_BRIEF = "com.jmr.atlas.BRIEF"
    const val ACTION_REMIND = "com.jmr.atlas.REMIND"
    const val ACTION_DONE = "com.jmr.atlas.DONE"
    const val ACTION_SNOOZE = "com.jmr.atlas.SNOOZE"
    const val ACTION_REPLY = "com.jmr.atlas.REPLY"
    const val EXTRA_ID = "id"

    /** One receiver for alarms, notification actions and boot. Not exported except for BOOT_COMPLETED. */
    class Receiver : BroadcastReceiver() {
        override fun onReceive(ctx: Context, i: Intent) {
            val app = ctx.applicationContext as AtlasApp
            val id = i.getStringExtra(EXTRA_ID)
            when (i.action) {
                Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIMEZONE_CHANGED -> { scheduleBriefing(ctx); scheduleReminders(ctx) }
                ACTION_BRIEF -> { postBriefing(ctx); scheduleBriefing(ctx) }
                ACTION_REMIND -> id?.let { postReminder(ctx, it) }
                ACTION_DONE -> id?.let { app.repo.complete(it); NotificationManagerCompat.from(ctx).cancel(code(it)) }
                ACTION_SNOOZE -> id?.let { app.repo.snap.objects[it]?.let { t -> app.repo.update(t.copy(remindAt = System.currentTimeMillis() + 3_600_000L), "Snoozed 1 hour") }; NotificationManagerCompat.from(ctx).cancel(code(it)); scheduleReminders(ctx) }
                ACTION_REPLY -> {
                    val text = RemoteInput.getResultsFromIntent(i)?.getCharSequence(KEY_REPLY)?.toString()?.trim()
                    if (id != null && !text.isNullOrBlank()) {
                        val cap = app.repo.captureText(text)
                        app.repo.snap.captures.firstOrNull { it.id == cap }?.noteId?.let { note -> app.repo.link(note, id, com.jmr.atlas.model.RelType.RELATED_TO) }
                        NotificationManagerCompat.from(ctx).cancel(code(id))
                    }
                }
            }
        }
    }
}
