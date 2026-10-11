package com.emman.pomodoro.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emman.pomodoro.R
import com.emman.pomodoro.core.audio.AmbientSound
import com.emman.pomodoro.core.util.TimeFormat
import com.emman.pomodoro.domain.model.ClockStyle
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.Tag
import com.emman.pomodoro.domain.model.TimerProfile
import com.emman.pomodoro.domain.model.TimerStatus
import com.emman.pomodoro.domain.model.TimerTextScale
import com.emman.pomodoro.ui.ambient.AmbientUiState
import com.emman.pomodoro.ui.ambient.AmbientViewModel
import com.emman.pomodoro.ui.components.AnalogClock
import com.emman.pomodoro.ui.components.PhaseTimeline
import com.emman.pomodoro.ui.components.PomodoroClock

@Composable
fun TimerScreen(
    onManageProfiles: () -> Unit = {},
    onManageTags: () -> Unit = {},
    viewModel: TimerViewModel = hiltViewModel(),
    ambientViewModel: AmbientViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ambient by ambientViewModel.uiState.collectAsStateWithLifecycle()
    val timer = state.timerState
    val phase = timer.currentPhase
    val phaseType = timer.currentPhaseType

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileSelector(
            profiles = state.profiles,
            activeProfileId = state.activeProfile.id,
            onSelect = viewModel::selectProfile,
            onManage = onManageProfiles
        )

        if (state.tagsEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            TagSelector(
                tags = state.tags,
                activeTagId = timer.activeTagId,
                onSelect = viewModel::setTag,
                onManage = onManageTags
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        stringResource(
                            R.string.timer_today_goal,
                            state.completedToday,
                            state.dailyGoal
                        )
                    )
                }
            )
            if (timer.pomodorosCompletedInRound > 0) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            stringResource(
                                R.string.timer_round_number,
                                timer.pomodorosCompletedInRound
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(phaseLabelRes(phaseType)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        val plannedSeconds = state.activeProfile.phases
            .getOrNull(timer.phaseIndex)?.durationSeconds ?: 25 * 60
        val displaySeconds = if (state.countUpMode) {
            (plannedSeconds - timer.remainingSeconds).coerceAtLeast(0)
        } else {
            timer.remainingSeconds
        }
        val timeText = TimeFormat.mmss(displaySeconds)
        val scale = when (state.timerTextScale) {
            TimerTextScale.S -> 0.85f
            TimerTextScale.M -> 1f
            TimerTextScale.L -> 1.25f
        }
        when (state.clockStyle) {
            ClockStyle.RING -> PomodoroClock(
                timeText = timeText,
                progress = timer.progress,
                phaseType = phaseType,
                ringSize = 280.dp,
                timeFontSizeSp = 56f * scale
            )
            ClockStyle.ANALOG -> AnalogClock(
                timeText = timeText,
                progress = timer.progress,
                phaseType = phaseType,
                size = 280.dp
            )
            ClockStyle.DIGITAL_LARGE -> PomodoroClock(
                timeText = timeText,
                progress = timer.progress,
                phaseType = phaseType,
                ringSize = 280.dp,
                showRing = false,
                timeFontSizeSp = 72f * scale
            )
            ClockStyle.DIGITAL_MINIMAL -> PomodoroClock(
                timeText = timeText,
                progress = timer.progress,
                phaseType = phaseType,
                ringSize = 200.dp,
                showRing = false,
                timeFontSizeSp = 48f * scale
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PhaseTimeline(
            phases = state.activeProfile.phases,
            currentIndex = timer.phaseIndex,
            completedCount = timer.phaseIndex,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (phase != null) {
            Text(
                text = stringResource(
                    R.string.timer_phase_of,
                    timer.phaseIndex + 1,
                    state.activeProfile.phases.size,
                    TimeFormat.minutesLabel(phase.durationSeconds)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        TimerControls(
            status = timer.status,
            onTogglePlayPause = viewModel::togglePlayPause,
            onReset = viewModel::reset,
            onSkip = viewModel::skipPhase,
            onAdjust = viewModel::adjust,
            strictMode = state.strictModeEnabled
        )

        if (ambient.enabled) {
            Spacer(modifier = Modifier.height(16.dp))
            AmbientControls(
                ambient = ambient,
                onSelect = ambientViewModel::onSoundSelected,
                onToggle = ambientViewModel::toggle
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (timer.status == TimerStatus.FINISHED) {
            Text(
                text = stringResource(R.string.timer_round_done),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = viewModel::reset) {
                Text(stringResource(R.string.timer_new_round))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            progress = { state.completedToday.toFloat() / state.dailyGoal.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(
                R.string.timer_daily_goal,
                state.completedToday,
                state.dailyGoal
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ProfileSelector(
    profiles: List<TimerProfile>,
    activeProfileId: String,
    onSelect: (String) -> Unit,
    onManage: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val active = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(active?.name ?: stringResource(R.string.timer_profile))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = { Text(profile.name) },
                    onClick = {
                        onSelect(profile.id)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.timer_manage_profiles)) },
                onClick = {
                    expanded = false
                    onManage()
                }
            )
        }
    }
}

@Composable
private fun TagSelector(
    tags: List<Tag>,
    activeTagId: Long?,
    onSelect: (Long?) -> Unit,
    onManage: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeTagId == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.tag_none)) }
            )
            tags.forEach { tag ->
                FilterChip(
                    selected = activeTagId == tag.id,
                    onClick = {
                        onSelect(if (activeTagId == tag.id) null else tag.id)
                    },
                    label = { Text(tag.name) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(tag.colorArgb))
                        )
                    }
                )
            }
        }
        IconButton(onClick = onManage, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.timer_manage_tags),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AmbientControls(
    ambient: AmbientUiState,
    onSelect: (AmbientSound) -> Unit,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = ambient.sound == AmbientSound.RAIN && ambient.playing,
                onClick = { onSelect(AmbientSound.RAIN) },
                label = { Text(stringResource(R.string.ambient_rain)) }
            )
            FilterChip(
                selected = ambient.sound == AmbientSound.WAVES && ambient.playing,
                onClick = { onSelect(AmbientSound.WAVES) },
                label = { Text(stringResource(R.string.ambient_waves)) }
            )
            FilterChip(
                selected = ambient.sound == AmbientSound.FOREST && ambient.playing,
                onClick = { onSelect(AmbientSound.FOREST) },
                label = { Text(stringResource(R.string.ambient_forest)) }
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (ambient.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(
                    if (ambient.playing) R.string.ambient_pause else R.string.ambient_play
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimerControls(
    status: TimerStatus,
    onTogglePlayPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onAdjust: (Int) -> Unit,
    strictMode: Boolean
) {
    val canAdjust = status == TimerStatus.RUNNING || status == TimerStatus.PAUSED
    val canSkip = status == TimerStatus.RUNNING || status == TimerStatus.PAUSED

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { onAdjust(-60) },
                enabled = canAdjust,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = stringResource(R.string.timer_minus_min)
                )
            }

            Button(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (status == TimerStatus.RUNNING) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = if (status == TimerStatus.RUNNING) {
                        stringResource(R.string.timer_pause)
                    } else {
                        stringResource(R.string.timer_play)
                    },
                    modifier = Modifier.size(40.dp)
                )
            }

            FilledTonalIconButton(
                onClick = { onAdjust(60) },
                enabled = canAdjust,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.timer_plus_min)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onReset) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.size(4.dp))
                Text(stringResource(R.string.timer_reset))
            }
            OutlinedButton(onClick = onSkip, enabled = canSkip) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    if (strictMode) {
                        stringResource(R.string.timer_skip_strict)
                    } else {
                        stringResource(R.string.timer_skip)
                    }
                )
            }
        }
    }
}

private fun phaseLabelRes(type: PhaseType?): Int = when (type) {
    PhaseType.WORK -> R.string.phase_work
    PhaseType.SHORT_BREAK -> R.string.phase_short_break
    PhaseType.LONG_BREAK -> R.string.phase_long_break
    null -> R.string.phase_ready
}
