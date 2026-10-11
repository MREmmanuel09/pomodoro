package com.emman.pomodoro.ui.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emman.pomodoro.R
import com.emman.pomodoro.core.util.TimeFormat
import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.PhaseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    onBack: () -> Unit,
    viewModel: ProfileEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.saved) {
        onBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.profile_editor_new
                            else R.string.profile_editor_edit
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::rename,
                label = { Text(stringResource(R.string.profile_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.profile_phases, state.phases.size),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(
                        R.string.profile_round,
                        TimeFormat.minutesLabel(state.totalSeconds)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(state.phases, key = { index, _ -> index }) { index, phase ->
                    PhaseEditorRow(
                        phase = phase,
                        index = index,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.phases.size - 1,
                        canDelete = state.phases.size > 1,
                        onTypeChange = { viewModel.updatePhaseType(index, it) },
                        onDurationChange = { viewModel.updatePhaseDuration(index, it) },
                        onToggleAutoAdvance = { viewModel.togglePhaseAutoAdvance(index) },
                        onMoveUp = { viewModel.movePhase(index, up = true) },
                        onMoveDown = { viewModel.movePhase(index, up = false) },
                        onDelete = { viewModel.removePhase(index) }
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.addPhase(PhaseType.WORK) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.profile_add_work))
                        }
                        OutlinedButton(onClick = { viewModel.addPhase(PhaseType.SHORT_BREAK) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.break_short))
                        }
                        OutlinedButton(onClick = { viewModel.addPhase(PhaseType.LONG_BREAK) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.break_long))
                        }
                    }
                }
            }

            state.error?.let { error ->
                Text(
                    stringResource(
                        when (error) {
                            ProfileEditorError.NOT_FOUND -> R.string.profile_error_not_found
                            ProfileEditorError.INVALID -> R.string.profile_error_invalid
                        }
                    ),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Button(
                onClick = viewModel::save,
                enabled = state.isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(stringResource(R.string.profile_save))
            }
        }
    }
}

@Composable
private fun PhaseEditorRow(
    phase: Phase,
    index: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canDelete: Boolean,
    onTypeChange: (PhaseType) -> Unit,
    onDurationChange: (Int) -> Unit,
    onToggleAutoAdvance: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }
    val minutes = phase.durationSeconds / 60

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "#${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(onClick = { typeMenuExpanded = true }) {
                    Text(stringResource(phaseLabelRes(phase.type)))
                }
                DropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.phase_work)) },
                        onClick = {
                            onTypeChange(PhaseType.WORK)
                            typeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.phase_short_break)) },
                        onClick = {
                            onTypeChange(PhaseType.SHORT_BREAK)
                            typeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.phase_long_break)) },
                        onClick = {
                            onTypeChange(PhaseType.LONG_BREAK)
                            typeMenuExpanded = false
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.profile_move_up)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.profile_move_down)
                    )
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.profile_delete_phase)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { onDurationChange((minutes - 1) * 60) },
                    enabled = minutes > 1
                ) {
                    Text("−")
                }
                Text(
                    "$minutes min",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(72.dp),
                    textAlign = TextAlign.Center
                )
                FilledTonalIconButton(
                    onClick = { onDurationChange((minutes + 1) * 60) },
                    enabled = minutes < 180
                ) {
                    Text("+")
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    stringResource(R.string.profile_auto),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = phase.autoAdvance, onCheckedChange = { onToggleAutoAdvance() })
            }
        }
    }
}

private fun phaseLabelRes(type: PhaseType): Int = when (type) {
    PhaseType.WORK -> R.string.phase_work
    PhaseType.SHORT_BREAK -> R.string.phase_short_break
    PhaseType.LONG_BREAK -> R.string.phase_long_break
}
