package com.klix.code.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.klix.code.MainActivity
import com.klix.code.R

class KlixHubWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_hub)

        // 1. Header Click -> Open KlixCode
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_hub_header_img, mainPendingIntent)

        // 2. New Project Click -> Open Folder Picker in MainActivity
        val newProjIntent = Intent(context, MainActivity::class.java).apply {
            action = KlixWidgets.ACTION_NEW_PROJECT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val newProjPendingIntent = PendingIntent.getActivity(
            context,
            1,
            newProjIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_new_project, newProjPendingIntent)

        // 3. Open Folder Click
        val openFolderIntent = Intent(context, MainActivity::class.java).apply {
            action = KlixWidgets.ACTION_OPEN_FOLDER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openFolderPendingIntent = PendingIntent.getActivity(
            context,
            2,
            openFolderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_open_folder, openFolderPendingIntent)

        // 4. Ask Agent Click
        val askAgentIntent = Intent(context, MainActivity::class.java).apply {
            action = KlixWidgets.ACTION_ASK_AGENT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val askAgentPendingIntent = PendingIntent.getActivity(
            context,
            3,
            askAgentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_ask_agent, askAgentPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
