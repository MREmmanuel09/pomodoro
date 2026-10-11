package com.emman.pomodoro.ui.profiles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.TimerProfile
import com.emman.pomodoro.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ProfileEditorError { NOT_FOUND, INVALID }

data class ProfileEditorUiState(
    val profileId: String = "",
    val name: String = "",
    val phases: List<Phase> = emptyList(),
    val isNew: Boolean = false,
    val saved: Boolean = false,
    val error: ProfileEditorError? = null
) {
    val isValid: Boolean
        get() = name.isNotBlank() && phases.isNotEmpty()
    val totalSeconds: Int
        get() = phases.sumOf { it.durationSeconds }
}

@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val profileId: String = savedStateHandle.get<String>("profileId") ?: ""

    private val _uiState = MutableStateFlow(ProfileEditorUiState())
    val uiState: StateFlow<ProfileEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (profileId == NEW_ID || profileId.isEmpty()) {
                _uiState.value = ProfileEditorUiState(
                    profileId = UUID.randomUUID().toString(),
                    name = "Mi perfil",
                    phases = listOf(
                        Phase(PhaseType.WORK, 25 * 60),
                        Phase(PhaseType.SHORT_BREAK, 5 * 60)
                    ),
                    isNew = true
                )
            } else {
                val profile = profileRepository.getProfile(profileId)
                if (profile != null) {
                    _uiState.value = ProfileEditorUiState(
                        profileId = profile.id,
                        name = profile.name,
                        phases = profile.phases
                    )
                } else {
                    _uiState.update { it.copy(error = ProfileEditorError.NOT_FOUND) }
                }
            }
        }
    }

    fun rename(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updatePhaseType(index: Int, type: PhaseType) {
        _uiState.update { state ->
            state.copy(phases = state.phases.mapIndexed { i, p ->
                if (i == index) p.copy(type = type) else p
            })
        }
    }

    fun updatePhaseDuration(index: Int, durationSeconds: Int) {
        val clamped = durationSeconds.coerceIn(60, 180 * 60)
        _uiState.update { state ->
            state.copy(phases = state.phases.mapIndexed { i, p ->
                if (i == index) p.copy(durationSeconds = clamped) else p
            })
        }
    }

    fun togglePhaseAutoAdvance(index: Int) {
        _uiState.update { state ->
            state.copy(phases = state.phases.mapIndexed { i, p ->
                if (i == index) p.copy(autoAdvance = !p.autoAdvance) else p
            })
        }
    }

    fun movePhase(index: Int, up: Boolean) {
        _uiState.update { state ->
            val target = if (up) index - 1 else index + 1
            if (target !in state.phases.indices) return@update state
            val mutable = state.phases.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(target, item)
            state.copy(phases = mutable)
        }
    }

    fun removePhase(index: Int) {
        _uiState.update { state ->
            if (state.phases.size <= 1) return@update state
            state.copy(phases = state.phases.filterIndexed { i, _ -> i != index })
        }
    }

    fun addPhase(type: PhaseType) {
        _uiState.update { state ->
            state.copy(
                phases = state.phases + Phase(
                    type = type,
                    durationSeconds = if (type == PhaseType.WORK) 25 * 60 else 5 * 60
                )
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = ProfileEditorError.INVALID) }
            return
        }
        viewModelScope.launch {
            profileRepository.saveProfile(
                TimerProfile(
                    id = state.profileId,
                    name = state.name.trim(),
                    phases = state.phases
                )
            )
            _uiState.update { it.copy(saved = true, error = null) }
        }
    }

    companion object {
        const val NEW_ID = "new"
    }
}
