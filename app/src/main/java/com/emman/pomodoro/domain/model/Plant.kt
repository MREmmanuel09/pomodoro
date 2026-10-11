package com.emman.pomodoro.domain.model

enum class PlantStage {
    SEED,
    SPROUT,
    YOUNG,
    FLOWER,
    TREE;

    val next: PlantStage
        get() = entries.getOrElse(ordinal + 1) { this }
}

data class PlantState(
    val stage: PlantStage = PlantStage.SEED,
    val health: Int = 100,
    val totalPomodoros: Int = 0,
    val lastProgressAtEpochMs: Long = 0,
    val lastWitherAtEpochMs: Long = 0,
    val lastWateredAtEpochMs: Long = 0
) {
    val clampedHealth: Int get() = health.coerceIn(0, 100)
    val isWithered: Boolean get() = clampedHealth <= 20
}
