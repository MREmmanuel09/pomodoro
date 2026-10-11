package com.emman.pomodoro.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.TimerProfile
import com.emman.pomodoro.domain.repository.ProfileRepository
import com.emman.pomodoro.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProfileListUiState(
    val profiles: List<TimerProfile> = emptyList(),
    val activeProfileId: String = TimerProfile.CLASSIC_ID
)

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileListUiState> = combine(
        profileRepository.profiles,
        settingsRepository.settings
    ) { profiles, settings ->
        ProfileListUiState(
            profiles = profiles,
            activeProfileId = settings.activeProfileId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileListUiState())

    fun setActive(profileId: String) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(activeProfileId = profileId) }
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profileId)
            val current = uiState.value
            if (current.activeProfileId == profileId) {
                val fallback = current.profiles.find { it.id != profileId }
                if (fallback != null) {
                    settingsRepository.update { it.copy(activeProfileId = fallback.id) }
                }
            }
        }
    }

    fun duplicateProfile(profileId: String) {
        viewModelScope.launch {
            val original = profileRepository.getProfile(profileId) ?: return@launch
            profileRepository.saveProfile(
                original.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${original.name} (copia)"
                )
            )
        }
    }

    fun createNewProfile() {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            profileRepository.saveProfile(
                TimerProfile(
                    id = id,
                    name = "Mi perfil",
                    phases = listOf(
                        Phase(PhaseType.WORK, 25 * 60),
                        Phase(PhaseType.SHORT_BREAK, 5 * 60)
                    )
                )
            )
            settingsRepository.update { it.copy(activeProfileId = id) }
        }
    }
}
