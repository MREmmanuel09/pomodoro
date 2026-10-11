package com.emman.pomodoro.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Phase(
    val type: PhaseType,
    val durationSeconds: Int,
    val autoAdvance: Boolean = true
) {
    init {
        require(durationSeconds > 0) { "Phase duration must be positive" }
    }
}
