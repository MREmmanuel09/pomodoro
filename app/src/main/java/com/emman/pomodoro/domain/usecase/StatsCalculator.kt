package com.emman.pomodoro.domain.usecase

import com.emman.pomodoro.domain.model.DayStats
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.Session
import com.emman.pomodoro.domain.model.SessionStatus
import com.emman.pomodoro.domain.model.StatsSummary
import java.util.Calendar

object StatsCalculator {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun startOfDay(epochMs: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun summarize(sessions: List<Session>, dailyGoal: Int, nowEpochMs: Long): StatsSummary {
        val todayStart = startOfDay(nowEpochMs)
        val thirtyDaysAgo = todayStart - 29L * DAY_MS

        val relevant = sessions.filter {
            it.completedAtEpochMs >= thirtyDaysAgo && it.phaseType == PhaseType.WORK
        }

        val todaySessions = relevant.filter { it.completedAtEpochMs >= todayStart }
        val todayPomodoros = todaySessions.count { it.status == SessionStatus.COMPLETED }
        val todayFocused = todaySessions
            .filter { it.status == SessionStatus.COMPLETED }
            .sumOf { it.actualDurationSec }

        val byDay = relevant.groupBy { startOfDay(it.completedAtEpochMs) }
        val last30 = (0 until 30).map { offset ->
            val dayStart = todayStart - (29 - offset) * DAY_MS
            val daySessions = byDay[dayStart].orEmpty()
                .filter { it.status == SessionStatus.COMPLETED }
            DayStats(
                dayStartEpochMs = dayStart,
                completedPomodoros = daySessions.size,
                focusedSeconds = daySessions.sumOf { it.actualDurationSec }
            )
        }

        val allCompleted = sessions.filter {
            it.phaseType == PhaseType.WORK && it.status == SessionStatus.COMPLETED
        }

        return StatsSummary(
            todayPomodoros = todayPomodoros,
            todayFocusedSeconds = todayFocused,
            dailyGoal = dailyGoal,
            currentStreakDays = currentStreak(byDay.keys, todayStart),
            bestStreakDays = bestStreak(byDay.keys),
            totalPomodoros = allCompleted.size,
            totalFocusedSeconds = allCompleted.sumOf { it.actualDurationSec }.toLong(),
            last30Days = last30
        )
    }

    fun dayDetail(sessions: List<Session>, dayStartEpochMs: Long): List<Session> {
        val dayEnd = dayStartEpochMs + DAY_MS
        return sessions.filter {
            it.completedAtEpochMs >= dayStartEpochMs && it.completedAtEpochMs < dayEnd
        }.sortedByDescending { it.completedAtEpochMs }
    }

    private fun currentStreak(activeDays: Set<Long>, todayStart: Long): Int {
        var streak = 0
        var cursor = if (activeDays.contains(todayStart)) todayStart else addDays(todayStart, -1)
        while (activeDays.contains(cursor)) {
            streak++
            cursor = addDays(cursor, -1)
        }
        return streak
    }

    private fun bestStreak(activeDays: Set<Long>): Int {
        if (activeDays.isEmpty()) return 0
        val sorted = activeDays.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            current = if (addDays(sorted[i - 1], 1) == sorted[i]) {
                current + 1
            } else {
                1
            }
            if (current > best) best = current
        }
        return best
    }

    private fun addDays(epochMs: Long, days: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = epochMs
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
}
