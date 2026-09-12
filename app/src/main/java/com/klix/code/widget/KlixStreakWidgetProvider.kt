package com.klix.code.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.klix.code.MainActivity
import com.klix.code.R
import com.klix.code.data.stats.StreakManager

class KlixStreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_streak)

        val streak = StreakManager.getStreakCount(context)
        views.setTextViewText(R.id.widget_streak_count, "$streak days")

        val weeklyActive = StreakManager.getWeeklyActiveDays(context)
        val dotIds = listOf(
            R.id.widget_dot_mon,
            R.id.widget_dot_tue,
            R.id.widget_dot_wed,
            R.id.widget_dot_thu,
            R.id.widget_dot_fri,
            R.id.widget_dot_sat,
            R.id.widget_dot_sun
        )

        for (i in 0 until 7) {
            val isActive = if (i < weeklyActive.size) weeklyActive[i] else false
            val res = if (isActive) R.drawable.widget_dot_active else R.drawable.widget_dot_inactive
            views.setImageViewResource(dotIds[i], res)
        }

        // Tap widget -> Open KlixCode
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            20,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_streak_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
