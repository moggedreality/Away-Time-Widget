package com.stxrs.awaytime

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews

class AwayTimeWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        scheduleRefresh(context)
        ids.forEach { updateWidget(context, manager, it) }
    }

    // Redraw when the user resizes the widget so the card always fits.
    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, manager, id, newOptions)
        updateWidget(context, manager, id)
    }

    override fun onEnabled(context: Context) {
        scheduleRefresh(context)
    }

    override fun onDisabled(context: Context) {
        cancelRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refreshAll(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.stxrs.awaytime.ACTION_REFRESH"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            widgetIds(context, manager).forEach { updateWidget(context, manager, it) }
        }

        /** Call after any setting changes: re-arms the timer and redraws every widget. */
        fun applySettings(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = widgetIds(context, manager)
            if (ids.isEmpty()) cancelRefresh(context) else scheduleRefresh(context)
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun widgetIds(context: Context, manager: AppWidgetManager): IntArray =
            manager.getAppWidgetIds(ComponentName(context, AwayTimeWidget::class.java))

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val settings = WidgetSettings.get(context)
            val result = AwayTimeCalculator.compute(context)
            val (wDp, hDp) = widgetSizeDp(context, manager, id)

            val views: RemoteViews
            if (settings.theme == Themes.ADAPTIVE) {
                // Both looks are drawn. layout/ shows the light one and layout-night/ the dark one,
                // so the launcher swaps them with the device theme without waiting for a refresh.
                views = RemoteViews(context.packageName, R.layout.widget_card_adaptive)
                views.setImageViewBitmap(
                    R.id.img_dark,
                    WidgetRenderer.render(context, settings, result, wDp, hDp, Themes.resolve(settings, night = true)),
                )
                views.setImageViewBitmap(
                    R.id.img_light,
                    WidgetRenderer.render(context, settings, result, wDp, hDp, Themes.resolve(settings, night = false)),
                )
            } else {
                views = RemoteViews(context.packageName, R.layout.widget_card)
                val palette = Themes.resolve(settings, night = false)
                views.setImageViewBitmap(
                    R.id.card,
                    WidgetRenderer.render(context, settings, result, wDp, hDp, palette),
                )
            }

            views.setContentDescription(
                R.id.root,
                if (result == null) "Away Time. Tap to grant usage access."
                else "Away today: ${AwayTimeCalculator.format(result.awayMs)}",
            )
            views.setOnClickPendingIntent(
                R.id.root,
                if (result == null) openAppIntent(context) else refreshIntent(context),
            )
            manager.updateAppWidget(id, views)
        }

        private fun widgetSizeDp(context: Context, manager: AppWidgetManager, id: Int): Pair<Int, Int> {
            val options = manager.getAppWidgetOptions(id)
            val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            val w = options.getInt(
                if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
                else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                0,
            )
            val h = options.getInt(
                if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                0,
            )
            return Pair(if (w > 0) w else 180, if (h > 0) h else 110)
        }

        // Non-wakeup alarm: fires while the phone is awake, so the widget catches up shortly after
        // you pick the phone up, without an always-on service. Interval comes from the settings.
        fun scheduleRefresh(context: Context) {
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarm.cancel(refreshIntent(context))
            val minutes = WidgetSettings.get(context).refreshMinutes
            if (minutes <= 0) return
            val interval = minutes * 60_000L
            alarm.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + interval,
                interval,
                refreshIntent(context),
            )
        }

        fun cancelRefresh(context: Context) {
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarm.cancel(refreshIntent(context))
        }

        private fun refreshIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, AwayTimeWidget::class.java).setAction(ACTION_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun openAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
