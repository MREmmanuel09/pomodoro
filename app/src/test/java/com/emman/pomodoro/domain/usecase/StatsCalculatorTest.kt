package com.emman.pomodoro.domain.usecase

import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.Session
import com.emman.pomodoro.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StatsCalculatorTest {

    private fun dayStart(daysAgo: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        return cal.timeInMillis
    }

    private fun session(
        completedAt: Long,
        type: PhaseType = PhaseType.WORK,
        status: SessionStatus = SessionStatus.COMPLETED,
        duration: Int = 25 * 60
    ) = Session(
        profileId = "p",
        phaseType = type,
        plannedDurationSec = 25 * 60,
        actualDurationSec = duration,
        startedAtEpochMs = completedAt - duration * 1000L,
        completedAtEpochMs = completedAt,
        status = status
    )

    @Test
    fun summarize_countsTodayCorrectly() {
        val now = System.currentTimeMillis()
        val sessions = listOf(
            session(now - 60_000),
            session(now - 120_000),
            session(now - 180_000)
        )
        val summary = StatsCalculator.summarize(sessions, dailyGoal = 4, nowEpochMs = now)
        assertEquals(3, summary.todayPomodoros)
        assertEquals(3 * 25 * 60, summary.todayFocusedSeconds)
        assertEquals(4, summary.dailyGoal)
        assertTrue(summary.goalProgress < 1f)
        assertFalse(summary.goalReached)
    }

    @Test
    fun summarize_goalReached() {
        val now = System.currentTimeMillis()
        val sessions = (1..4).map { session(now - it * 60_000L) }
        val summary = StatsCalculator.summarize(sessions, dailyGoal = 4, nowEpochMs = now)
        assertTrue(summary.goalReached)
        assertEquals(1f, summary.goalProgress, 0.001f)
    }

    @Test
    fun summarize_ignoresBreaksAndSkipped() {
        val now = System.currentTimeMillis()
        val sessions = listOf(
            session(now - 60_000, type = PhaseType.SHORT_BREAK),
            session(now - 120_000, status = SessionStatus.SKIPPED),
            session(now - 180_000)
        )
        val summary = StatsCalculator.summarize(sessions, dailyGoal = 8, nowEpochMs = now)
        assertEquals(1, summary.todayPomodoros)
    }

    @Test
    fun summarize_last30Days_hasEntryForEachDay() {
        val now = System.currentTimeMillis()
        val summary = StatsCalculator.summarize(emptyList(), dailyGoal = 8, nowEpochMs = now)
        assertEquals(30, summary.last30Days.size)
        val todayStart = StatsCalculator.startOfDay(now)
        assertEquals(todayStart, summary.last30Days.last().dayStartEpochMs)
    }

    @Test
    fun streak_consecutiveDays() {
        val now = System.currentTimeMillis()
        val sessions = listOf(
            session(dayStart(0) + 3600_000),
            session(dayStart(1) + 3600_000),
            session(dayStart(2) + 3600_000),
            session(dayStart(5) + 3600_000)
        )
        val summary = StatsCalculator.summarize(sessions, dailyGoal = 8, nowEpochMs = now)
        assertEquals(3, summary.currentStreakDays)
        assertEquals(3, summary.bestStreakDays)
    }

    @Test
    fun streak_brokenDay_resetsToYesterday() {
        val now = System.currentTimeMillis()
        val sessions = listOf(
            session(dayStart(1) + 3600_000),
            session(dayStart(2) + 3600_000)
        )
        val summary = StatsCalculator.summarize(sessions, dailyGoal = 8, nowEpochMs = now)
        assertEquals(2, summary.currentStreakDays)
    }

    @Test
    fun dayDetail_filtersByDay() {
        val now = System.currentTimeMillis()
        val today = StatsCalculator.startOfDay(now)
        val yesterday = today - 24L * 60 * 60 * 1000
        val sessions = listOf(
            session(today + 3600_000),
            session(yesterday + 3600_000)
        )
        val detail = StatsCalculator.dayDetail(sessions, today)
        assertEquals(1, detail.size)
        assertEquals(today + 3600_000, detail.first().completedAtEpochMs)
    }
}
