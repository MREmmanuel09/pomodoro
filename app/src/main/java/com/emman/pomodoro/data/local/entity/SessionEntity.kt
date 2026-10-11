package com.emman.pomodoro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: String,
    val phaseType: String,
    val plannedDurationSec: Int,
    val actualDurationSec: Int,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val status: String,
    val tagId: Long?
)
