package com.emman.pomodoro.core.timing

import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.SessionStatus
import com.emman.pomodoro.domain.model.TimerProfile
import com.emman.pomodoro.domain.model.TimerSnapshot
import com.emman.pomodoro.domain.model.TimerStatus
import com.emman.pomodoro.domain.repository.TimerSnapshotStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    private class ManualTicker : Ticker {
        var now: Long = 0L
        override fun nowMs(): Long = now
        fun advance(ms: Long) {
            now += ms
        }
    }

    private class FakeSnapshotStore : TimerSnapshotStore {
        var saved: TimerSnapshot? = null
        override suspend fun save(snapshot: TimerSnapshot) {
            saved = snapshot
        }

        override suspend fun load(): TimerSnapshot? = saved

        override suspend fun clear() {
            saved = null
        }
    }

    private fun shortProfile() = TimerProfile(
        id = "test",
        name = "Test",
        phases = listOf(
            Phase(PhaseType.WORK, 60),
            Phase(PhaseType.SHORT_BREAK, 30),
            Phase(PhaseType.WORK, 60),
            Phase(PhaseType.LONG_BREAK, 30)
        )
    )

    @Test
    fun loadProfile_setsIdleWithFirstPhase() = runTest {
        val engine = TimerEngine(ManualTicker(), backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        val s = engine.state.value
        assertEquals(TimerStatus.IDLE, s.status)
        assertEquals(0, s.phaseIndex)
        assertEquals(60, s.remainingSeconds)
    }

    @Test
    fun start_beginsRunning() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        engine.start()
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
        assertEquals(60_000L, engine.state.value.phaseEndsAtEpochMs)
    }

    @Test
    fun pause_freezesRemainingTime() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(20_000)
        advanceTimeBy(300)
        engine.pause()
        val s = engine.state.value
        assertEquals(TimerStatus.PAUSED, s.status)
        assertEquals(40, s.remainingSeconds)
        assertNull(s.phaseEndsAtEpochMs)
    }

    @Test
    fun resume_continuesFromPausedRemaining() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(20_000)
        advanceTimeBy(300)
        engine.pause()
        engine.resume()
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
        assertEquals(40, engine.state.value.remainingSeconds)
    }

    @Test
    fun phaseCompletion_advancesToNextPhase() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(61_000)
        advanceTimeBy(300)
        val s = engine.state.value
        assertEquals(1, s.phaseIndex)
        assertEquals(PhaseType.SHORT_BREAK, s.currentPhaseType)
        assertEquals(30, s.remainingSeconds)
        assertEquals(1, s.pomodorosCompletedInRound)
    }

    @Test
    fun phaseCompletion_emitsEvent() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val events = mutableListOf<TimerEvent>()
        backgroundScope.launch { engine.events.toList(events) }
        runCurrent()

        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(61_000)
        advanceTimeBy(300)

        assertTrue(events.any { it is TimerEvent.PhaseCompleted })
        val completed = events.filterIsInstance<TimerEvent.PhaseCompleted>().first()
        assertEquals(SessionStatus.COMPLETED, completed.status)
        assertEquals(PhaseType.WORK, completed.phase.type)
        assertEquals(60, completed.actualDurationSec)
    }

    @Test
    fun skipPhase_marksSkippedAndAdvances() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(10_000)
        advanceTimeBy(300)
        engine.skipPhase()
        val s = engine.state.value
        assertEquals(1, s.phaseIndex)
        assertEquals(PhaseType.SHORT_BREAK, s.currentPhaseType)
    }

    @Test
    fun skipPhase_reportsActualElapsedDuration() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val events = mutableListOf<TimerEvent>()
        backgroundScope.launch { engine.events.toList(events) }
        runCurrent()

        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(10_000)
        advanceTimeBy(300)
        engine.skipPhase()
        runCurrent()

        val completed = events.filterIsInstance<TimerEvent.PhaseCompleted>().first()
        assertEquals(SessionStatus.SKIPPED, completed.status)
        assertEquals(10, completed.actualDurationSec)
    }

    @Test
    fun skipPhase_whilePaused_reportsActualElapsed() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val events = mutableListOf<TimerEvent>()
        backgroundScope.launch { engine.events.toList(events) }
        runCurrent()

        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(15_000)
        advanceTimeBy(300)
        engine.pause()
        engine.skipPhase()
        runCurrent()

        val completed = events.filterIsInstance<TimerEvent.PhaseCompleted>().first()
        assertEquals(SessionStatus.SKIPPED, completed.status)
        assertEquals(15, completed.actualDurationSec)
    }

    @Test
    fun adjust_addsTimeWhenRunning() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(10_000)
        engine.adjust(60)
        assertEquals(110, engine.state.value.remainingSeconds)
    }

    @Test
    fun adjust_clampsToZero_andCompletesPhase() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(10_000)
        engine.adjust(-1000)
        val s = engine.state.value
        assertEquals(1, s.phaseIndex)
        assertEquals(PhaseType.SHORT_BREAK, s.currentPhaseType)
    }

    @Test
    fun adjust_plusThenComplete_reportsExtendedDuration() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val events = mutableListOf<TimerEvent>()
        backgroundScope.launch { engine.events.toList(events) }
        runCurrent()

        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        engine.adjust(30)
        ticker.advance(91_000)
        advanceTimeBy(300)

        val completed = events.filterIsInstance<TimerEvent.PhaseCompleted>().first()
        assertEquals(90, completed.actualDurationSec)
    }

    @Test
    fun adjust_minusThenComplete_reportsShortenedDuration() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val events = mutableListOf<TimerEvent>()
        backgroundScope.launch { engine.events.toList(events) }
        runCurrent()

        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        engine.adjust(-20)
        ticker.advance(41_000)
        advanceTimeBy(300)

        val completed = events.filterIsInstance<TimerEvent.PhaseCompleted>().first()
        assertEquals(40, completed.actualDurationSec)
    }

    @Test
    fun adjust_whilePaused_updatesRemaining() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(10_000)
        advanceTimeBy(300)
        engine.pause()
        engine.adjust(5)
        assertEquals(TimerStatus.PAUSED, engine.state.value.status)
        assertEquals(55, engine.state.value.remainingSeconds)
    }

    @Test
    fun reset_returnsToIdleFirstPhase() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(30_000)
        advanceTimeBy(300)
        engine.reset()
        val s = engine.state.value
        assertEquals(TimerStatus.IDLE, s.status)
        assertEquals(0, s.phaseIndex)
        assertEquals(60, s.remainingSeconds)
        assertEquals(0, s.pomodorosCompletedInRound)
    }

    @Test
    fun lastPhaseCompletion_firesRoundCompleted() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val profile = TimerProfile(
            id = "one",
            name = "One",
            phases = listOf(Phase(PhaseType.WORK, 60))
        )
        val events = mutableListOf<TimerEvent>()
        backgroundScope.launch { engine.events.toList(events) }
        runCurrent()

        engine.loadProfile(profile)
        engine.start()
        runCurrent()
        ticker.advance(61_000)
        advanceTimeBy(300)

        assertEquals(TimerStatus.FINISHED, engine.state.value.status)
        assertTrue(events.any { it is TimerEvent.RoundCompleted })
    }

    @Test
    fun start_afterFinished_restartsRoundFromFirstPhase() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val profile = TimerProfile(
            id = "one",
            name = "One",
            phases = listOf(Phase(PhaseType.WORK, 60))
        )
        engine.loadProfile(profile)
        engine.start()
        runCurrent()
        ticker.advance(61_000)
        advanceTimeBy(300)
        assertEquals(TimerStatus.FINISHED, engine.state.value.status)

        engine.start()
        val s = engine.state.value
        assertEquals(TimerStatus.RUNNING, s.status)
        assertEquals(0, s.phaseIndex)
        assertEquals(60, s.remainingSeconds)
        assertEquals(0, s.pomodorosCompletedInRound)
    }

    @Test
    fun noAutoAdvance_pausesAtNextPhase() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val profile = TimerProfile(
            id = "manual",
            name = "Manual",
            phases = listOf(
                Phase(PhaseType.WORK, 60, autoAdvance = false),
                Phase(PhaseType.SHORT_BREAK, 30)
            )
        )
        engine.loadProfile(profile)
        engine.start()
        runCurrent()
        ticker.advance(61_000)
        advanceTimeBy(300)
        val s = engine.state.value
        assertEquals(TimerStatus.PAUSED, s.status)
        assertEquals(1, s.phaseIndex)
        assertEquals(30, s.remainingSeconds)
    }

    @Test
    fun loadProfile_ignoredWhileRunning() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()

        engine.loadProfile(TimerProfile(id = "other", name = "Other", phases = listOf(Phase(PhaseType.WORK, 10))))
        assertEquals("test", engine.state.value.profile.id)
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
    }

    @Test
    fun setTag_propagatesToEvents() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val events = mutableListOf<TimerEvent>()
        backgroundScope.launch { engine.events.toList(events) }
        runCurrent()

        engine.loadProfile(shortProfile())
        engine.setTag(42L)
        engine.start()
        runCurrent()
        ticker.advance(61_000)
        advanceTimeBy(300)

        val completed = events.filterIsInstance<TimerEvent.PhaseCompleted>().first()
        assertEquals(42L, completed.tagId)
    }

    @Test
    fun start_persistsRunningSnapshot() = runTest {
        val ticker = ManualTicker()
        val store = FakeSnapshotStore()
        val engine = TimerEngine(ticker, backgroundScope, store)
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()

        val saved = store.saved
        assertEquals("test", saved?.profileId)
        assertEquals(TimerStatus.RUNNING.name, saved?.status)
        assertEquals(60_000L, saved?.phaseEndsAtEpochMs)
    }

    @Test
    fun pause_persistsPausedSnapshot() = runTest {
        val ticker = ManualTicker()
        val store = FakeSnapshotStore()
        val engine = TimerEngine(ticker, backgroundScope, store)
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        ticker.advance(20_000)
        advanceTimeBy(300)
        engine.pause()
        runCurrent()

        val saved = store.saved
        assertEquals(TimerStatus.PAUSED.name, saved?.status)
        assertEquals(40, saved?.remainingSeconds)
        assertNull(saved?.phaseEndsAtEpochMs)
    }

    @Test
    fun reset_clearsSnapshot() = runTest {
        val ticker = ManualTicker()
        val store = FakeSnapshotStore()
        val engine = TimerEngine(ticker, backgroundScope, store)
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()
        engine.reset()
        runCurrent()

        assertNull(store.saved)
    }

    @Test
    fun roundCompletion_clearsSnapshot() = runTest {
        val ticker = ManualTicker()
        val store = FakeSnapshotStore()
        val engine = TimerEngine(ticker, backgroundScope, store)
        val profile = TimerProfile(
            id = "one",
            name = "One",
            phases = listOf(Phase(PhaseType.WORK, 60))
        )
        engine.loadProfile(profile)
        engine.start()
        runCurrent()
        ticker.advance(61_000)
        advanceTimeBy(300)
        runCurrent()

        assertNull(store.saved)
    }

    @Test
    fun restore_runningSnapshot_resumesWithRemainingTime() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val snapshot = TimerSnapshot(
            profileId = "test",
            phaseIndex = 0,
            status = TimerStatus.RUNNING.name,
            remainingSeconds = 40,
            phaseEndsAtEpochMs = 40_000L,
            totalSecondsInPhase = 60,
            adjustTotalSeconds = 0,
            pomodorosCompletedInRound = 0,
            activeTagId = null,
            sessionStartedAtEpochMs = 0L
        )
        engine.restore(snapshot, shortProfile())

        val s = engine.state.value
        assertEquals(TimerStatus.RUNNING, s.status)
        assertEquals(40, s.remainingSeconds)
        assertEquals(0, s.phaseIndex)
    }

    @Test
    fun restore_overdueSnapshot_completesPhaseAndAdvances() = runTest {
        val ticker = ManualTicker()
        ticker.advance(100_000)
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val events = mutableListOf<TimerEvent>()
        backgroundScope.launch { engine.events.toList(events) }
        runCurrent()

        val snapshot = TimerSnapshot(
            profileId = "test",
            phaseIndex = 0,
            status = TimerStatus.RUNNING.name,
            remainingSeconds = 5,
            phaseEndsAtEpochMs = 90_000L,
            totalSecondsInPhase = 60,
            adjustTotalSeconds = 0,
            pomodorosCompletedInRound = 0,
            activeTagId = null,
            sessionStartedAtEpochMs = 0L
        )
        engine.restore(snapshot, shortProfile())
        advanceTimeBy(300)

        assertTrue(events.any { it is TimerEvent.PhaseCompleted })
        val s = engine.state.value
        assertEquals(1, s.phaseIndex)
        assertEquals(PhaseType.SHORT_BREAK, s.currentPhaseType)
    }

    @Test
    fun restore_pausedSnapshot_setsPausedState() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        val snapshot = TimerSnapshot(
            profileId = "test",
            phaseIndex = 1,
            status = TimerStatus.PAUSED.name,
            remainingSeconds = 25,
            phaseEndsAtEpochMs = null,
            totalSecondsInPhase = 30,
            adjustTotalSeconds = 0,
            pomodorosCompletedInRound = 1,
            activeTagId = 7L,
            sessionStartedAtEpochMs = 0L
        )
        engine.restore(snapshot, shortProfile())

        val s = engine.state.value
        assertEquals(TimerStatus.PAUSED, s.status)
        assertEquals(1, s.phaseIndex)
        assertEquals(25, s.remainingSeconds)
        assertEquals(1, s.pomodorosCompletedInRound)
        assertEquals(7L, s.activeTagId)
    }

    @Test
    fun restore_ignoredWhenNotIdle() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope, FakeSnapshotStore())
        engine.loadProfile(shortProfile())
        engine.start()
        runCurrent()

        val snapshot = TimerSnapshot(
            profileId = "other",
            phaseIndex = 2,
            status = TimerStatus.RUNNING.name,
            remainingSeconds = 10,
            phaseEndsAtEpochMs = 10_000L,
            totalSecondsInPhase = 60,
            adjustTotalSeconds = 0,
            pomodorosCompletedInRound = 0,
            activeTagId = null,
            sessionStartedAtEpochMs = null
        )
        engine.restore(snapshot, TimerProfile(id = "other", name = "Other", phases = listOf(Phase(PhaseType.WORK, 10))))

        assertEquals("test", engine.state.value.profile.id)
    }

    @Test
    fun restore_afterClockReset_discardsSnapshot() = runTest {
        val ticker = ManualTicker()
        val store = FakeSnapshotStore()
        val engine = TimerEngine(ticker, backgroundScope, store)
        val snapshot = TimerSnapshot(
            profileId = "test",
            phaseIndex = 0,
            status = TimerStatus.RUNNING.name,
            remainingSeconds = 40,
            phaseEndsAtEpochMs = 40_000L,
            totalSecondsInPhase = 60,
            adjustTotalSeconds = 0,
            pomodorosCompletedInRound = 0,
            activeTagId = null,
            sessionStartedAtEpochMs = 0L,
            savedAtNowMs = 1_000_000L,
            savedAtWallClockMs = System.currentTimeMillis()
        )
        engine.restore(snapshot, shortProfile())
        runCurrent()

        assertEquals(TimerStatus.IDLE, engine.state.value.status)
        assertNull(store.saved)
    }
}
