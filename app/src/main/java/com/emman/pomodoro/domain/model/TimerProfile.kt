package com.emman.pomodoro.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TimerProfile(
    val id: String,
    val name: String,
    val phases: List<Phase>
) {
    init {
        require(phases.isNotEmpty()) { "Profile must have at least one phase" }
    }

    val totalRoundSeconds: Int get() = phases.sumOf { it.durationSeconds }
    val workPhaseCount: Int get() = phases.count { it.type == PhaseType.WORK }

    companion object {
        const val CLASSIC_ID = "profile_classic"
        const val FIFTY_TEN_ID = "profile_50_10"
        const val NINETY_TWENTY_ID = "profile_90_20"

        fun classic(): TimerProfile = TimerProfile(
            id = CLASSIC_ID,
            name = "Pomodoro clásico",
            phases = listOf(
                Phase(PhaseType.WORK, 25 * 60),
                Phase(PhaseType.SHORT_BREAK, 5 * 60),
                Phase(PhaseType.WORK, 25 * 60),
                Phase(PhaseType.SHORT_BREAK, 5 * 60),
                Phase(PhaseType.WORK, 25 * 60),
                Phase(PhaseType.SHORT_BREAK, 5 * 60),
                Phase(PhaseType.WORK, 25 * 60),
                Phase(PhaseType.LONG_BREAK, 15 * 60)
            )
        )

        fun fiftyTen(): TimerProfile = TimerProfile(
            id = FIFTY_TEN_ID,
            name = "50/10",
            phases = listOf(
                Phase(PhaseType.WORK, 50 * 60),
                Phase(PhaseType.SHORT_BREAK, 10 * 60),
                Phase(PhaseType.WORK, 50 * 60),
                Phase(PhaseType.SHORT_BREAK, 10 * 60),
                Phase(PhaseType.WORK, 50 * 60),
                Phase(PhaseType.LONG_BREAK, 20 * 60)
            )
        )

        fun ninetyTwenty(): TimerProfile = TimerProfile(
            id = NINETY_TWENTY_ID,
            name = "90/20 ultradian",
            phases = listOf(
                Phase(PhaseType.WORK, 90 * 60),
                Phase(PhaseType.LONG_BREAK, 20 * 60)
            )
        )

        fun defaultProfiles(): List<TimerProfile> = listOf(
            classic(),
            fiftyTen(),
            ninetyTwenty()
        )
    }
}
