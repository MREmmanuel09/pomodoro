package com.emman.pomodoro.domain.repository

import com.emman.pomodoro.domain.model.TimerProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val profiles: Flow<List<TimerProfile>>
    suspend fun getProfile(id: String): TimerProfile?
    suspend fun saveProfile(profile: TimerProfile)
    suspend fun deleteProfile(id: String)
}
