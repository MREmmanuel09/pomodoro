package com.emman.pomodoro.data.export

import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.Session
import com.emman.pomodoro.domain.model.SessionStatus
import com.emman.pomodoro.domain.model.Tag
import com.emman.pomodoro.domain.repository.ProfileRepository
import com.emman.pomodoro.domain.repository.SessionRepository
import com.emman.pomodoro.domain.repository.SettingsRepository
import com.emman.pomodoro.domain.repository.TagRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataExporter @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val tagRepository: TagRepository,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportJson(): String {
        val envelope = ExportEnvelope(
            version = 1,
            exportedAtEpochMs = System.currentTimeMillis(),
            settings = settingsRepository.settings.first(),
            profiles = profileRepository.profiles.first(),
            tags = tagRepository.tags.first().map {
                ExportTag(id = it.id, name = it.name, colorArgb = it.colorArgb)
            },
            sessions = sessionRepository.observeSince(0).first().map { session ->
                ExportSession(
                    profileId = session.profileId,
                    phaseType = session.phaseType.name,
                    plannedDurationSec = session.plannedDurationSec,
                    actualDurationSec = session.actualDurationSec,
                    startedAtEpochMs = session.startedAtEpochMs,
                    completedAtEpochMs = session.completedAtEpochMs,
                    status = session.status.name,
                    tagId = session.tagId
                )
            }
        )
        return json.encodeToString(ExportEnvelope.serializer(), envelope)
    }

    suspend fun importJson(raw: String) {
        val envelope = json.decodeFromString(ExportEnvelope.serializer(), raw)
        envelope.settings?.let { imported ->
            settingsRepository.update { imported }
        }
        envelope.profiles.forEach { profileRepository.saveProfile(it) }
        tagRepository.importTags(
            envelope.tags.map { Tag(id = it.id, name = it.name, colorArgb = it.colorArgb) }
        )
        sessionRepository.importSessions(
            envelope.sessions.mapNotNull { s ->
                runCatching {
                    Session(
                        profileId = s.profileId,
                        phaseType = PhaseType.valueOf(s.phaseType),
                        plannedDurationSec = s.plannedDurationSec,
                        actualDurationSec = s.actualDurationSec,
                        startedAtEpochMs = s.startedAtEpochMs,
                        completedAtEpochMs = s.completedAtEpochMs,
                        status = SessionStatus.valueOf(s.status),
                        tagId = s.tagId
                    )
                }.getOrNull()
            }
        )
    }
}
