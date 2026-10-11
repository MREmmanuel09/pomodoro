package com.emman.pomodoro.data.repository

import com.emman.pomodoro.data.local.datastore.PomodoroDataStore
import com.emman.pomodoro.domain.model.AppSettings
import com.emman.pomodoro.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: PomodoroDataStore
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.settings

    override val onboardingDone: Flow<Boolean> = dataStore.onboardingDone

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.updateSettings(transform)
    }

    override suspend fun setOnboardingDone(done: Boolean) {
        dataStore.setOnboardingDone(done)
    }
}
