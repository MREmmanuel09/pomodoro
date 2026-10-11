package com.emman.pomodoro.domain.repository

import com.emman.pomodoro.domain.model.PlantState
import kotlinx.coroutines.flow.Flow

interface PlantRepository {
    val plant: Flow<PlantState>
    suspend fun onPomodoroCompleted()
    suspend fun onDayMissed()
    suspend fun evaluateWither()
    suspend fun water()
}
