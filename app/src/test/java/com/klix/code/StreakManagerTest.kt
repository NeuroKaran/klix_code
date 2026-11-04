package com.klix.code

import android.content.Context
import android.content.SharedPreferences
import com.klix.code.data.stats.StreakManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StreakManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private val storage = mutableMapOf<String, Any?>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Before
    fun setUp() {
        storage.clear()
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)

        `when`(mockPrefs.getStringSet(anyString(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val default = invocation.getArgument<Set<String>?>(1)
            @Suppress("UNCHECKED_CAST")
            (storage[key] as? Set<String>) ?: default
        }

        `when`(mockEditor.putStringSet(anyString(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            val set = invocation.getArgument<Set<String>?>(1)
            storage[key] = set
            mockEditor
        }
    }

    @Test
    fun testDayOfWeekIndexRange() {
        val index = StreakManager.getCurrentDayOfWeekIndex()
        assertTrue("Index should be between 0 (Mon) and 6 (Sun)", index in 0..6)
    }

    @Test
    fun testMillisUntilNextMidnight() {
        val millis = StreakManager.getMillisUntilNextMidnight()
        assertTrue("Millis should be positive", millis > 0)
        assertTrue("Millis should be less than or equal to 24 hours", millis <= 24 * 60 * 60 * 1000L)
    }

    @Test
    fun testFormatTimeRemaining() {
        val millis = (3 * 3600 + 25 * 60 + 42) * 1000L
        val formatted = StreakManager.formatTimeRemaining(millis)
        assertEquals("03h 25m 42s", formatted)
    }

    @Test
    fun testInitialStreakZeroWhenNoHistory() {
        val count = StreakManager.getStreakCount(mockContext)
        assertEquals(0, count)
        assertFalse(StreakManager.isClaimedToday(mockContext))
    }

    @Test
    fun testClaimDailyStreakIncrementsCount() {
        assertFalse(StreakManager.isClaimedToday(mockContext))
        val newCount = StreakManager.claimDailyStreak(mockContext)
        assertEquals(1, newCount)
        assertTrue(StreakManager.isClaimedToday(mockContext))
    }

    @Test
    fun testUnbrokenStreakFromYesterday() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dateFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val dayBefore = dateFormat.format(cal.time)

        // User logged in yesterday and day before
        storage["active_days_set"] = setOf(yesterday, dayBefore)

        // Before claiming today, streak count should be 2
        assertEquals(2, StreakManager.getStreakCount(mockContext))
        assertFalse(StreakManager.isClaimedToday(mockContext))

        // After claiming today, streak count becomes 3
        val claimed = StreakManager.claimDailyStreak(mockContext)
        assertEquals(3, claimed)
        assertTrue(StreakManager.isClaimedToday(mockContext))
    }

    @Test
    fun testBrokenStreakWhenYesterdayMissed() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        val threeDaysAgo = dateFormat.format(cal.time)

        // Last activity was 3 days ago
        storage["active_days_set"] = setOf(threeDaysAgo)

        // Streak is broken (yesterday was missed) -> count is 0 before claiming
        assertEquals(0, StreakManager.getStreakCount(mockContext))
        assertFalse(StreakManager.isClaimedToday(mockContext))

        // Claiming today starts a fresh streak of 1
        val claimed = StreakManager.claimDailyStreak(mockContext)
        assertEquals(1, claimed)
        assertTrue(StreakManager.isClaimedToday(mockContext))
    }

    @Test
    fun testWeeklyActiveDaysReflectsClaimedDays() {
        val todayStr = dateFormat.format(Date())
        storage["active_days_set"] = setOf(todayStr)

        val weekly = StreakManager.getWeeklyActiveDays(mockContext)
        val todayIdx = StreakManager.getCurrentDayOfWeekIndex()

        assertEquals(7, weekly.size)
        assertTrue(weekly[todayIdx])
    }
}

