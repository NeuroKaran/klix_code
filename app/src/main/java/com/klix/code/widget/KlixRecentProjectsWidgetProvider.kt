package com.klix.code.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews
import com.klix.code.MainActivity
import com.klix.code.R
import com.klix.code.data.model.ProjectItem
import com.klix.code.data.repository.WorkspaceRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KlixRecentProjectsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repo = WorkspaceRepositoryImpl(context)
        CoroutineScope(Dispatchers.IO).launch {
            val projects = repo.getRecentProjects()
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, projects)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        projects: List<ProjectItem>
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_recent_projects)

        // See All -> Open Recent Projects Dialog
        val seeAllIntent = Intent(context, MainActivity::class.java).apply {
            action = KlixWidgets.ACTION_SEE_ALL_PROJECTS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val seeAllPending = PendingIntent.getActivity(
            context,
            10,
            seeAllIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_recent_see_all, seeAllPending)

        // Row IDs mapped for easy population
        val rowIds = listOf(R.id.widget_project_row_1, R.id.widget_project_row_2, R.id.widget_project_row_3)
        val nameIds = listOf(R.id.widget_project_name_1, R.id.widget_project_name_2, R.id.widget_project_name_3)
        val pathIds = listOf(R.id.widget_project_path_1, R.id.widget_project_path_2, R.id.widget_project_path_3)
        val timeIds = listOf(R.id.widget_project_time_1, R.id.widget_project_time_2, R.id.widget_project_time_3)

        // If user has real projects, show them; otherwise show initial starter projects
        val displayProjects = if (projects.isNotEmpty()) {
            projects
        } else {
            listOf(
                ProjectItem(android.net.Uri.EMPTY, "orbit-ui", System.currentTimeMillis() - 7200000L),
                ProjectItem(android.net.Uri.EMPTY, "study-buddy", System.currentTimeMillis() - 86400000L),
                ProjectItem(android.net.Uri.EMPTY, "vision-ai", System.currentTimeMillis() - 259200000L)
            )
        }

        for (i in 0 until 3) {
            val rowId = rowIds[i]
            val nameId = nameIds[i]
            val pathId = pathIds[i]
            val timeId = timeIds[i]

            if (i < displayProjects.size) {
                val proj = displayProjects[i]
                views.setViewVisibility(rowId, View.VISIBLE)
                views.setTextViewText(nameId, proj.name)
                val displayPath = if (proj.uri != android.net.Uri.EMPTY) {
                    proj.uri.path?.substringAfterLast(':')?.let { "/$it" } ?: "/${proj.name}"
                } else {
                    "/Projects/${proj.name}"
                }
                views.setTextViewText(pathId, displayPath)

                val timeAgo = DateUtils.getRelativeTimeSpanString(
                    proj.lastOpened,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
                views.setTextViewText(timeId, timeAgo)

                // Row click intent -> Open this project in KlixCode
                val rowIntent = Intent(context, MainActivity::class.java).apply {
                    action = KlixWidgets.ACTION_OPEN_PROJECT
                    putExtra(KlixWidgets.EXTRA_PROJECT_URI, proj.uri.toString())
                    putExtra(KlixWidgets.EXTRA_PROJECT_NAME, proj.name)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val rowPending = PendingIntent.getActivity(
                    context,
                    100 + i,
                    rowIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(rowId, rowPending)
            } else {
                views.setViewVisibility(rowId, View.GONE)
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
