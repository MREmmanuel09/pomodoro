package com.emman.pomodoro.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emman.pomodoro.R
import com.emman.pomodoro.domain.model.AccentPalette
import com.emman.pomodoro.domain.model.AppSettings
import com.emman.pomodoro.domain.model.AppLanguage
import com.emman.pomodoro.domain.model.ClockStyle
import com.emman.pomodoro.domain.model.ThemeMode
import com.emman.pomodoro.domain.model.TimerTextScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onManageProfiles: () -> Unit = {},
    onManageTags: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    exportViewModel: ExportViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val exportMessage = stringResource(R.string.export_success)
    val importMessage = stringResource(R.string.import_success)
    val importErrorMessage = stringResource(R.string.import_error)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = exportViewModel.exportTo(context, uri)
                snackbarHostState.showSnackbar(
                    if (ok) exportMessage else importErrorMessage
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = exportViewModel.importFrom(context, uri)
                snackbarHostState.showSnackbar(
                    if (ok) importMessage else importErrorMessage
                )
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = stringResource(R.string.settings_section_timer)) {
                Column {
                    Text(
                        stringResource(R.string.settings_daily_goal, settings.dailyGoalPomodoros),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.dailyGoalPomodoros.toFloat(),
                        onValueChange = { value ->
                            viewModel.update { it.copy(dailyGoalPomodoros = value.toInt()) }
                        },
                        valueRange = 1f..16f,
                        steps = 14
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_count_up),
                        subtitle = stringResource(R.string.settings_count_up_desc),
                        checked = settings.countUpMode
                    ) { checked ->
                        viewModel.update { it.copy(countUpMode = checked) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_clock_style),
                        style = MaterialTheme.typography.labelLarge
                    )
                    EnumChips(
                        options = ClockStyle.entries,
                        selected = settings.clockStyle,
                        label = { Text(stringResource(clockLabelRes(it))) }
                    ) { option ->
                        viewModel.update { it.copy(clockStyle = option) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_time_size),
                        style = MaterialTheme.typography.labelLarge
                    )
                    EnumChips(
                        options = TimerTextScale.entries,
                        selected = settings.timerTextScale,
                        label = { Text(it.name) }
                    ) { option ->
                        viewModel.update { it.copy(timerTextScale = option) }
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_section_appearance)) {
                Column {
                    Text(
                        stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.labelLarge
                    )
                    EnumChips(
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        label = { Text(stringResource(themeLabelRes(it))) }
                    ) { option ->
                        viewModel.update { it.copy(themeMode = option) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SwitchRow(
                        title = stringResource(R.string.settings_dynamic_color),
                        subtitle = stringResource(R.string.settings_dynamic_color_desc),
                        checked = settings.dynamicColor
                    ) { checked ->
                        viewModel.update { it.copy(dynamicColor = checked) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_accent_palette),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AccentPalette.entries.forEach { palette ->
                            val selected = settings.accentPalette == palette
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(paletteColor(palette))
                                    .clickable {
                                        viewModel.update { it.copy(accentPalette = palette) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_section_alerts)) {
                Column {
                    SwitchRow(
                        title = stringResource(R.string.settings_cue_sound),
                        checked = settings.cueSoundEnabled
                    ) { checked ->
                        viewModel.update { it.copy(cueSoundEnabled = checked) }
                    }
                    SwitchRow(
                        title = stringResource(R.string.settings_vibration),
                        checked = settings.vibrationEnabled
                    ) { checked ->
                        viewModel.update { it.copy(vibrationEnabled = checked) }
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_section_optional)) {
                Column {
                    SwitchRow(
                        title = stringResource(R.string.settings_plant),
                        subtitle = stringResource(R.string.settings_plant_desc),
                        checked = settings.plantEnabled
                    ) { checked ->
                        viewModel.update { it.copy(plantEnabled = checked) }
                    }
                    SwitchRow(
                        title = stringResource(R.string.settings_tags),
                        subtitle = stringResource(R.string.settings_tags_desc),
                        checked = settings.tagsEnabled
                    ) { checked ->
                        viewModel.update { it.copy(tagsEnabled = checked) }
                    }
                    SwitchRow(
                        title = stringResource(R.string.settings_strict),
                        subtitle = stringResource(R.string.settings_strict_desc),
                        checked = settings.strictModeEnabled
                    ) { checked ->
                        viewModel.update { it.copy(strictModeEnabled = checked) }
                    }
                    SwitchRow(
                        title = stringResource(R.string.settings_ambient),
                        subtitle = stringResource(R.string.settings_ambient_desc),
                        checked = settings.ambientSoundsEnabled
                    ) { checked ->
                        viewModel.update { it.copy(ambientSoundsEnabled = checked) }
                    }
                }
            }

            SectionCard(title = stringResource(R.string.settings_section_manage)) {
                Column {
                    NavRow(
                        title = stringResource(R.string.settings_profiles),
                        onClick = onManageProfiles
                    )
                    NavRow(
                        title = stringResource(R.string.settings_tags_manage),
                        onClick = onManageTags
                    )
                    NavRow(
                        title = stringResource(R.string.settings_export),
                        onClick = {
                            val timestamp = SimpleDateFormat(
                                "yyyyMMdd_HHmm",
                                Locale.US
                            ).format(Date())
                            exportLauncher.launch("pomodoro_backup_$timestamp.json")
                        }
                    )
                    NavRow(
                        title = stringResource(R.string.settings_import),
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                    )
                }
            }

            SectionCard(title = stringResource(R.string.settings_section_language)) {
                EnumChips(
                    options = AppLanguage.entries,
                    selected = settings.language,
                    label = { Text(stringResource(languageLabelRes(it))) }
                ) { option ->
                    viewModel.update { it.copy(language = option) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NavRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun <T> EnumChips(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> Unit,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { label(option) }
            )
        }
    }
}

private fun paletteColor(palette: AccentPalette): Color = when (palette) {
    AccentPalette.TOMATO -> Color(0xFFC0392B)
    AccentPalette.FOREST -> Color(0xFF2E7D32)
    AccentPalette.OCEAN -> Color(0xFF0277BD)
    AccentPalette.LAVENDER -> Color(0xFF5E35B1)
    AccentPalette.AMBER -> Color(0xFFF57F17)
}

private fun clockLabelRes(style: ClockStyle): Int = when (style) {
    ClockStyle.DIGITAL_LARGE -> R.string.clock_digital_large
    ClockStyle.DIGITAL_MINIMAL -> R.string.clock_digital_minimal
    ClockStyle.ANALOG -> R.string.clock_analog
    ClockStyle.RING -> R.string.clock_ring
}

private fun themeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun languageLabelRes(language: AppLanguage): Int = when (language) {
    AppLanguage.SYSTEM -> R.string.language_system
    AppLanguage.ES -> R.string.language_es
    AppLanguage.EN -> R.string.language_en
}
