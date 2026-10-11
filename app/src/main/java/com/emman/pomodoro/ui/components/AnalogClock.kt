package com.emman.pomodoro.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emman.pomodoro.domain.model.PhaseType

@Composable
fun AnalogClock(
    timeText: String,
    progress: Float,
    phaseType: PhaseType?,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp
) {
    val handColor = when (phaseType) {
        PhaseType.WORK -> MaterialTheme.colorScheme.primary
        PhaseType.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
        PhaseType.LONG_BREAK -> MaterialTheme.colorScheme.secondary
        null -> MaterialTheme.colorScheme.primary
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "analogHand"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = this.size.minDimension / 2 - 8.dp.toPx()

            drawCircle(
                color = handColor.copy(alpha = 0.08f),
                radius = radius,
                center = center
            )
            drawCircle(
                color = handColor.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30).toDouble())
                val outer = radius * 0.92f
                val inner = if (i % 3 == 0) radius * 0.78f else radius * 0.85f
                val tickColor = handColor.copy(alpha = if (i % 3 == 0) 0.8f else 0.35f)
                drawLine(
                    color = tickColor,
                    start = center + Offset(
                        (inner * kotlin.math.sin(angle)).toFloat(),
                        -(inner * kotlin.math.cos(angle)).toFloat()
                    ),
                    end = center + Offset(
                        (outer * kotlin.math.sin(angle)).toFloat(),
                        -(outer * kotlin.math.cos(angle)).toFloat()
                    ),
                    strokeWidth = if (i % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val handAngle = Math.toRadians((animatedProgress * 360).toDouble())
            val handLength = radius * 0.7f
            drawLine(
                color = handColor,
                start = center,
                end = center + Offset(
                    (handLength * kotlin.math.sin(handAngle)).toFloat(),
                    -(handLength * kotlin.math.cos(handAngle)).toFloat()
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = handColor, radius = 6.dp.toPx(), center = center)
        }
        Text(
            text = timeText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
