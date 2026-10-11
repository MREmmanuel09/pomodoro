package com.emman.pomodoro.data.repository

import com.emman.pomodoro.data.local.dao.SessionDao
import com.emman.pomodoro.data.mapper.toDomain
import com.emman.pomodoro.data.mapper.toEntity
import com.emman.pomodoro.domain.model.Session
import com.emman.pomodoro.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {

    override val recentSessions: Flow<List<Session>> =
        sessionDao.observeRecent().map { entities -> entities.map { it.toDomain() } }

    override fun observeSince(startEpochMs: Long): Flow<List<Session>> =
        sessionDao.observeSince(startEpochMs).map { entities -> entities.map { it.toDomain() } }

    override suspend fun logSession(session: Session): Long =
        sessionDao.insert(session.toEntity())

    override suspend fun importSessions(sessions: List<Session>) {
        sessionDao.insertAll(sessions.map { it.toEntity().copy(id = 0) })
    }

    override suspend fun getSessionsBetween(startEpochMs: Long, endEpochMs: Long): List<Session> =
        sessionDao.getBetween(startEpochMs, endEpochMs).map { it.toDomain() }

    override fun observeCompletedWorkCountBetween(startEpochMs: Long, endEpochMs: Long): Flow<Int> =
        sessionDao.observeCompletedWorkCount(startEpochMs, endEpochMs)

    override fun observeFocusedSecondsBetween(startEpochMs: Long, endEpochMs: Long): Flow<Int> =
        sessionDao.observeFocusedSeconds(startEpochMs, endEpochMs)
}
