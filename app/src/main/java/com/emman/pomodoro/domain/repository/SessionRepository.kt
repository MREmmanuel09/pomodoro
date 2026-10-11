package com.emman.pomodoro.domain.repository

import com.emman.pomodoro.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val recentSessions: Flow<List<Session>>
    fun observeSince(startEpochMs: Long): Flow<List<Session>>
    suspend fun logSession(session: Session): Long
    suspend fun importSessions(sessions: List<Session>)
    suspend fun getSessionsBetween(startEpochMs: Long, endEpochMs: Long): List<Session>
    fun observeCompletedWorkCountBetween(startEpochMs: Long, endEpochMs: Long): Flow<Int>
    fun observeFocusedSecondsBetween(startEpochMs: Long, endEpochMs: Long): Flow<Int>
}
