package com.emman.pomodoro.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SpaIcon: ImageVector
    get() {
        val stroke = SolidColor(Color(0xFF2E7D32))
        return ImageVector.Builder(
            name = "Spa",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(stroke = stroke, strokeLineWidth = 1.8f) {
                moveTo(12f, 20f)
                lineTo(12f, 10f)
            }
            path(stroke = stroke, strokeLineWidth = 1.8f) {
                moveTo(12f, 14f)
                curveTo(8f, 14f, 6f, 11f, 6f, 8f)
                curveTo(10f, 8f, 12f, 11f, 12f, 14f)
            }
            path(stroke = stroke, strokeLineWidth = 1.8f) {
                moveTo(12f, 12f)
                curveTo(16f, 12f, 18f, 9f, 18f, 6f)
                curveTo(14f, 6f, 12f, 9f, 12f, 12f)
            }
        }.build()
    }
