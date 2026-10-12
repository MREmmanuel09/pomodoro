package com.emman.pomodoro.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TimerSnapshot(
    val profileId: String,
    val phaseIndex: Int,
    val status: String,
    val remainingSeconds: Int,
    val phaseEndsAtEpochMs: Long?,
    val totalSecondsInPhase: Int,
    val adjustTotalSeconds: Int,
    val pomodorosCompletedInRound: Int,
    val activeTagId: Long?,
    val sessionStartedAtEpochMs: Long?,
    val savedAtNowMs: Long = 0,
    val savedAtWallClockMs: Long = 0
)
