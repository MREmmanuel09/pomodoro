package com.emman.pomodoro.domain.repository

import com.emman.pomodoro.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    val onboardingDone: Flow<Boolean>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    suspend fun setOnboardingDone(done: Boolean)
}
