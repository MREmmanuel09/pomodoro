package com.emman.pomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerStateTest {

    @Test
    fun progress_isZeroWhenIdle() {
        val state = TimerState()
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun progress_halfwayThroughPhase() {
        val state = TimerState(
            status = TimerStatus.RUNNING,
            remainingSeconds = 30,
            totalSecondsInPhase = 60
        )
        assertEquals(0.5f, state.progress, 0.001f)
    }

    @Test
    fun currentPhase_outOfBoundsIsNull() {
        val state = TimerState(phaseIndex = 99)
        assertEquals(null, state.currentPhase)
        assertEquals(null, state.currentPhaseType)
    }

    @Test
    fun totalElapsed_accumulatesPreviousPhases() {
        val profile = TimerProfile.classic()
        val state = TimerState(
            profile = profile,
            phaseIndex = 2,
            totalSecondsInPhase = 25 * 60,
            remainingSeconds = 20 * 60
        )
        val expected = (25 + 5) * 60 + 5 * 60
        assertEquals(expected, state.totalElapsedSecondsInRound)
    }
}
