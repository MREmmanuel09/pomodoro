package com.emman.pomodoro.domain.model

data class Session(
    val id: Long = 0,
    val profileId: String,
    val phaseType: PhaseType,
    val plannedDurationSec: Int,
    val actualDurationSec: Int,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val status: SessionStatus,
    val tagId: Long? = null
)
