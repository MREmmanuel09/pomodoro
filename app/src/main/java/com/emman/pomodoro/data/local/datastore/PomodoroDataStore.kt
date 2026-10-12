package com.emman.pomodoro.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.emman.pomodoro.domain.model.AccentPalette
import com.emman.pomodoro.domain.model.AppSettings
import com.emman.pomodoro.domain.model.AppLanguage
import com.emman.pomodoro.domain.model.ClockStyle
import com.emman.pomodoro.domain.model.PlantStage
import com.emman.pomodoro.domain.model.PlantState
import com.emman.pomodoro.domain.model.ThemeMode
import com.emman.pomodoro.domain.model.TimerProfile
import com.emman.pomodoro.domain.model.TimerSnapshot
import com.emman.pomodoro.domain.model.TimerTextScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val COUNT_UP_MODE = booleanPreferencesKey("count_up_mode")
        val CUE_SOUND_ENABLED = booleanPreferencesKey("cue_sound_enabled")
        val CUE_SOUND_NAME = stringPreferencesKey("cue_sound_name")
        val CUE_VOLUME = floatPreferencesKey("cue_volume")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val DAILY_GOAL = intPreferencesKey("daily_goal_pomodoros")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val TIMER_TEXT_SCALE = stringPreferencesKey("timer_text_scale")
        val PLANT_ENABLED = booleanPreferencesKey("plant_enabled")
        val TAGS_ENABLED = booleanPreferencesKey("tags_enabled")
        val STRICT_MODE = booleanPreferencesKey("strict_mode_enabled")
        val AMBIENT_SOUNDS = booleanPreferencesKey("ambient_sounds_enabled")
        val AMBIENT_SOUND_NAME = stringPreferencesKey("ambient_sound_name")
        val LANGUAGE = stringPreferencesKey("language")
        val PROFILES_JSON = stringPreferencesKey("profiles_json")
        val PLANT_JSON = stringPreferencesKey("plant_json")
        val TIMER_SNAPSHOT_JSON = stringPreferencesKey("timer_snapshot_json")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }

    val profiles: Flow<List<TimerProfile>> = dataStore.data.map { prefs ->
        prefs[Keys.PROFILES_JSON]?.let { decodeProfiles(it) }
            ?: TimerProfile.defaultProfiles()
    }

    val plant: Flow<PlantState> = dataStore.data.map { prefs ->
        prefs[Keys.PLANT_JSON]?.let { decodePlant(it) } ?: PlantState()
    }

    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val current = prefs.toAppSettings()
            val new = transform(current)
            new.writeTo(prefs)
        }
    }

    suspend fun saveProfiles(profiles: List<TimerProfile>) {
        dataStore.edit { prefs ->
            prefs[Keys.PROFILES_JSON] = json.encodeToString(profiles)
        }
    }

    suspend fun savePlant(state: PlantState) {
        dataStore.edit { prefs ->
            prefs[Keys.PLANT_JSON] = json.encodeToString(state)
        }
    }

    suspend fun saveTimerSnapshot(snapshot: TimerSnapshot) {
        dataStore.edit { prefs ->
            prefs[Keys.TIMER_SNAPSHOT_JSON] = json.encodeToString(snapshot)
        }
    }

    suspend fun clearTimerSnapshot() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.TIMER_SNAPSHOT_JSON)
        }
    }

    suspend fun loadTimerSnapshot(): TimerSnapshot? {
        val raw = dataStore.data.map { it[Keys.TIMER_SNAPSHOT_JSON] }.first()
        return raw?.let {
            runCatching { json.decodeFromString<TimerSnapshot>(it) }.getOrNull()
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_DONE] = done
        }
    }

    private fun decodeProfiles(raw: String): List<TimerProfile> = runCatching {
        json.decodeFromString<List<TimerProfile>>(raw)
    }.getOrElse { TimerProfile.defaultProfiles() }

    private fun decodePlant(raw: String): PlantState = runCatching {
        json.decodeFromString<PlantState>(raw)
    }.getOrElse { PlantState() }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        activeProfileId = this[Keys.ACTIVE_PROFILE_ID] ?: TimerProfile.CLASSIC_ID,
        countUpMode = this[Keys.COUNT_UP_MODE] ?: false,
        cueSoundEnabled = this[Keys.CUE_SOUND_ENABLED] ?: true,
        cueSoundName = this[Keys.CUE_SOUND_NAME] ?: "chime",
        cueVolume = this[Keys.CUE_VOLUME] ?: 0.8f,
        vibrationEnabled = this[Keys.VIBRATION_ENABLED] ?: true,
        dailyGoalPomodoros = this[Keys.DAILY_GOAL] ?: 8,
        themeMode = this[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        dynamicColor = this[Keys.DYNAMIC_COLOR] ?: true,
        accentPalette = this[Keys.ACCENT_PALETTE]?.let { runCatching { AccentPalette.valueOf(it) }.getOrNull() } ?: AccentPalette.TOMATO,
        clockStyle = this[Keys.CLOCK_STYLE]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() } ?: ClockStyle.DIGITAL_LARGE,
        timerTextScale = this[Keys.TIMER_TEXT_SCALE]?.let { runCatching { TimerTextScale.valueOf(it) }.getOrNull() } ?: TimerTextScale.M,
        plantEnabled = this[Keys.PLANT_ENABLED] ?: true,
        tagsEnabled = this[Keys.TAGS_ENABLED] ?: false,
        strictModeEnabled = this[Keys.STRICT_MODE] ?: false,
        ambientSoundsEnabled = this[Keys.AMBIENT_SOUNDS] ?: false,
        ambientSoundName = this[Keys.AMBIENT_SOUND_NAME] ?: "rain",
        language = this[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM
    )

    private fun AppSettings.writeTo(prefs: MutablePreferences) {
        prefs[Keys.ACTIVE_PROFILE_ID] = activeProfileId
        prefs[Keys.COUNT_UP_MODE] = countUpMode
        prefs[Keys.CUE_SOUND_ENABLED] = cueSoundEnabled
        prefs[Keys.CUE_SOUND_NAME] = cueSoundName
        prefs[Keys.CUE_VOLUME] = cueVolume
        prefs[Keys.VIBRATION_ENABLED] = vibrationEnabled
        prefs[Keys.DAILY_GOAL] = dailyGoalPomodoros
        prefs[Keys.THEME_MODE] = themeMode.name
        prefs[Keys.DYNAMIC_COLOR] = dynamicColor
        prefs[Keys.ACCENT_PALETTE] = accentPalette.name
        prefs[Keys.CLOCK_STYLE] = clockStyle.name
        prefs[Keys.TIMER_TEXT_SCALE] = timerTextScale.name
        prefs[Keys.PLANT_ENABLED] = plantEnabled
        prefs[Keys.TAGS_ENABLED] = tagsEnabled
        prefs[Keys.STRICT_MODE] = strictModeEnabled
        prefs[Keys.AMBIENT_SOUNDS] = ambientSoundsEnabled
        prefs[Keys.AMBIENT_SOUND_NAME] = ambientSoundName
        prefs[Keys.LANGUAGE] = language.name
    }

    companion object {
        const val DATA_STORE_FILE_NAME = "pomodoro.preferences_pb"
    }
}
