package com.studentlauncher

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Gentle "you have been in this app for N minutes" reminders.
 *
 * No accessibility service, usage-access or overlay permission is used. The launcher knows when
 * YOU opened a distracting app from it, starts an inexact alarm, and posts a notification every
 * N minutes until you come back to the launcher (MainActivity.onResume cancels it).
 * Needs only the normal notification permission on Android 13+.
 */
object FocusReminders {
    const val CHANNEL = "focus_reminders"
    private const val NOTIF_ID = 4242
    private const val REQ = 7001

    fun start(ctx: Context, pkg: String, label: String, intervalMin: Int) {
        if (intervalMin > 0) schedule(ctx, pkg, label, intervalMin, 1)
    }

    fun schedule(ctx: Context, pkg: String, label: String, intervalMin: Int, count: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val i = Intent(ctx, FocusReminderReceiver::class.java)
            .putExtra("pkg", pkg).putExtra("label", label)
            .putExtra("interval", intervalMin).putExtra("count", count)
        val pi = PendingIntent.getBroadcast(ctx, REQ, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Inexact on purpose: needs no exact-alarm permission. May arrive a little late in Doze.
        am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + intervalMin * 60_000L, pi)
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val i = Intent(ctx, FocusReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(ctx, REQ, i, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) {
            am?.cancel(pi)
            pi.cancel()
        }
        ctx.getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
    }

    fun notify(ctx: Context, label: String, minutes: Int) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val ch = NotificationChannel(CHANNEL, "Focus reminders", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "A nudge while you are in a distracting app"
            nm.createNotificationChannel(ch)
        }
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("$minutes min on $label")
            .setContentText("Ready to get back to your studies?")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        nm.notify(NOTIF_ID, n)
    }
}

class FocusReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra("pkg") ?: return
        val label = intent.getStringExtra("label") ?: "this app"
        val interval = intent.getIntExtra("interval", 10)
        val count = intent.getIntExtra("count", 1)
        FocusReminders.notify(context, label, interval * count)
        FocusReminders.schedule(context, pkg, label, interval, count + 1)
    }
}
