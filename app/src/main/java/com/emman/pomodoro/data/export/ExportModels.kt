package com.emman.pomodoro.data.export

import com.emman.pomodoro.domain.model.AppSettings
import com.emman.pomodoro.domain.model.TimerProfile
import kotlinx.serialization.Serializable

@Serializable
data class ExportEnvelope(
    val version: Int = 1,
    val exportedAtEpochMs: Long = 0,
    val settings: AppSettings? = null,
    val profiles: List<TimerProfile> = emptyList(),
    val tags: List<ExportTag> = emptyList(),
    val sessions: List<ExportSession> = emptyList()
)

@Serializable
data class ExportTag(
    val id: Long = 0,
    val name: String,
    val colorArgb: Long
)

@Serializable
data class ExportSession(
    val profileId: String,
    val phaseType: String,
    val plannedDurationSec: Int,
    val actualDurationSec: Int,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val status: String,
    val tagId: Long? = null
)
