package com.emman.pomodoro.domain.usecase

import com.emman.pomodoro.domain.model.PlantStage
import java.util.Calendar

object PlantRules {

    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val HEALTH_PER_POMODORO = 5
    private const val HEALTH_PER_WATER = 10
    private const val HEALTH_PER_MISSED_DAY = 15

    fun stageForPomodoros(total: Int): PlantStage = when {
        total >= 100 -> PlantStage.TREE
        total >= 40 -> PlantStage.FLOWER
        total >= 15 -> PlantStage.YOUNG
        total >= 5 -> PlantStage.SPROUT
        else -> PlantStage.SEED
    }

    fun pomodorosToNextStage(total: Int): Int? {
        val next = when (stageForPomodoros(total)) {
            PlantStage.SEED -> 5 - total
            PlantStage.SPROUT -> 15 - total
            PlantStage.YOUNG -> 40 - total
            PlantStage.FLOWER -> 100 - total
            PlantStage.TREE -> return null
        }
        return next.coerceAtLeast(0)
    }

    fun healOnPomodoro(currentHealth: Int): Int =
        (currentHealth + HEALTH_PER_POMODORO).coerceAtMost(100)

    fun healOnWater(currentHealth: Int): Int =
        (currentHealth + HEALTH_PER_WATER).coerceAtMost(100)

    fun daysSince(lastEpochMs: Long, nowEpochMs: Long): Long {
        if (lastEpochMs <= 0L) return 0
        return (nowEpochMs - lastEpochMs) / DAY_MS
    }

    fun shouldWither(lastProgressAtEpochMs: Long, lastWitherAtEpochMs: Long, nowEpochMs: Long): Boolean {
        if (lastProgressAtEpochMs <= 0L) return false
        if (daysSince(lastProgressAtEpochMs, nowEpochMs) < 1) return false
        if (lastWitherAtEpochMs <= 0L) return true
        return daysSince(lastWitherAtEpochMs, nowEpochMs) >= 1
    }

    fun wither(currentHealth: Int): Int =
        (currentHealth - HEALTH_PER_MISSED_DAY).coerceAtLeast(0)

    fun canWater(lastWateredAtEpochMs: Long, nowEpochMs: Long): Boolean =
        lastWateredAtEpochMs <= 0L || daysSince(lastWateredAtEpochMs, nowEpochMs) >= 1

    fun startOfDay(epochMs: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
