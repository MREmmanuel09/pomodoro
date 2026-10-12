package com.emman.pomodoro.data.mapper

import com.emman.pomodoro.data.local.entity.SessionEntity
import com.emman.pomodoro.data.local.entity.TagEntity
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.Session
import com.emman.pomodoro.domain.model.SessionStatus
import com.emman.pomodoro.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    @Test
    fun sessionEntity_toDomain() {
        val entity = SessionEntity(
            id = 1L,
            profileId = "classic",
            phaseType = "WORK",
            plannedDurationSec = 1500,
            actualDurationSec = 1200,
            startedAtEpochMs = 1000L,
            completedAtEpochMs = 5000L,
            status = "COMPLETED",
            tagId = 3L
        )
        val session = entity.toDomain()
        assertEquals(1L, session.id)
        assertEquals("classic", session.profileId)
        assertEquals(PhaseType.WORK, session.phaseType)
        assertEquals(1500, session.plannedDurationSec)
        assertEquals(1200, session.actualDurationSec)
        assertEquals(1000L, session.startedAtEpochMs)
        assertEquals(5000L, session.completedAtEpochMs)
        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(3L, session.tagId)
    }

    @Test
    fun session_roundTrip() {
        val session = Session(
            id = 2L,
            profileId = "custom",
            phaseType = PhaseType.LONG_BREAK,
            plannedDurationSec = 600,
            actualDurationSec = 600,
            startedAtEpochMs = 100L,
            completedAtEpochMs = 700L,
            status = SessionStatus.SKIPPED,
            tagId = null
        )
        assertEquals(session, session.toEntity().toDomain())
    }

    @Test
    fun tagEntity_toDomain() {
        val entity = TagEntity(id = 5L, name = "Escritura", colorArgb = 0xFF0277BD)
        val tag = entity.toDomain()
        assertEquals(5L, tag.id)
        assertEquals("Escritura", tag.name)
        assertEquals(0xFF0277BD, tag.colorArgb)
    }

    @Test
    fun tag_roundTrip() {
        val tag = Tag(id = 7L, name = "Estudio", colorArgb = 0xFF2E7D32)
        assertEquals(tag, tag.toEntity().toDomain())
    }
}
