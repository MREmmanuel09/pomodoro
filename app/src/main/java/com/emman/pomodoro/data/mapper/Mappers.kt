package com.emman.pomodoro.data.mapper

import com.emman.pomodoro.data.local.entity.SessionEntity
import com.emman.pomodoro.data.local.entity.TagEntity
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.Session
import com.emman.pomodoro.domain.model.SessionStatus
import com.emman.pomodoro.domain.model.Tag

fun SessionEntity.toDomain(): Session = Session(
    id = id,
    profileId = profileId,
    phaseType = PhaseType.valueOf(phaseType),
    plannedDurationSec = plannedDurationSec,
    actualDurationSec = actualDurationSec,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    status = SessionStatus.valueOf(status),
    tagId = tagId
)

fun Session.toEntity(): SessionEntity = SessionEntity(
    id = id,
    profileId = profileId,
    phaseType = phaseType.name,
    plannedDurationSec = plannedDurationSec,
    actualDurationSec = actualDurationSec,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    status = status.name,
    tagId = tagId
)

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    colorArgb = colorArgb
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    colorArgb = colorArgb
)
