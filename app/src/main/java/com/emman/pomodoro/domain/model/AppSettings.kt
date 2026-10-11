package com.emman.pomodoro.domain.model

import kotlinx.serialization.Serializable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentPalette { TOMATO, FOREST, OCEAN, LAVENDER, AMBER }

enum class ClockStyle { DIGITAL_LARGE, DIGITAL_MINIMAL, ANALOG, RING }

enum class TimerTextScale { S, M, L }

enum class AppLanguage { SYSTEM, ES, EN }

@Serializable
data class AppSettings(
    val activeProfileId: String = TimerProfile.CLASSIC_ID,
    val countUpMode: Boolean = false,
    val cueSoundEnabled: Boolean = true,
    val cueSoundName: String = "chime",
    val cueVolume: Float = 0.8f,
    val vibrationEnabled: Boolean = true,
    val dailyGoalPomodoros: Int = 8,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accentPalette: AccentPalette = AccentPalette.TOMATO,
    val clockStyle: ClockStyle = ClockStyle.DIGITAL_LARGE,
    val timerTextScale: TimerTextScale = TimerTextScale.M,
    val plantEnabled: Boolean = true,
    val tagsEnabled: Boolean = false,
    val strictModeEnabled: Boolean = false,
    val ambientSoundsEnabled: Boolean = false,
    val ambientSoundName: String = "rain",
    val language: AppLanguage = AppLanguage.SYSTEM
)
