package com.emman.pomodoro.core.timing

import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.SessionStatus
import com.emman.pomodoro.domain.model.TimerProfile
import com.emman.pomodoro.domain.model.TimerSnapshot
import com.emman.pomodoro.domain.model.TimerState
import com.emman.pomodoro.domain.model.TimerStatus
import com.emman.pomodoro.domain.repository.TimerSnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerEngine @Inject constructor(
    private val ticker: Ticker,
    @param:ApplicationScope private val scope: CoroutineScope,
    private val snapshotStore: TimerSnapshotStore
) {
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TimerEvent>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()

    private var tickJob: Job? = null

    private var adjustTotalSeconds: Int = 0
    private var phaseStartedAtMs: Long = 0

    fun loadProfile(profile: TimerProfile) {
        if (_state.value.status == TimerStatus.RUNNING) return
        resetInternal()
        _state.update {
            it.copy(
                profile = profile,
                phaseIndex = 0,
                status = TimerStatus.IDLE,
                remainingSeconds = profile.phases.first().durationSeconds,
                totalSecondsInPhase = profile.phases.first().durationSeconds,
                pomodorosCompletedInRound = 0
            )
        }
    }

    fun setTag(tagId: Long?) {
        _state.update { it.copy(activeTagId = tagId) }
        persistSnapshot()
    }

    fun start() {
        val s = _state.value
        when (s.status) {
            TimerStatus.IDLE -> beginPhase(s.phaseIndex, s.profile)
            TimerStatus.PAUSED -> resume()
            TimerStatus.FINISHED -> {
                _state.update { it.copy(pomodorosCompletedInRound = 0) }
                beginPhase(0, s.profile)
            }
            else -> Unit
        }
    }

    fun pause() {
        val s = _state.value
        if (s.status != TimerStatus.RUNNING) return
        val end = s.phaseEndsAtEpochMs ?: return
        val remainingMs = (end - ticker.nowMs()).coerceAtLeast(0)
        stopTickerLoop()
        _state.update {
            it.copy(
                status = TimerStatus.PAUSED,
                remainingSeconds = (remainingMs / 1000).toInt().coerceAtLeast(0),
                phaseEndsAtEpochMs = null
            )
        }
        persistSnapshot()
    }

    fun resume() {
        val s = _state.value
        if (s.status != TimerStatus.PAUSED) return
        beginPhaseFromRemaining(s.phaseIndex, s.profile, s.remainingSeconds)
    }

    fun skipPhase() {
        val s = _state.value
        if (s.status != TimerStatus.RUNNING && s.status != TimerStatus.PAUSED) return
        completePhase(status = SessionStatus.SKIPPED)
    }

    fun reset() {
        stopTickerLoop()
        val profile = _state.value.profile
        resetInternal()
        _state.update {
            it.copy(
                profile = profile,
                phaseIndex = 0,
                status = TimerStatus.IDLE,
                remainingSeconds = profile.phases.first().durationSeconds,
                totalSecondsInPhase = profile.phases.first().durationSeconds,
                pomodorosCompletedInRound = 0,
                phaseEndsAtEpochMs = null,
                sessionStartedAtEpochMs = null
            )
        }
        scope.launch { snapshotStore.clear() }
    }

    fun adjust(deltaSeconds: Int) {
        val s = _state.value
        if (s.status != TimerStatus.RUNNING && s.status != TimerStatus.PAUSED) return
        val currentRemaining = if (s.status == TimerStatus.RUNNING && s.phaseEndsAtEpochMs != null) {
            ((s.phaseEndsAtEpochMs - ticker.nowMs()) / 1000).toInt().coerceAtLeast(0)
        } else {
            s.remainingSeconds
        }
        val newRemaining = (currentRemaining + deltaSeconds).coerceAtLeast(0)
        if (s.status == TimerStatus.RUNNING) {
            val newEnd = ticker.nowMs() + newRemaining * 1000L
            adjustTotalSeconds += deltaSeconds
            _state.update {
                it.copy(remainingSeconds = newRemaining, phaseEndsAtEpochMs = newEnd)
            }
            if (newRemaining == 0) {
                completePhase(status = SessionStatus.COMPLETED)
            } else {
                persistSnapshot()
            }
        } else {
            _state.update { it.copy(remainingSeconds = newRemaining) }
            persistSnapshot()
        }
    }

    fun restore(snapshot: TimerSnapshot, profile: TimerProfile) {
        if (_state.value.status != TimerStatus.IDLE) return
        val status = runCatching { TimerStatus.valueOf(snapshot.status) }.getOrNull() ?: return
        if (status != TimerStatus.RUNNING && status != TimerStatus.PAUSED) return
        if (snapshot.savedAtNowMs > 0L && snapshot.savedAtWallClockMs > 0L) {
            val monoDelta = ticker.nowMs() - snapshot.savedAtNowMs
            val wallDelta = System.currentTimeMillis() - snapshot.savedAtWallClockMs
            if (kotlin.math.abs(wallDelta - monoDelta) > REBOOT_TOLERANCE_MS) {
                scope.launch { snapshotStore.clear() }
                return
            }
        }
        resetInternal()
        adjustTotalSeconds = snapshot.adjustTotalSeconds
        when (status) {
            TimerStatus.RUNNING -> {
                val end = snapshot.phaseEndsAtEpochMs ?: return
                _state.update {
                    it.copy(
                        profile = profile,
                        phaseIndex = snapshot.phaseIndex,
                        status = TimerStatus.RUNNING,
                        remainingSeconds = ((end - ticker.nowMs()) / 1000).toInt().coerceAtLeast(0),
                        totalSecondsInPhase = snapshot.totalSecondsInPhase,
                        phaseEndsAtEpochMs = end,
                        pomodorosCompletedInRound = snapshot.pomodorosCompletedInRound,
                        activeTagId = snapshot.activeTagId,
                        sessionStartedAtEpochMs = snapshot.sessionStartedAtEpochMs
                    )
                }
                startTickerLoop()
            }
            TimerStatus.PAUSED -> {
                _state.update {
                    it.copy(
                        profile = profile,
                        phaseIndex = snapshot.phaseIndex,
                        status = TimerStatus.PAUSED,
                        remainingSeconds = snapshot.remainingSeconds,
                        totalSecondsInPhase = snapshot.totalSecondsInPhase,
                        phaseEndsAtEpochMs = null,
                        pomodorosCompletedInRound = snapshot.pomodorosCompletedInRound,
                        activeTagId = snapshot.activeTagId,
                        sessionStartedAtEpochMs = snapshot.sessionStartedAtEpochMs
                    )
                }
                persistSnapshot()
            }
            else -> Unit
        }
    }

    private fun beginPhase(index: Int, profile: TimerProfile) {
        val phase = profile.phases.getOrNull(index) ?: run {
            _state.update { it.copy(status = TimerStatus.FINISHED) }
            return
        }
        adjustTotalSeconds = 0
        beginPhaseFromRemaining(index, profile, phase.durationSeconds)
    }

    private fun beginPhaseFromRemaining(index: Int, profile: TimerProfile, remainingSeconds: Int) {
        val phase = profile.phases.getOrNull(index) ?: return
        val now = ticker.nowMs()
        phaseStartedAtMs = now
        val endAt = now + remainingSeconds * 1000L
        _state.update {
            it.copy(
                status = TimerStatus.RUNNING,
                phaseIndex = index,
                remainingSeconds = remainingSeconds,
                totalSecondsInPhase = phase.durationSeconds,
                phaseEndsAtEpochMs = endAt,
                sessionStartedAtEpochMs = it.sessionStartedAtEpochMs ?: now
            )
        }
        persistSnapshot()
        startTickerLoop()
    }

    private fun startTickerLoop() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val s = _state.value
                if (s.status != TimerStatus.RUNNING) break
                val end = s.phaseEndsAtEpochMs ?: break
                val remainingMs = end - ticker.nowMs()
                if (remainingMs <= 0) {
                    completePhase(status = SessionStatus.COMPLETED)
                    break
                }
                val remainingSec = (remainingMs / 1000).toInt().coerceAtLeast(0)
                _state.update { it.copy(remainingSeconds = remainingSec) }
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private fun stopTickerLoop() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun completePhase(status: SessionStatus) {
        stopTickerLoop()
        val s = _state.value
        val phase = s.profile.phases.getOrNull(s.phaseIndex) ?: return
        val remainingAtEvent = if (status == SessionStatus.COMPLETED) 0 else s.remainingSeconds
        val actualSec = (phase.durationSeconds + adjustTotalSeconds - remainingAtEvent)
            .coerceAtLeast(0)
        val newPomodoros = if (phase.type == PhaseType.WORK) {
            s.pomodorosCompletedInRound + 1
        } else {
            s.pomodorosCompletedInRound
        }
        _events.tryEmit(
            TimerEvent.PhaseCompleted(
                phase = phase,
                status = status,
                actualDurationSec = actualSec,
                profileId = s.profile.id,
                tagId = s.activeTagId
            )
        )
        advanceAfterPhase(s, phase, newPomodoros)
    }

    private fun advanceAfterPhase(
        s: TimerState,
        completedPhase: Phase,
        newPomodoros: Int
    ) {
        val nextIndex = s.phaseIndex + 1
        val nextPhase = s.profile.phases.getOrNull(nextIndex)
        if (nextPhase == null) {
            _state.update {
                it.copy(
                    status = TimerStatus.FINISHED,
                    remainingSeconds = 0,
                    phaseEndsAtEpochMs = null,
                    pomodorosCompletedInRound = newPomodoros
                )
            }
            scope.launch { snapshotStore.clear() }
            _events.tryEmit(
                TimerEvent.RoundCompleted(
                    profileId = s.profile.id,
                    pomodorosInRound = newPomodoros
                )
            )
            return
        }
        _state.update {
            it.copy(
                pomodorosCompletedInRound = newPomodoros,
                phaseIndex = nextIndex,
                totalSecondsInPhase = nextPhase.durationSeconds,
                phaseEndsAtEpochMs = null
            )
        }
        if (completedPhase.autoAdvance) {
            adjustTotalSeconds = 0
            beginPhase(nextIndex, s.profile)
        } else {
            adjustTotalSeconds = 0
            _state.update {
                it.copy(
                    status = TimerStatus.PAUSED,
                    remainingSeconds = nextPhase.durationSeconds
                )
            }
            persistSnapshot()
        }
    }

    private fun persistSnapshot() {
        val s = _state.value
        if (s.status == TimerStatus.IDLE || s.status == TimerStatus.FINISHED) {
            scope.launch { snapshotStore.clear() }
            return
        }
        val snapshot = TimerSnapshot(
            profileId = s.profile.id,
            phaseIndex = s.phaseIndex,
            status = s.status.name,
            remainingSeconds = s.remainingSeconds,
            phaseEndsAtEpochMs = s.phaseEndsAtEpochMs,
            totalSecondsInPhase = s.totalSecondsInPhase,
            adjustTotalSeconds = adjustTotalSeconds,
            pomodorosCompletedInRound = s.pomodorosCompletedInRound,
            activeTagId = s.activeTagId,
            sessionStartedAtEpochMs = s.sessionStartedAtEpochMs,
            savedAtNowMs = ticker.nowMs(),
            savedAtWallClockMs = System.currentTimeMillis()
        )
        scope.launch { snapshotStore.save(snapshot) }
    }

    private fun resetInternal() {
        stopTickerLoop()
        adjustTotalSeconds = 0
        phaseStartedAtMs = 0
    }

    companion object {
        const val TICK_INTERVAL_MS = 250L
        private const val REBOOT_TOLERANCE_MS = 10L * 60 * 1000
    }
}
