package com.emman.pomodoro.data.export

import com.emman.pomodoro.domain.model.AppSettings
import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.PlantStage
import com.emman.pomodoro.domain.model.PlantState
import com.emman.pomodoro.domain.model.ThemeMode
import com.emman.pomodoro.domain.model.TimerProfile
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportModelsTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun envelope_roundTrip() {
        val envelope = ExportEnvelope(
            version = 1,
            exportedAtEpochMs = 123456789L,
            settings = AppSettings(
                dailyGoalPomodoros = 10,
                themeMode = ThemeMode.DARK,
                plantEnabled = false,
                tagsEnabled = true
            ),
            profiles = listOf(
                TimerProfile(
                    id = "custom",
                    name = "Custom",
                    phases = listOf(
                        Phase(PhaseType.WORK, 40 * 60),
                        Phase(PhaseType.LONG_BREAK, 10 * 60)
                    )
                )
            ),
            tags = listOf(ExportTag(id = 3, name = "Escritura", colorArgb = 0xFF0277BD)),
            sessions = listOf(
                ExportSession(
                    profileId = "custom",
                    phaseType = "WORK",
                    plannedDurationSec = 2400,
                    actualDurationSec = 2400,
                    startedAtEpochMs = 1000L,
                    completedAtEpochMs = 3400L,
                    status = "COMPLETED",
                    tagId = 3L
                )
            )
        )

        val encoded = json.encodeToString(ExportEnvelope.serializer(), envelope)
        val decoded = json.decodeFromString(ExportEnvelope.serializer(), encoded)

        assertEquals(envelope, decoded)
    }

    @Test
    fun envelope_ignoresUnknownKeys() {
        val raw = """
            {"version":1,"unknownField":true,"profiles":[]}
        """.trimIndent()
        val decoded = json.decodeFromString(ExportEnvelope.serializer(), raw)
        assertEquals(1, decoded.version)
        assertEquals(0, decoded.profiles.size)
    }

    @Test
    fun plantState_roundTrip() {
        val plant = PlantState(
            stage = PlantStage.FLOWER,
            health = 75,
            totalPomodoros = 42,
            lastProgressAtEpochMs = 1000L,
            lastWitherAtEpochMs = 2000L,
            lastWateredAtEpochMs = 3000L
        )
        val encoded = json.encodeToString(PlantState.serializer(), plant)
        val decoded = json.decodeFromString(PlantState.serializer(), encoded)
        assertEquals(plant, decoded)
    }
}
