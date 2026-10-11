package com.emman.pomodoro.data.repository

import com.emman.pomodoro.data.local.datastore.PomodoroDataStore
import com.emman.pomodoro.domain.model.PlantState
import com.emman.pomodoro.domain.repository.PlantRepository
import com.emman.pomodoro.domain.usecase.PlantRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlantRepositoryImpl @Inject constructor(
    private val dataStore: PomodoroDataStore
) : PlantRepository {

    override val plant: Flow<PlantState> = dataStore.plant

    override suspend fun onPomodoroCompleted() {
        val current = dataStore.plant.first()
        val now = System.currentTimeMillis()
        val newTotal = current.totalPomodoros + 1
        dataStore.savePlant(
            PlantState(
                stage = PlantRules.stageForPomodoros(newTotal),
                health = PlantRules.healOnPomodoro(current.clampedHealth),
                totalPomodoros = newTotal,
                lastProgressAtEpochMs = now,
                lastWitherAtEpochMs = current.lastWitherAtEpochMs,
                lastWateredAtEpochMs = current.lastWateredAtEpochMs
            )
        )
    }

    override suspend fun onDayMissed() {
        val current = dataStore.plant.first()
        val now = System.currentTimeMillis()
        dataStore.savePlant(
            current.copy(
                health = PlantRules.wither(current.clampedHealth),
                lastWitherAtEpochMs = PlantRules.startOfDay(now)
            )
        )
    }

    override suspend fun evaluateWither() {
        val current = dataStore.plant.first()
        val now = System.currentTimeMillis()
        if (PlantRules.shouldWither(current.lastProgressAtEpochMs, current.lastWitherAtEpochMs, now)) {
            onDayMissed()
        }
    }

    override suspend fun water() {
        val current = dataStore.plant.first()
        val now = System.currentTimeMillis()
        if (!PlantRules.canWater(current.lastWateredAtEpochMs, now)) return
        dataStore.savePlant(
            current.copy(
                health = PlantRules.healOnWater(current.clampedHealth),
                lastWateredAtEpochMs = now
            )
        )
    }
}
