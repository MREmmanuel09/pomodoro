package com.emman.pomodoro.ui.timer

import app.cash.turbine.test
import com.emman.pomodoro.core.notification.TimerServiceController
import com.emman.pomodoro.core.timing.Ticker
import com.emman.pomodoro.core.timing.TimerEngine
import com.emman.pomodoro.domain.model.AppSettings
import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.Session
import com.emman.pomodoro.domain.model.Tag
import com.emman.pomodoro.domain.model.TimerProfile
import com.emman.pomodoro.domain.model.TimerSnapshot
import com.emman.pomodoro.domain.model.TimerStatus
import com.emman.pomodoro.domain.repository.ProfileRepository
import com.emman.pomodoro.domain.repository.SessionRepository
import com.emman.pomodoro.domain.repository.SettingsRepository
import com.emman.pomodoro.domain.repository.TagRepository
import com.emman.pomodoro.domain.repository.TimerSnapshotStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private class ManualTicker : Ticker {
        var now: Long = 0L
        override fun nowMs(): Long = now
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

    private class FakeSettingsRepository : SettingsRepository {
        val state = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = state
        override val onboardingDone: Flow<Boolean> = flowOf(true)
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            state.value = transform(state.value)
        }

        override suspend fun setOnboardingDone(done: Boolean) = Unit
    }

    private class FakeProfileRepository : ProfileRepository {
        val state = MutableStateFlow<List<TimerProfile>>(emptyList())
        override val profiles: Flow<List<TimerProfile>> = state
        override suspend fun getProfile(id: String): TimerProfile? = state.value.find { it.id == id }
        override suspend fun saveProfile(profile: TimerProfile) = Unit
        override suspend fun deleteProfile(id: String) = Unit
    }

    private class FakeTagRepository : TagRepository {
        val state = MutableStateFlow<List<Tag>>(emptyList())
        override val tags: Flow<List<Tag>> = state
        override suspend fun createTag(name: String, colorArgb: Long): Long = 0
        override suspend fun updateTag(tag: Tag) = Unit
        override suspend fun deleteTag(id: Long) = Unit
        override suspend fun importTags(tags: List<Tag>) = Unit
    }

    private class FakeSessionRepository(count: Int = 0) : SessionRepository {
        val count = MutableStateFlow(count)
        override val recentSessions: Flow<List<Session>> = flowOf(emptyList())
        override fun observeSince(startEpochMs: Long): Flow<List<Session>> = flowOf(emptyList())
        override suspend fun logSession(session: Session): Long = 0
        override suspend fun importSessions(sessions: List<Session>) = Unit
        override suspend fun getSessionsBetween(startEpochMs: Long, endEpochMs: Long): List<Session> = emptyList()
        override fun observeCompletedWorkCountBetween(startEpochMs: Long, endEpochMs: Long): Flow<Int> = count
        override fun observeFocusedSecondsBetween(startEpochMs: Long, endEpochMs: Long): Flow<Int> = flowOf(0)
    }

    private class FakeTimerServiceController : TimerServiceController {
        var starts = 0
        override fun start() {
            starts++
        }
    }

    private val customProfile = TimerProfile(
        id = "custom",
        name = "Custom",
        phases = listOf(Phase(PhaseType.WORK, 3000), Phase(PhaseType.SHORT_BREAK, 600))
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        scope: kotlinx.coroutines.CoroutineScope,
        ticker: ManualTicker = ManualTicker(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        profiles: FakeProfileRepository = FakeProfileRepository().apply {
            state.value = listOf(customProfile, TimerProfile.classic())
        },
        tags: FakeTagRepository = FakeTagRepository(),
        sessions: FakeSessionRepository = FakeSessionRepository(),
        serviceController: FakeTimerServiceController = FakeTimerServiceController()
    ): Triple<TimerViewModel, TimerEngine, FakeTimerServiceController> {
        val engine = TimerEngine(ticker, scope, FakeSnapshotStore())
        val vm = TimerViewModel(engine, settings, profiles, tags, sessions, serviceController)
        return Triple(vm, engine, serviceController)
    }

    @Test
    fun initialState_exposesActiveProfileDuration() = runTest {
        val (vm, _, _) = createViewModel(backgroundScope)
        vm.uiState.test {
            val state = awaitItem()
            assertEquals(TimerStatus.IDLE, state.timerState.status)
            assertEquals(TimerProfile.classic().phases.first().durationSeconds, state.timerState.remainingSeconds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadActiveProfile_updatesEngineDuration() = runTest {
        val (vm, engine, _) = createViewModel(backgroundScope)
        vm.loadActiveProfile(customProfile)
        assertEquals("custom", engine.state.value.profile.id)
        assertEquals(3000, engine.state.value.remainingSeconds)
    }

    @Test
    fun completedToday_reflectsSessionCount() = runTest {
        val sessions = FakeSessionRepository(count = 3)
        val (vm, _, _) = createViewModel(backgroundScope, sessions = sessions)
        vm.uiState.test {
            var state = awaitItem()
            if (state.completedToday != 3) state = awaitItem()
            assertEquals(3, state.completedToday)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectProfile_updatesSettings() = runTest {
        val settings = FakeSettingsRepository()
        val (vm, _, _) = createViewModel(backgroundScope, settings = settings)
        vm.selectProfile("custom")
        assertEquals("custom", settings.state.value.activeProfileId)
    }

    @Test
    fun togglePlayPause_startsEngineAndService() = runTest {
        val (vm, engine, service) = createViewModel(backgroundScope)
        vm.togglePlayPause()
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
        assertEquals(1, service.starts)
    }

    @Test
    fun togglePlayPause_whenRunning_pausesWithoutService() = runTest {
        val (vm, engine, service) = createViewModel(backgroundScope)
        vm.uiState.test {
            awaitItem()
            vm.togglePlayPause()
            awaitItem()
            vm.togglePlayPause()
            awaitItem()
            assertEquals(TimerStatus.PAUSED, engine.state.value.status)
            assertEquals(1, service.starts)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
