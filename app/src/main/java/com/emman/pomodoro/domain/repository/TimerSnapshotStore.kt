package com.emman.pomodoro.domain.repository

import com.emman.pomodoro.domain.model.TimerSnapshot

interface TimerSnapshotStore {
    suspend fun save(snapshot: TimerSnapshot)
    suspend fun load(): TimerSnapshot?
    suspend fun clear()
}
