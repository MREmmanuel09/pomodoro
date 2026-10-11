package com.emman.pomodoro.domain.model

data class DayStats(
    val dayStartEpochMs: Long,
    val completedPomodoros: Int,
    val focusedSeconds: Int
)

data class StatsSummary(
    val todayPomodoros: Int = 0,
    val todayFocusedSeconds: Int = 0,
    val dailyGoal: Int = 8,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val totalPomodoros: Int = 0,
    val totalFocusedSeconds: Long = 0,
    val last30Days: List<DayStats> = emptyList()
) {
    val goalProgress: Float
        get() = if (dailyGoal > 0) todayPomodoros.toFloat() / dailyGoal else 0f
    val goalReached: Boolean
        get() = todayPomodoros >= dailyGoal
}
