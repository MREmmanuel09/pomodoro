package com.emman.pomodoro.ui.plant

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emman.pomodoro.R
import com.emman.pomodoro.domain.model.PlantStage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantScreen(
    viewModel: PlantViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val plant = state.plant
    var showHelp by remember { mutableStateOf(false) }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.plant_help_title)) },
            text = { Text(stringResource(R.string.plant_help_body)) },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plant_title)) },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.plant_help))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = plantEmoji(plant.stage),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.size(140.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(stageLabelRes(plant.stage)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (plant.isWithered) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.plant_withered),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.plant_health),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            "${plant.clampedHealth}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { plant.clampedHealth / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            plant.isWithered -> MaterialTheme.colorScheme.error
                            plant.clampedHealth < 50 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    value = "${plant.totalPomodoros}",
                    label = stringResource(R.string.plant_pomodoros_total),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "${state.currentStreakDays}",
                    label = stringResource(R.string.plant_streak),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            state.pomodorosToNextStage?.let { remaining ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(
                                R.string.plant_next_stage,
                                stringResource(stageLabelRes(plant.stage.next))
                            ),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.plant_pomodoros_left, remaining),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } ?: run {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        stringResource(R.string.plant_max_stage),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            FilledTonalButton(
                onClick = viewModel::water,
                enabled = state.canWater,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (state.canWater) {
                        stringResource(R.string.plant_water)
                    } else {
                        stringResource(R.string.plant_watered)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun plantEmoji(stage: PlantStage): String = when (stage) {
    PlantStage.SEED -> "🌱"
    PlantStage.SPROUT -> "🌿"
    PlantStage.YOUNG -> "🪴"
    PlantStage.FLOWER -> "🌸"
    PlantStage.TREE -> "🌳"
}

private fun stageLabelRes(stage: PlantStage): Int = when (stage) {
    PlantStage.SEED -> R.string.plant_stage_seed
    PlantStage.SPROUT -> R.string.plant_stage_sprout
    PlantStage.YOUNG -> R.string.plant_stage_young
    PlantStage.FLOWER -> R.string.plant_stage_flower
    PlantStage.TREE -> R.string.plant_stage_tree
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
