package com.emman.pomodoro.core.timing

import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.SessionStatus

sealed interface TimerEvent {
    data class PhaseCompleted(
        val phase: Phase,
        val status: SessionStatus,
        val actualDurationSec: Int,
        val profileId: String,
        val tagId: Long?
    ) : TimerEvent

    data class RoundCompleted(
        val profileId: String,
        val pomodorosInRound: Int
    ) : TimerEvent
}
