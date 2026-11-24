package be.dexter.serenite

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId

/** Widget : check-in d'humeur en un appui (ouvre le check-in avec le niveau choisi). */
class MoodWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = Widget.refresh(ctx)

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == Widget.ACTION_TICK) Widget.refresh(ctx)
    }
}

object Widget {
    const val ACTION_TICK = "be.dexter.serenite.WIDGET_TICK"
    private val ids = listOf(R.id.m1, R.id.m2, R.id.m3, R.id.m4, R.id.m5)

    fun refresh(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val wids = mgr.getAppWidgetIds(ComponentName(ctx, MoodWidget::class.java))
        if (wids.isEmpty()) return
        val s = Store.get(ctx).state.value
        val today = Stats.today()
        val m = Stats.moodToday(s, today)
        val streak = Stats.streak(s, today)
        val v = RemoteViews(ctx.packageName, R.layout.widget)

        v.setTextViewText(R.id.title, if (m == null) "Comment te sens-tu ?" else "Aujourd'hui : ${Content.moodLabels[m.mood - 1]}")
        v.setTextViewText(R.id.detail, if (streak > 0) "🔥 $streak jour" + (if (streak > 1) "s" else "") + " d'affilée" else "Appuie sur une humeur")
        ids.forEachIndexed { i, id ->
            v.setFloat(id, "setAlpha", if (m == null || m.mood == i + 1) 1f else 0.45f)
            val pi = PendingIntent.getActivity(
                ctx, 200 + i,
                Intent(ctx, MainActivity::class.java).putExtra(Reminders.EXTRA_OPEN, "mood:${i + 1}")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            v.setOnClickPendingIntent(id, pi)
        }
        v.setOnClickPendingIntent(
            R.id.root,
            PendingIntent.getActivity(ctx, 199, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        )
        mgr.updateAppWidget(wids, v)

        // Remise à zéro juste après minuit.
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val tick = PendingIntent.getBroadcast(
            ctx, 198, Intent(ctx, MoodWidget::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val zone = ZoneId.systemDefault()
        val midnight = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 60_000
        am.set(AlarmManager.RTC, midnight, tick)
    }
}
