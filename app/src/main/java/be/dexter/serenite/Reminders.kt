package be.dexter.serenite

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Rappels quotidiens : check-in d'humeur, journal du soir, parcours du jour.
 * Chaque rappel reprogramme le suivant. Alarmes inexactes (pas besoin d'autorisation spéciale).
 */
object Reminders {
    private const val CHANNEL = "rappels"
    const val EXTRA_OPEN = "open" // mood | journal | program
    private const val EXTRA_KIND = "kind"

    enum class Kind(val code: Int) { MOOD(10), EVENING(11), PROGRAM(12) }

    private fun enabled(s: Settings, k: Kind) = when (k) {
        Kind.MOOD -> s.remindMood
        Kind.EVENING -> s.remindEvening
        Kind.PROGRAM -> s.remindProgram
    }

    private fun minuteOf(s: Settings, k: Kind) = when (k) {
        Kind.MOOD -> s.moodTime
        Kind.EVENING -> s.eveningTime
        Kind.PROGRAM -> s.programTime
    }

    fun nextTime(minute: Int, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val today = LocalDate.now(zone)
        val t = LocalTime.of(minute / 60, minute % 60)
        var at = today.atTime(t).atZone(zone).toInstant().toEpochMilli()
        if (at <= now) at = today.plusDays(1).atTime(t).atZone(zone).toInstant().toEpochMilli()
        return at
    }

    private fun pending(ctx: Context, k: Kind) = PendingIntent.getBroadcast(
        ctx, k.code,
        Intent(ctx, ReminderReceiver::class.java).setAction("be.dexter.serenite.REMIND." + k.name).putExtra(EXTRA_KIND, k.name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun schedule(ctx: Context) {
        val s = Store.get(ctx).state.value.settings
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()
        Kind.entries.forEach { k ->
            val pi = pending(ctx, k)
            am.cancel(pi)
            if (enabled(s, k)) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime(minuteOf(s, k), now), pi)
        }
    }

    fun fire(ctx: Context, kindName: String?) {
        val k = Kind.entries.firstOrNull { it.name == kindName } ?: return
        val st = Store.get(ctx).state.value
        if (!enabled(st.settings, k)) return
        val hello = st.settings.name.takeIf { it.isNotBlank() }?.let { "$it, " } ?: ""
        val (title, text, open) = when (k) {
            Kind.MOOD -> {
                if (Stats.moodToday(st) != null) return
                Triple("Comment te sens-tu ?", "${hello}prends 30 secondes pour ton check-in du jour.", "mood")
            }
            Kind.EVENING -> Triple("Moment journal", "${hello}trois choses positives de ta journée ?", "journal")
            Kind.PROGRAM -> {
                val prog = Content.program(st.activeProgram) ?: return
                val p = st.programs[prog.id]
                val next = Stats.nextDay(p, prog.days.size) ?: return
                if (!Stats.isUnlocked(p, next, st.settings.oneDayPerDay)) return
                Triple(prog.title, "Jour ${next + 1} : ${prog.days[next].title}", "program")
            }
        }
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Rappels", NotificationManager.IMPORTANCE_DEFAULT))
        val tap = PendingIntent.getActivity(
            ctx, k.code + 100,
            Intent(ctx, MainActivity::class.java).putExtra(EXTRA_OPEN, open)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(k.code, n) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminders.fire(ctx, intent.getStringExtra("kind"))
        Reminders.schedule(ctx)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminders.schedule(ctx)
        Widget.refresh(ctx)
    }
}
