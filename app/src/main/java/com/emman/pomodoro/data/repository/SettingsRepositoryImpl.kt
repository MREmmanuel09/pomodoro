package com.emman.pomodoro.data.repository

import android.content.Context
import com.emman.pomodoro.core.timing.ApplicationScope
import com.emman.pomodoro.core.util.LanguageCache
import com.emman.pomodoro.data.local.datastore.PomodoroDataStore
import com.emman.pomodoro.domain.model.AppSettings
import com.emman.pomodoro.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: PomodoroDataStore,
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.settings

    override val onboardingDone: Flow<Boolean> = dataStore.onboardingDone

    init {
        scope.launch {
            dataStore.settings.collect { LanguageCache.write(context, it.language) }
        }
    }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.updateSettings(transform)
        LanguageCache.write(context, dataStore.settings.first().language)
    }

    override suspend fun setOnboardingDone(done: Boolean) {
        dataStore.setOnboardingDone(done)
    }
}
