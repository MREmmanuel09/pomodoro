package com.emman.pomodoro.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emman.pomodoro.BuildConfig
import com.emman.pomodoro.core.update.UpdateCheckResult
import com.emman.pomodoro.core.update.UpdateChecker
import com.emman.pomodoro.core.update.UpdateInfo
import com.emman.pomodoro.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ManualCheckResult { UP_TO_DATE, ERROR }

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateChecker: UpdateChecker,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _update = MutableStateFlow<UpdateInfo?>(null)
    val update: StateFlow<UpdateInfo?> = _update.asStateFlow()

    private val _manualResult = MutableSharedFlow<ManualCheckResult>()
    val manualResult: SharedFlow<ManualCheckResult> = _manualResult.asSharedFlow()

    private var checking = false

    init {
        viewModelScope.launch {
            val result = updateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
            if (result !is UpdateCheckResult.Available) return@launch
            val dismissed = runCatching {
                settingsRepository.settings.first().dismissedUpdateVersion
            }.getOrNull()
            if (result.info.version != dismissed) {
                _update.value = result.info
            }
        }
    }

    fun checkNow() {
        if (checking) return
        checking = true
        viewModelScope.launch {
            try {
                when (val result = updateChecker.checkForUpdate(BuildConfig.VERSION_NAME)) {
                    is UpdateCheckResult.Available -> _update.value = result.info
                    UpdateCheckResult.UpToDate -> _manualResult.emit(ManualCheckResult.UP_TO_DATE)
                    UpdateCheckResult.Failed -> _manualResult.emit(ManualCheckResult.ERROR)
                }
            } finally {
                checking = false
            }
        }
    }

    fun hide() {
        _update.value = null
    }

    fun dismiss() {
        val info = _update.value ?: return
        _update.value = null
        viewModelScope.launch {
            runCatching {
                settingsRepository.update { it.copy(dismissedUpdateVersion = info.version) }
            }
        }
    }
}
