package com.emman.pomodoro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.emman.pomodoro.domain.model.AccentPalette
import com.emman.pomodoro.domain.model.ThemeMode

private fun schemeFor(palette: AccentPalette, dark: Boolean) = when (palette) {
    AccentPalette.TOMATO -> if (dark) {
        darkColorScheme(primary = Tomato80, secondary = Leaf80, tertiary = BreakBlue80, background = NeutralDark)
    } else {
        lightColorScheme(primary = Tomato40, secondary = Leaf40, tertiary = BreakBlue40, background = NeutralLight)
    }
    AccentPalette.FOREST -> if (dark) {
        darkColorScheme(primary = Forest80, secondary = Tomato80, tertiary = ForestBreak80, background = NeutralDark)
    } else {
        lightColorScheme(primary = Forest40, secondary = Tomato40, tertiary = ForestBreak40, background = NeutralLight)
    }
    AccentPalette.OCEAN -> if (dark) {
        darkColorScheme(primary = Ocean80, secondary = Leaf80, tertiary = OceanBreak80, background = NeutralDark)
    } else {
        lightColorScheme(primary = Ocean40, secondary = Leaf40, tertiary = OceanBreak40, background = NeutralLight)
    }
    AccentPalette.LAVENDER -> if (dark) {
        darkColorScheme(primary = Lavender80, secondary = BreakBlue80, tertiary = LavenderBreak80, background = NeutralDark)
    } else {
        lightColorScheme(primary = Lavender40, secondary = BreakBlue40, tertiary = LavenderBreak40, background = NeutralLight)
    }
    AccentPalette.AMBER -> if (dark) {
        darkColorScheme(primary = Amber80, secondary = Leaf80, tertiary = AmberBreak80, background = NeutralDark)
    } else {
        lightColorScheme(primary = Amber40, secondary = Leaf40, tertiary = AmberBreak40, background = NeutralLight)
    }
}

@Composable
fun PomodoroTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    accentPalette: AccentPalette = AccentPalette.TOMATO,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val useDynamic = dynamicColor &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        accentPalette == AccentPalette.TOMATO

    val colorScheme = when {
        useDynamic -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> schemeFor(accentPalette, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
