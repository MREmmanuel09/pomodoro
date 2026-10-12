package com.emman.pomodoro.data.export

import com.emman.pomodoro.domain.model.AppSettings
import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.Session
import com.emman.pomodoro.domain.model.SessionStatus
import com.emman.pomodoro.domain.model.Tag
import com.emman.pomodoro.domain.model.TimerProfile
import com.emman.pomodoro.domain.repository.ProfileRepository
import com.emman.pomodoro.domain.repository.SessionRepository
import com.emman.pomodoro.domain.repository.SettingsRepository
import com.emman.pomodoro.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DataExporterTest {

    private class FakeSettingsRepository : SettingsRepository {
        val state = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = state
        override val onboardingDone: Flow<Boolean> = flowOf(false)
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            state.value = transform(state.value)
        }

        override suspend fun setOnboardingDone(done: Boolean) = Unit
    }

    private class FakeProfileRepository : ProfileRepository {
        val state = MutableStateFlow<List<TimerProfile>>(emptyList())
        override val profiles: Flow<List<TimerProfile>> = state
        override suspend fun getProfile(id: String): TimerProfile? =
            state.value.find { it.id == id }

        override suspend fun saveProfile(profile: TimerProfile) {
            state.value = state.value.filterNot { it.id == profile.id } + profile
        }

        override suspend fun deleteProfile(id: String) {
            state.value = state.value.filterNot { it.id == id }
        }
    }

    private class FakeTagRepository : TagRepository {
        val state = MutableStateFlow<List<Tag>>(emptyList())
        var imported: List<Tag> = emptyList()
        override val tags: Flow<List<Tag>> = state
        override suspend fun createTag(name: String, colorArgb: Long): Long = 0
        override suspend fun updateTag(tag: Tag) = Unit
        override suspend fun deleteTag(id: Long) = Unit
        override suspend fun importTags(tags: List<Tag>) {
            imported = tags
            state.value = tags
        }
    }

    private class FakeSessionRepository : SessionRepository {
        val state = MutableStateFlow<List<Session>>(emptyList())
        var imported: List<Session> = emptyList()
        override val recentSessions: Flow<List<Session>> = state
        override fun observeSince(startEpochMs: Long): Flow<List<Session>> = state
        override suspend fun logSession(session: Session): Long = 0
        override suspend fun importSessions(sessions: List<Session>) {
            imported = sessions
        }

        override suspend fun getSessionsBetween(startEpochMs: Long, endEpochMs: Long): List<Session> = emptyList()
        override fun observeCompletedWorkCountBetween(startEpochMs: Long, endEpochMs: Long): Flow<Int> = flowOf(0)
        override fun observeFocusedSecondsBetween(startEpochMs: Long, endEpochMs: Long): Flow<Int> = flowOf(0)
    }

    private fun profile() = TimerProfile(
        id = "custom",
        name = "Custom",
        phases = listOf(Phase(PhaseType.WORK, 3000), Phase(PhaseType.SHORT_BREAK, 600))
    )

    private fun session(id: Long) = Session(
        id = id,
        profileId = "custom",
        phaseType = PhaseType.WORK,
        plannedDurationSec = 3000,
        actualDurationSec = 3000,
        startedAtEpochMs = 1000L,
        completedAtEpochMs = 4000L,
        status = SessionStatus.COMPLETED,
        tagId = 9L
    )

    @Test
    fun exportJson_containsAllData() = runTest {
        val settings = FakeSettingsRepository().apply { state.value = AppSettings(dailyGoalPomodoros = 12, tagsEnabled = true) }
        val profiles = FakeProfileRepository().apply { state.value = listOf(profile()) }
        val tags = FakeTagRepository().apply { state.value = listOf(Tag(9L, "Proyecto", 0xFF0277BD)) }
        val sessions = FakeSessionRepository().apply { state.value = listOf(session(1L)) }

        val exporter = DataExporter(profiles, tags, sessions, settings)
        val raw = exporter.exportJson()

        val decoded = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString(ExportEnvelope.serializer(), raw)
        assertEquals(12, decoded.settings?.dailyGoalPomodoros)
        assertEquals(1, decoded.profiles.size)
        assertEquals("Proyecto", decoded.tags.first().name)
        assertEquals(1, decoded.sessions.size)
        assertEquals("COMPLETED", decoded.sessions.first().status)
    }

    @Test
    fun importJson_appliesSettingsTagsAndSessions() = runTest {
        val settings = FakeSettingsRepository()
        val profiles = FakeProfileRepository()
        val tags = FakeTagRepository()
        val sessions = FakeSessionRepository()

        val raw = """
            {
              "version": 1,
              "exportedAtEpochMs": 5,
              "settings": {"dailyGoalPomodoros": 6, "themeMode": "DARK"},
              "profiles": [{"id":"custom","name":"Custom","phases":[{"type":"WORK","durationSeconds":300,"autoAdvance":true}]}],
              "tags": [{"id":9,"name":"Proyecto","colorArgb":123}],
              "sessions": [
                {"profileId":"custom","phaseType":"WORK","plannedDurationSec":300,"actualDurationSec":300,
                 "startedAtEpochMs":1,"completedAtEpochMs":2,"status":"COMPLETED","tagId":9},
                {"profileId":"custom","phaseType":"WORK","plannedDurationSec":300,"actualDurationSec":10,
                 "startedAtEpochMs":1,"completedAtEpochMs":2,"status":"INVALID_STATUS","tagId":null}
              ]
            }
        """.trimIndent()

        val exporter = DataExporter(profiles, tags, sessions, settings)
        exporter.importJson(raw)

        assertEquals(6, settings.state.value.dailyGoalPomodoros)
        assertEquals(1, profiles.state.value.size)
        assertEquals(listOf(Tag(9L, "Proyecto", 123L)), tags.imported)
        assertEquals(1, sessions.imported.size)
        assertEquals(SessionStatus.COMPLETED, sessions.imported.first().status)
    }
}
