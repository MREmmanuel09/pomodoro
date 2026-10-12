package com.emman.pomodoro.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emman.pomodoro.core.notification.TimerServiceController
import com.emman.pomodoro.core.timing.TimerEngine
import com.emman.pomodoro.domain.model.Tag
import com.emman.pomodoro.domain.model.ClockStyle
import com.emman.pomodoro.domain.model.TimerProfile
import com.emman.pomodoro.domain.model.TimerState
import com.emman.pomodoro.domain.model.TimerStatus
import com.emman.pomodoro.domain.model.TimerTextScale
import com.emman.pomodoro.domain.repository.ProfileRepository
import com.emman.pomodoro.domain.repository.SessionRepository
import com.emman.pomodoro.domain.repository.SettingsRepository
import com.emman.pomodoro.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TimerUiState(
    val timerState: TimerState = TimerState(),
    val profiles: List<TimerProfile> = emptyList(),
    val activeProfile: TimerProfile = TimerProfile.classic(),
    val completedToday: Int = 0,
    val dailyGoal: Int = 8,
    val tagsEnabled: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val strictModeEnabled: Boolean = false,
    val countUpMode: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.RING,
    val timerTextScale: TimerTextScale = TimerTextScale.M
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val engine: TimerEngine,
    private val settingsRepository: SettingsRepository,
    profileRepository: ProfileRepository,
    tagRepository: TagRepository,
    private val sessionRepository: SessionRepository,
    private val timerServiceController: TimerServiceController
) : ViewModel() {

    private val completedToday = MutableStateFlow(0)

    val uiState: StateFlow<TimerUiState> = combine(
        engine.state,
        profileRepository.profiles,
        settingsRepository.settings,
        completedToday,
        tagRepository.tags
    ) { state, profiles, settings, doneToday, tags ->
        val activeProfile = profiles.find { it.id == settings.activeProfileId }
            ?: profiles.firstOrNull()
            ?: TimerProfile.classic()

        val displayState = if (state.status == TimerStatus.IDLE && state.profile.id != activeProfile.id) {
            state.copy(profile = activeProfile)
        } else {
            state
        }

        TimerUiState(
            timerState = displayState,
            profiles = profiles,
            activeProfile = activeProfile,
            completedToday = doneToday,
            dailyGoal = settings.dailyGoalPomodoros,
            tagsEnabled = settings.tagsEnabled,
            tags = tags,
            strictModeEnabled = settings.strictModeEnabled,
            countUpMode = settings.countUpMode,
            clockStyle = settings.clockStyle,
            timerTextScale = settings.timerTextScale
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState())

    init {
        viewModelScope.launch {
            val todayStart = startOfTodayEpochMs()
            sessionRepository.observeCompletedWorkCountBetween(
                todayStart,
                todayStart + DAY_MS
            ).collect { count ->
                completedToday.value = count
            }
        }
    }

    fun togglePlayPause() {
        when (uiState.value.timerState.status) {
            TimerStatus.IDLE, TimerStatus.FINISHED -> {
                engine.start()
                timerServiceController.start()
            }
            TimerStatus.RUNNING -> engine.pause()
            TimerStatus.PAUSED -> {
                engine.resume()
                timerServiceController.start()
            }
        }
    }

    fun reset() {
        engine.reset()
    }

    fun skipPhase() = engine.skipPhase()
    fun adjust(deltaSeconds: Int) = engine.adjust(deltaSeconds)

    fun selectProfile(profileId: String) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(activeProfileId = profileId) }
        }
    }

    fun loadActiveProfile(profile: TimerProfile) {
        engine.loadProfile(profile)
    }

    fun setTag(tagId: Long?) = engine.setTag(tagId)

    private fun startOfTodayEpochMs(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
