package com.emman.pomodoro.domain.usecase

import com.emman.pomodoro.domain.model.PlantStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantRulesTest {

    @Test
    fun stageForPomodoros_thresholds() {
        assertEquals(PlantStage.SEED, PlantRules.stageForPomodoros(0))
        assertEquals(PlantStage.SEED, PlantRules.stageForPomodoros(4))
        assertEquals(PlantStage.SPROUT, PlantRules.stageForPomodoros(5))
        assertEquals(PlantStage.YOUNG, PlantRules.stageForPomodoros(15))
        assertEquals(PlantStage.FLOWER, PlantRules.stageForPomodoros(40))
        assertEquals(PlantStage.TREE, PlantRules.stageForPomodoros(100))
        assertEquals(PlantStage.TREE, PlantRules.stageForPomodoros(500))
    }

    @Test
    fun pomodorosToNextStage() {
        assertEquals(5, PlantRules.pomodorosToNextStage(0))
        assertEquals(10, PlantRules.pomodorosToNextStage(5))
        assertEquals(25, PlantRules.pomodorosToNextStage(15))
        assertEquals(60, PlantRules.pomodorosToNextStage(40))
        assertNull(PlantRules.pomodorosToNextStage(100))
    }

    @Test
    fun heal_capsAt100() {
        assertEquals(98, PlantRules.healOnPomodoro(93))
        assertEquals(100, PlantRules.healOnPomodoro(98))
        assertEquals(15, PlantRules.healOnWater(5))
        assertEquals(100, PlantRules.healOnWater(95))
    }

    @Test
    fun wither_floorAtZero() {
        assertEquals(5, PlantRules.wither(20))
        assertEquals(0, PlantRules.wither(10))
    }

    private val DAY_MS = 24L * 60 * 60 * 1000
    private val now = 1_700_000_000_000L

    @Test
    fun shouldWither_neverProgressed() {
        assertFalse(PlantRules.shouldWither(0L, 0L, now))
    }

    @Test
    fun shouldWither_progressedToday() {
        assertFalse(PlantRules.shouldWither(now - 3600_000, 0L, now))
    }

    @Test
    fun shouldWither_missedDay() {
        assertTrue(PlantRules.shouldWither(now - DAY_MS * 2, 0L, now))
    }

    @Test
    fun shouldWither_onlyOncePerDay() {
        val lastWither = now - 3600_000
        assertFalse(PlantRules.shouldWither(now - DAY_MS * 3, lastWither, now))
    }

    @Test
    fun canWater_oncePerDay() {
        assertTrue(PlantRules.canWater(0L, now))
        assertTrue(PlantRules.canWater(now - DAY_MS, now))
        assertFalse(PlantRules.canWater(now - 3600_000, now))
    }

    @Test
    fun daysSince() {
        assertEquals(0L, PlantRules.daysSince(0L, now))
        assertEquals(2L, PlantRules.daysSince(now - DAY_MS * 2 - 3600_000, now))
    }
}
