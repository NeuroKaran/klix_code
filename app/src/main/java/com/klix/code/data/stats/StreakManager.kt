package com.klix.code.data.stats

import android.content.Context
import android.content.SharedPreferences
import com.klix.code.widget.KlixWidgets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StreakManager {
    private const val PREFS_NAME = "klix_streak_prefs"
    private const val KEY_ACTIVE_DAYS = "active_days_set"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Records coding activity for today and updates widgets.
     */
    fun recordActivity(context: Context) {
        val prefs = getPrefs(context)
        val todayStr = dateFormat.format(Date())
        val daysSet = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet())?.toMutableSet() ?: mutableSetOf()

        if (!daysSet.contains(todayStr)) {
            daysSet.add(todayStr)
            prefs.edit().putStringSet(KEY_ACTIVE_DAYS, daysSet).apply()
            KlixWidgets.updateAll(context)
        }
    }

    /**
     * Calculates the current continuous coding streak in days.
     */
    fun getStreakCount(context: Context): Int {
        val prefs = getPrefs(context)
        val daysSet = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet()) ?: return 1

        val cal = Calendar.getInstance()
        val todayStr = dateFormat.format(cal.time)

        var streak = 0
        // Check today or yesterday as start
        if (daysSet.contains(todayStr)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(cal.time)
            if (daysSet.contains(yesterdayStr)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                // Default minimum streak display for positive motivation
                return 1
            }
        }

        while (true) {
            val dateStr = dateFormat.format(cal.time)
            if (daysSet.contains(dateStr)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return maxOf(streak, 1)
    }

    /**
     * Returns a 7-element boolean array for Monday..Sunday of the current week.
     */
    fun getWeeklyActiveDays(context: Context): BooleanArray {
        val prefs = getPrefs(context)
        val daysSet = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet()) ?: emptySet()

        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val result = BooleanArray(7)
        for (i in 0..6) {
            val dateStr = dateFormat.format(cal.time)
            result[i] = daysSet.contains(dateStr)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }
}
