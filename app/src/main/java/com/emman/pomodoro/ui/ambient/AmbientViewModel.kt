package com.emman.pomodoro.ui.ambient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emman.pomodoro.core.audio.AmbientSound
import com.emman.pomodoro.core.audio.AmbientSoundPlayer
import com.emman.pomodoro.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AmbientUiState(
    val enabled: Boolean = false,
    val sound: AmbientSound = AmbientSound.RAIN,
    val playing: Boolean = false
)

@HiltViewModel
class AmbientViewModel @Inject constructor(
    private val player: AmbientSoundPlayer,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val playing = MutableStateFlow(false)

    val uiState: StateFlow<AmbientUiState> = combine(
        settingsRepository.settings,
        playing
    ) { settings, isPlaying ->
        AmbientUiState(
            enabled = settings.ambientSoundsEnabled,
            sound = AmbientSound.fromId(settings.ambientSoundName),
            playing = isPlaying
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AmbientUiState())

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                if (!settings.ambientSoundsEnabled && playing.value) {
                    stop()
                }
            }
        }
    }

    fun onSoundSelected(sound: AmbientSound) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(ambientSoundName = sound.id) }
            play(sound)
        }
    }

    fun toggle() {
        val current = uiState.value
        if (current.playing) {
            stop()
        } else {
            play(current.sound)
        }
    }

    private fun play(sound: AmbientSound) {
        viewModelScope.launch {
            player.play(sound)
            playing.value = true
        }
    }

    private fun stop() {
        player.stop()
        playing.value = false
    }

    override fun onCleared() {
        player.stop()
        super.onCleared()
    }
}
