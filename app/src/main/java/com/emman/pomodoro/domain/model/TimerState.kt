package com.emman.pomodoro.domain.model

enum class TimerStatus { IDLE, RUNNING, PAUSED, FINISHED }

data class TimerState(
    val status: TimerStatus = TimerStatus.IDLE,
    val profile: TimerProfile = TimerProfile.classic(),
    val phaseIndex: Int = 0,
    val remainingSeconds: Int = profile.phases.first().durationSeconds,
    val totalSecondsInPhase: Int = profile.phases.first().durationSeconds,
    val pomodorosCompletedInRound: Int = 0,
    val phaseEndsAtEpochMs: Long? = null,
    val activeTagId: Long? = null,
    val sessionStartedAtEpochMs: Long? = null
) {
    val currentPhase: Phase?
        get() = profile.phases.getOrNull(phaseIndex)

    val currentPhaseType: PhaseType?
        get() = currentPhase?.type

    val isRunning: Boolean get() = status == TimerStatus.RUNNING

    val progress: Float
        get() = if (totalSecondsInPhase > 0) {
            1f - remainingSeconds.toFloat() / totalSecondsInPhase
        } else {
            0f
        }

    val totalElapsedSecondsInRound: Int
        get() = profile.phases.take(phaseIndex).sumOf { it.durationSeconds } +
            (totalSecondsInPhase - remainingSeconds)
}
