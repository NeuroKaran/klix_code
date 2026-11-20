package com.klix.code.data.stats

import android.content.Context
import android.content.SharedPreferences
import com.klix.code.widget.KlixWidgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayActivity(
    val dayOfMonth: Int,
    val dateString: String,
    val runCount: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
    val intensityLevel: Int
)

data class StreakState(
    val streakCount: Int = 0,
    val isClaimedToday: Boolean = false,
    val weeklyActive: BooleanArray = BooleanArray(7),
    val todayIndex: Int = 0,
    val millisUntilMidnight: Long = 0L,
    val totalMonthRuns: Int = 0,
    val monthActivities: List<DayActivity> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StreakState
        if (streakCount != other.streakCount) return false
        if (isClaimedToday != other.isClaimedToday) return false
        if (!weeklyActive.contentEquals(other.weeklyActive)) return false
        if (todayIndex != other.todayIndex) return false
        if (millisUntilMidnight != other.millisUntilMidnight) return false
        if (totalMonthRuns != other.totalMonthRuns) return false
        if (monthActivities != other.monthActivities) return false
        return true
    }

    override fun hashCode(): Int {
        var result = streakCount
        result = 31 * result + isClaimedToday.hashCode()
        result = 31 * result + weeklyActive.contentHashCode()
        result = 31 * result + todayIndex
        result = 31 * result + millisUntilMidnight.hashCode()
        result = 31 * result + totalMonthRuns
        result = 31 * result + monthActivities.hashCode()
        return result
    }
}

