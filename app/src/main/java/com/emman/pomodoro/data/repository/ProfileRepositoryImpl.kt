package com.emman.pomodoro.data.repository

import com.emman.pomodoro.data.local.datastore.PomodoroDataStore
import com.emman.pomodoro.domain.model.TimerProfile
import com.emman.pomodoro.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val dataStore: PomodoroDataStore
) : ProfileRepository {

    override val profiles: Flow<List<TimerProfile>> = dataStore.profiles

    override suspend fun getProfile(id: String): TimerProfile? =
        dataStore.profiles.first().find { it.id == id }

    override suspend fun saveProfile(profile: TimerProfile) {
        val current = dataStore.profiles.first()
        val updated = if (current.any { it.id == profile.id }) {
            current.map { if (it.id == profile.id) profile else it }
        } else {
            current + profile
        }
        dataStore.saveProfiles(updated)
    }

    override suspend fun deleteProfile(id: String) {
        val current = dataStore.profiles.first()
        if (current.size <= 1) return
        dataStore.saveProfiles(current.filterNot { it.id == id })
    }
}
