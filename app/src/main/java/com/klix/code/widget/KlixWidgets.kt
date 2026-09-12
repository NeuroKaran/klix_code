package com.klix.code.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object KlixWidgets {
    const val ACTION_NEW_PROJECT = "com.klix.code.action.NEW_PROJECT"
    const val ACTION_OPEN_FOLDER = "com.klix.code.action.OPEN_FOLDER"
    const val ACTION_ASK_AGENT = "com.klix.code.action.ASK_AGENT"
    const val ACTION_OPEN_PROJECT = "com.klix.code.action.OPEN_PROJECT"
    const val ACTION_SEE_ALL_PROJECTS = "com.klix.code.action.SEE_ALL_PROJECTS"

    const val EXTRA_PROJECT_URI = "extra_project_uri"
    const val EXTRA_PROJECT_NAME = "extra_project_name"

    fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // 1. Update Hub Widgets
        val hubComponent = ComponentName(context, KlixHubWidgetProvider::class.java)
        val hubIds = appWidgetManager.getAppWidgetIds(hubComponent)
        if (hubIds.isNotEmpty()) {
            val intent = Intent(context, KlixHubWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, hubIds)
            }
            context.sendBroadcast(intent)
        }

        // 2. Update Recent Projects Widgets
        val recentComponent = ComponentName(context, KlixRecentProjectsWidgetProvider::class.java)
        val recentIds = appWidgetManager.getAppWidgetIds(recentComponent)
        if (recentIds.isNotEmpty()) {
            val intent = Intent(context, KlixRecentProjectsWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, recentIds)
            }
            context.sendBroadcast(intent)
        }

        // 3. Update Streak Widgets
        val streakComponent = ComponentName(context, KlixStreakWidgetProvider::class.java)
        val streakIds = appWidgetManager.getAppWidgetIds(streakComponent)
        if (streakIds.isNotEmpty()) {
            val intent = Intent(context, KlixStreakWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, streakIds)
            }
            context.sendBroadcast(intent)
        }

        // 4. Update Companion Widgets
        val companionComponent = ComponentName(context, KlixCompanionWidgetProvider::class.java)
        val companionIds = appWidgetManager.getAppWidgetIds(companionComponent)
        if (companionIds.isNotEmpty()) {
            val intent = Intent(context, KlixCompanionWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, companionIds)
            }
            context.sendBroadcast(intent)
        }
    }
}
