package com.emman.pomodoro.data.repository

import com.emman.pomodoro.data.local.datastore.PomodoroDataStore
import com.emman.pomodoro.domain.model.TimerSnapshot
import com.emman.pomodoro.domain.repository.TimerSnapshotStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerSnapshotStoreImpl @Inject constructor(
    private val dataStore: PomodoroDataStore
) : TimerSnapshotStore {

    override suspend fun save(snapshot: TimerSnapshot) {
        dataStore.saveTimerSnapshot(snapshot)
    }

    override suspend fun load(): TimerSnapshot? = dataStore.loadTimerSnapshot()

    override suspend fun clear() {
        dataStore.clearTimerSnapshot()
    }
}
