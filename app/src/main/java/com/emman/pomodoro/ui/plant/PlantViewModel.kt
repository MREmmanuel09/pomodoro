package com.emman.pomodoro.ui.plant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emman.pomodoro.domain.model.PlantState
import com.emman.pomodoro.domain.repository.PlantRepository
import com.emman.pomodoro.domain.repository.SessionRepository
import com.emman.pomodoro.domain.usecase.PlantRules
import com.emman.pomodoro.domain.usecase.StatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlantUiState(
    val plant: PlantState = PlantState(),
    val canWater: Boolean = false,
    val pomodorosToNextStage: Int? = null,
    val currentStreakDays: Int = 0
)

@HiltViewModel
class PlantViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    sessionRepository: SessionRepository
) : ViewModel() {

    private val yearAgo = StatsCalculator.startOfDay(System.currentTimeMillis()) - 400L * DAY_MS

    val uiState: StateFlow<PlantUiState> = combine(
        plantRepository.plant,
        sessionRepository.observeSince(yearAgo)
    ) { plant, sessions ->
        PlantUiState(
            plant = plant,
            canWater = PlantRules.canWater(plant.lastWateredAtEpochMs, System.currentTimeMillis()),
            pomodorosToNextStage = PlantRules.pomodorosToNextStage(plant.totalPomodoros),
            currentStreakDays = StatsCalculator.summarize(
                sessions = sessions,
                dailyGoal = 1,
                nowEpochMs = System.currentTimeMillis()
            ).currentStreakDays
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlantUiState())

    init {
        viewModelScope.launch { plantRepository.evaluateWither() }
    }

    fun water() {
        viewModelScope.launch { plantRepository.water() }
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