object StreakManager {
    private const val PREFS_NAME = "klix_streak_prefs"
    private const val KEY_ACTIVE_DAYS = "active_days_set"
    private const val KEY_RUN_COUNTS_PREFIX = "run_count_"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _streakState = MutableStateFlow(StreakState())
    val streakState: StateFlow<StreakState> = _streakState.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks whether today's daily streak has already been claimed.
     */
    fun isClaimedToday(context: Context): Boolean {
        val prefs = getPrefs(context)
        val todayStr = dateFormat.format(Date())
        val daysSet = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet()) ?: emptySet()
        return daysSet.contains(todayStr)
    }

    /**
     * Records/claims the daily login streak for today, updates widgets, and notifies observers.
     * Returns the new streak count.
     */
    fun claimDailyStreak(context: Context): Int {
        val prefs = getPrefs(context)
        val todayStr = dateFormat.format(Date())
        val daysSet = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val runKey = KEY_RUN_COUNTS_PREFIX + todayStr
        val currentRuns = prefs.getInt(runKey, 0)
        val editor = prefs.edit()

        if (!daysSet.contains(todayStr)) {
            daysSet.add(todayStr)
            editor.putStringSet(KEY_ACTIVE_DAYS, daysSet)
        }
        if (currentRuns == 0) {
            editor.putInt(runKey, 1)
        }
        editor.apply()

        refresh(context)
        KlixWidgets.updateAll(context)
        return getStreakCount(context)
    }

    /**
     * Records a real run/execution in the sandbox, terminal, editor, or agent.
     * Increments the daily run counter, records the active day in the streak set,
     * refreshes streak and month activity state, and notifies widgets.
     */
    fun recordActivityRun(context: Context, count: Int = 1) {
        val prefs = getPrefs(context)
        val todayStr = dateFormat.format(Date())
        val daysSet = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet())?.toMutableSet() ?: mutableSetOf()
        daysSet.add(todayStr)

        val runKey = KEY_RUN_COUNTS_PREFIX + todayStr
        val currentRuns = prefs.getInt(runKey, 0)
        val newRuns = currentRuns + count

        prefs.edit()
            .putStringSet(KEY_ACTIVE_DAYS, daysSet)
            .putInt(runKey, newRuns)
            .apply()

        refresh(context)
        KlixWidgets.updateAll(context)
    }

    /**
     * Compatibility wrapper for activity recording.
     */
    fun recordActivity(context: Context) {
        recordActivityRun(context, 1)
    }

    /**
     * Returns the recorded run count for a specific date (yyyy-MM-dd).
     */
    fun getRunCountForDay(context: Context, dateStr: String): Int {
        val prefs = getPrefs(context)
        val explicitRuns = prefs.getInt(KEY_RUN_COUNTS_PREFIX + dateStr, 0)
        if (explicitRuns > 0) return explicitRuns
        val daysSet = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet()) ?: emptySet()
        return if (daysSet.contains(dateStr)) 1 else 0
    }

    /**
     * Returns real activity data for every day of the given calendar month.
     */
    fun getMonthActivities(context: Context, calendar: Calendar = Calendar.getInstance()): List<DayActivity> {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH)
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val activities = ArrayList<DayActivity>(daysInMonth)
        for (day in 1..daysInMonth) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
            val isCurrentMonthAndYear = year == todayYear && month == todayMonth
            val isToday = isCurrentMonthAndYear && day == todayDay
            val isFuture = if (isCurrentMonthAndYear) {
                day > todayDay
            } else {
                year > todayYear || (year == todayYear && month > todayMonth)
            }

            val runCount = if (isFuture) 0 else getRunCountForDay(context, dateStr)
            val intensity = when {
                isFuture || runCount == 0 -> 0
                runCount in 1..2 -> 1
                runCount in 3..5 -> 2
                else -> 3
            }

            activities.add(
                DayActivity(
                    dayOfMonth = day,
                    dateString = dateStr,
                    runCount = runCount,
                    isToday = isToday,
                    isFuture = isFuture,
                    intensityLevel = intensity
                )
            )
        }
        return activities
    }

    /**
     * Calculates the current continuous coding streak in days.
     * - If claimed today: count consecutive days ending today.
     * - If not claimed today: count consecutive days ending yesterday (unbroken streak awaiting claim).
     *   If yesterday was missed, streak is 0 until claimed.
     */
    fun getStreakCount(context: Context): Int {
        val prefs = getPrefs(context)
        val daysSet = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet()) ?: emptySet()
        if (daysSet.isEmpty()) return 0

        val cal = Calendar.getInstance()
        val todayStr = dateFormat.format(cal.time)

        var streak = 0
        if (daysSet.contains(todayStr)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            while (daysSet.contains(dateFormat.format(cal.time))) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(cal.time)
            if (daysSet.contains(yesterdayStr)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
                while (daysSet.contains(dateFormat.format(cal.time))) {
                    streak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
            } else {
                streak = 0
            }
        }

        return streak
    }

    /**
     * Returns the 0-based index for today in the Monday..Sunday week:
     * 0 = Monday, 1 = Tuesday, ..., 6 = Sunday.
     */
    fun getCurrentDayOfWeekIndex(): Int {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    }

    /**
     * Returns a 7-element boolean array for Monday..Sunday of the current week.
     */
    fun getWeeklyActiveDays(context: Context): BooleanArray {
        val prefs = getPrefs(context)
        val daysSet = prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet()) ?: emptySet()

        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        val result = BooleanArray(7)
        for (i in 0..6) {
            val dateStr = dateFormat.format(cal.time)
            result[i] = daysSet.contains(dateStr)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    /**
     * Returns milliseconds remaining until the upcoming 12:00:00 AM midnight.
     */
    fun getMillisUntilNextMidnight(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return maxOf(0L, midnight.timeInMillis - now.timeInMillis)
    }

    /**
     * Formats milliseconds into e.g. "01h 24m 35s".
     */
    fun formatTimeRemaining(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
    }

    /**
     * Re-reads shared preferences and updates the reactive [streakState] Flow.
     */
    fun refresh(context: Context) {
        val count = getStreakCount(context)
        val claimed = isClaimedToday(context)
        val weekly = getWeeklyActiveDays(context)
        val todayIdx = getCurrentDayOfWeekIndex()
        val millis = getMillisUntilNextMidnight()
        val monthActivities = getMonthActivities(context)
        val totalMonthRuns = monthActivities.sumOf { it.runCount }
        _streakState.value = StreakState(
            streakCount = count,
            isClaimedToday = claimed,
            weeklyActive = weekly,
            todayIndex = todayIdx,
            millisUntilMidnight = millis,
            totalMonthRuns = totalMonthRuns,
            monthActivities = monthActivities
        )
    }
}
