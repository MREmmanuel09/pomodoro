package com.emman.pomodoro.core.timing

import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.SessionStatus
import com.emman.pomodoro.domain.model.TimerProfile
import com.emman.pomodoro.domain.model.TimerStatus
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
        val engine = TimerEngine(ManualTicker(), backgroundScope)
        engine.loadProfile(shortProfile())
        val s = engine.state.value
        assertEquals(TimerStatus.IDLE, s.status)
        assertEquals(0, s.phaseIndex)
        assertEquals(60, s.remainingSeconds)
    }

    @Test
    fun start_beginsRunning() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope)
        engine.loadProfile(shortProfile())
        engine.start()
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
        assertEquals(60_000L, engine.state.value.phaseEndsAtEpochMs)
    }

    @Test
    fun pause_freezesRemainingTime() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope)
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
        val engine = TimerEngine(ticker, backgroundScope)
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
        val engine = TimerEngine(ticker, backgroundScope)
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
        val engine = TimerEngine(ticker, backgroundScope)
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
    }

    @Test
    fun skipPhase_marksSkippedAndAdvances() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope)
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
    fun adjust_addsTimeWhenRunning() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope)
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
        val engine = TimerEngine(ticker, backgroundScope)
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
    fun reset_returnsToIdleFirstPhase() = runTest {
        val ticker = ManualTicker()
        val engine = TimerEngine(ticker, backgroundScope)
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
        val engine = TimerEngine(ticker, backgroundScope)
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
}
