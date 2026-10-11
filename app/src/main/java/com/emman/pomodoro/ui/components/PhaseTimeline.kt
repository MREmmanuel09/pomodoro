package com.emman.pomodoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emman.pomodoro.domain.model.Phase
import com.emman.pomodoro.domain.model.PhaseType

@Composable
fun PhaseTimeline(
    phases: List<Phase>,
    currentIndex: Int,
    completedCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {        phases.forEachIndexed { index, phase ->
            val isActive = index == currentIndex
            val isDone = index < completedCount
            val color = when {
                isActive -> phaseColor(phase.type)
                isDone -> phaseColor(phase.type).copy(alpha = 0.55f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            }
            val weight = if (phase.durationSeconds >= 60 * 60) 2f else 1f
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(if (isActive) 10.dp else 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

private fun phaseColor(type: PhaseType): Color = when (type) {
    PhaseType.WORK -> Color(0xFFC0392B)
    PhaseType.SHORT_BREAK -> Color(0xFF1565C0)
    PhaseType.LONG_BREAK -> Color(0xFF2E7D32)
}
