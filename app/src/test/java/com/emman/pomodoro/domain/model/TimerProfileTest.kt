package com.emman.pomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerProfileTest {

    @Test
    fun classicProfile_hasExpectedShape() {
        val profile = TimerProfile.classic()
        assertEquals(8, profile.phases.size)
        assertEquals(4, profile.workPhaseCount)
        assertEquals(PhaseType.WORK, profile.phases.first().type)
        assertEquals(PhaseType.LONG_BREAK, profile.phases.last().type)
        assertEquals(25 * 60, profile.phases.first().durationSeconds)
        assertEquals(15 * 60, profile.phases.last().durationSeconds)
    }

    @Test
    fun classicProfile_totalRoundSeconds() {
        val profile = TimerProfile.classic()
        val expected = (4 * 25 + 3 * 5 + 15) * 60
        assertEquals(expected, profile.totalRoundSeconds)
    }

    @Test
    fun defaultProfiles_containsThreePresets() {
        val profiles = TimerProfile.defaultProfiles()
        assertEquals(3, profiles.size)
        assertTrue(profiles.any { it.id == TimerProfile.CLASSIC_ID })
        assertTrue(profiles.any { it.id == TimerProfile.FIFTY_TEN_ID })
        assertTrue(profiles.any { it.id == TimerProfile.NINETY_TWENTY_ID })
    }

    @Test(expected = IllegalArgumentException::class)
    fun phase_rejectsNonPositiveDuration() {
        Phase(PhaseType.WORK, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun profile_rejectsEmptyPhases() {
        TimerProfile(id = "x", name = "x", phases = emptyList())
    }
}
