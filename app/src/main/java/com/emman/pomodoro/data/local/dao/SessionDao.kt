package com.emman.pomodoro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.emman.pomodoro.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Insert
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Query("SELECT * FROM sessions ORDER BY completedAtEpochMs DESC")
    fun observeRecent(): Flow<List<SessionEntity>>

    @Query(
        "SELECT * FROM sessions " +
            "WHERE completedAtEpochMs >= :start AND completedAtEpochMs < :end " +
            "ORDER BY completedAtEpochMs DESC"
    )
    suspend fun getBetween(start: Long, end: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE completedAtEpochMs >= :start ORDER BY completedAtEpochMs DESC")
    fun observeSince(start: Long): Flow<List<SessionEntity>>

    @Query(
        "SELECT COUNT(*) FROM sessions " +
            "WHERE status = 'COMPLETED' AND phaseType = 'WORK' " +
            "AND completedAtEpochMs >= :start AND completedAtEpochMs < :end"
    )
    fun observeCompletedWorkCount(start: Long, end: Long): Flow<Int>

    @Query(
        "SELECT COALESCE(SUM(actualDurationSec), 0) FROM sessions " +
            "WHERE status = 'COMPLETED' AND phaseType = 'WORK' " +
            "AND completedAtEpochMs >= :start AND completedAtEpochMs < :end"
    )
    fun observeFocusedSeconds(start: Long, end: Long): Flow<Int>
}
