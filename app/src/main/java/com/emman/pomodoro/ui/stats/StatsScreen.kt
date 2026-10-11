package com.emman.pomodoro.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emman.pomodoro.R
import com.emman.pomodoro.core.util.TimeFormat
import com.emman.pomodoro.domain.model.DayStats
import com.emman.pomodoro.domain.model.PhaseType
import com.emman.pomodoro.domain.model.Session
import com.emman.pomodoro.domain.model.SessionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryCards(state)
            }
            item {
                StreakCard(state)
            }
            if (state.tagDistribution.isNotEmpty()) {
                item {
                    TagDistributionCard(state.tagDistribution)
                }
            }
            item {
                Text(
                    stringResource(R.string.stats_last_30_days),
                    style = MaterialTheme.typography.titleMedium
                )
                BarChart(
                    days = state.summary.last30Days,
                    selectedDayStart = state.selectedDayStart,
                    onSelectDay = { day -> viewModel.selectDay(day) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
            state.selectedDaySessions?.let { sessions ->
                item {
                    DayDetailCard(
                        dayStart = state.selectedDayStart ?: 0,
                        sessions = sessions,
                        onDismiss = { viewModel.selectDay(null) }
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.stats_recent),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(state.recentSessions, key = { it.id }) { session ->
                SessionRow(session)
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SummaryCards(state: StatsUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            value = "${state.summary.todayPomodoros}",
            label = stringResource(R.string.stats_today),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = TimeFormat.minutesLabel(state.summary.todayFocusedSeconds),
            label = stringResource(R.string.stats_focused_today),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = "${state.summary.totalPomodoros}",
            label = stringResource(R.string.stats_total),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreakCard(state: StatsUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(R.string.stats_streak_current, state.summary.currentStreakDays),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.stats_streak_best, state.summary.bestStreakDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val goal = state.summary
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (goal.goalReached) {
                        stringResource(R.string.stats_goal_done)
                    } else {
                        stringResource(R.string.stats_goal_label, goal.dailyGoal)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (goal.goalReached) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun TagDistributionCard(entries: List<TagDistributionEntry>) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.stats_by_tag),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        entry.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(100.dp),
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = entry.fraction)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${entry.pomodoros}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BarChart(
    days: List<DayStats>,
    selectedDayStart: Long?,
    onSelectDay: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val barIdle = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val maxPomodoros = (days.maxOfOrNull { it.completedPomodoros } ?: 0).coerceAtLeast(1)
    val dateFormatter = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(days) {
                    detectTapGestures { offset ->
                        if (days.isEmpty()) return@detectTapGestures
                        val index = (offset.x / (size.width / days.size))
                            .toInt()
                            .coerceIn(days.indices)
                        val day = days[index]
                        onSelectDay(
                            if (day.dayStartEpochMs == selectedDayStart) null
                            else day.dayStartEpochMs
                        )
                    }
                }
        ) {
            if (days.isEmpty()) return@Canvas
            val slotWidth = size.width / days.size
            val barWidth = slotWidth * 0.6f
            val gap = (slotWidth - barWidth) / 2
            val labelHeight = 28f

            days.forEachIndexed { index, day ->
                val heightRatio = day.completedPomodoros.toFloat() / maxPomodoros
                val barHeight = (size.height - labelHeight) * heightRatio
                val left = index * slotWidth + gap
                val top = size.height - labelHeight - barHeight
                val color = when {
                    day.dayStartEpochMs == selectedDayStart -> selectedColor
                    day.completedPomodoros > 0 -> barColor
                    else -> barIdle
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                )
            }

            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, size.height - labelHeight),
                end = Offset(size.width, size.height - labelHeight),
                strokeWidth = 1f
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (days.isNotEmpty()) {
                Text(
                    dateFormatter.format(Date(days.first().dayStartEpochMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.stats_tap_bar),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    dateFormatter.format(Date(days.last().dayStartEpochMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DayDetailCard(
    dayStart: Long,
    sessions: List<Session>,
    onDismiss: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("EEEE dd 'de' MMMM", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                dateFormatter.format(Date(dayStart)).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.stats_sessions_close, sessions.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            sessions.forEach { session ->
                SessionRow(session, compact = true)
            }
        }
    }
}

@Composable
private fun SessionRow(session: Session, compact: Boolean = false) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 2.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            when (session.phaseType) {
                PhaseType.WORK -> "🍅"
                PhaseType.SHORT_BREAK -> "☕"
                PhaseType.LONG_BREAK -> "🌿"
            },
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                when (session.phaseType) {
                    PhaseType.WORK -> stringResource(R.string.phase_work)
                    PhaseType.SHORT_BREAK -> stringResource(R.string.phase_short_break)
                    PhaseType.LONG_BREAK -> stringResource(R.string.phase_long_break)
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${timeFormat.format(Date(session.completedAtEpochMs))} · " +
                    TimeFormat.minutesLabel(session.actualDurationSec),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            when (session.status) {
                SessionStatus.COMPLETED -> "✓"
                SessionStatus.SKIPPED -> "→"
                SessionStatus.ABORTED -> "✕"
            },
            color = when (session.status) {
                SessionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
