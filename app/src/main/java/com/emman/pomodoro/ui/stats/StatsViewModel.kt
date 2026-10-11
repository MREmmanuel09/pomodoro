package com.emman.pomodoro.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.Session
import com.emman.pomodoro.domain.model.SessionStatus
import com.emman.pomodoro.domain.model.StatsSummary
import com.emman.pomodoro.domain.model.Tag
import com.emman.pomodoro.domain.repository.SessionRepository
import com.emman.pomodoro.domain.repository.SettingsRepository
import com.emman.pomodoro.domain.repository.TagRepository
import com.emman.pomodoro.domain.usecase.StatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TagDistributionEntry(
    val tagId: Long?,
    val name: String,
    val pomodoros: Int,
    val fraction: Float
)

data class StatsUiState(
    val summary: StatsSummary = StatsSummary(),
    val recentSessions: List<Session> = emptyList(),
    val selectedDaySessions: List<Session>? = null,
    val selectedDayStart: Long? = null,
    val tagDistribution: List<TagDistributionEntry> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    settingsRepository: SettingsRepository,
    tagRepository: TagRepository
) : ViewModel() {

    private val thirtyDaysAgo = StatsCalculator.startOfDay(System.currentTimeMillis()) - 29L * DAY_MS

    private val selectedDay = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<StatsUiState> = combine(
        sessionRepository.observeSince(thirtyDaysAgo),
        settingsRepository.settings,
        selectedDay,
        tagRepository.tags
    ) { sessions, settings, day, tags ->
        val summary = StatsCalculator.summarize(
            sessions = sessions,
            dailyGoal = settings.dailyGoalPomodoros,
            nowEpochMs = System.currentTimeMillis()
        )
        val byTag = sessions
            .filter {
                it.completedAtEpochMs >= thirtyDaysAgo &&
                    it.phaseType == PhaseType.WORK &&
                    it.status == SessionStatus.COMPLETED
            }
            .groupBy { it.tagId }
            .map { (tagId, list) ->
                TagDistributionEntry(
                    tagId = tagId,
                    name = tags.find { it.id == tagId }?.name ?: "Sin etiqueta",
                    pomodoros = list.size,
                    fraction = 0f
                )
            }
            .sortedByDescending { it.pomodoros }
        val total = byTag.sumOf { it.pomodoros }.coerceAtLeast(1)
        StatsUiState(
            summary = summary,
            recentSessions = sessions.sortedByDescending { it.completedAtEpochMs }.take(20),
            selectedDayStart = day,
            selectedDaySessions = day?.let { StatsCalculator.dayDetail(sessions, it) },
            tagDistribution = byTag.map { it.copy(fraction = it.pomodoros.toFloat() / total) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun selectDay(dayStartEpochMs: Long?) {
        selectedDay.value = dayStartEpochMs
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
