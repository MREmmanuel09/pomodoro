package com.emman.pomodoro.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.emman.pomodoro.domain.repository.SettingsRepository
import com.emman.pomodoro.R
import com.emman.pomodoro.ui.icons.SpaIcon
import com.emman.pomodoro.ui.onboarding.OnboardingScreen
import com.emman.pomodoro.ui.plant.PlantScreen
import com.emman.pomodoro.ui.profiles.ProfileEditorScreen
import com.emman.pomodoro.ui.profiles.ProfileListScreen
import com.emman.pomodoro.ui.settings.SettingsScreen
import com.emman.pomodoro.ui.stats.StatsScreen
import com.emman.pomodoro.ui.tags.TagsScreen
import com.emman.pomodoro.ui.timer.TimerScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class NavUiState(
    val plantEnabled: Boolean = true,
    val tagsEnabled: Boolean = false,
    val onboardingDone: Boolean = true
)

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val navState: StateFlow<NavUiState> = settingsRepository.settings
        .combine(settingsRepository.onboardingDone) { settings, onboardingDone ->
            NavUiState(
                plantEnabled = settings.plantEnabled,
                tagsEnabled = settings.tagsEnabled,
                onboardingDone = onboardingDone
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NavUiState())
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

object Route {
    const val ONBOARDING = "onboarding"
    const val TIMER = "timer"
    const val STATS = "stats"
    const val PLANT = "plant"
    const val SETTINGS = "settings"
    const val PROFILES = "profiles"
    const val PROFILE_EDIT = "profiles/{profileId}"
    const val TAGS = "tags"
    fun profileEdit(profileId: String) = "profiles/$profileId"
}

@Composable
fun PomodoroNavGraph(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navState by viewModel.navState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(navState.onboardingDone) {
        if (navState.onboardingDone && currentRoute == Route.ONBOARDING) {
            navController.navigate(Route.TIMER) {
                popUpTo(Route.ONBOARDING) { inclusive = true }
            }
        }
    }

    val items = buildList {
        add(NavItem(Route.TIMER, stringResource(R.string.nav_timer), Icons.Default.Timer))
        add(NavItem(Route.STATS, stringResource(R.string.nav_stats), Icons.Default.BarChart))
        if (navState.plantEnabled) {
            add(NavItem(Route.PLANT, stringResource(R.string.nav_plant), SpaIcon))
        }
        add(NavItem(Route.SETTINGS, stringResource(R.string.nav_settings), Icons.Default.Settings))
    }

    Scaffold(
        bottomBar = {
            if (items.any { it.route == currentRoute }) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Route.TIMER) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (navState.onboardingDone) Route.TIMER else Route.ONBOARDING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.ONBOARDING) {
                OnboardingScreen()
            }
            composable(Route.TIMER) {
                TimerScreen(
                    onManageProfiles = { navController.navigate(Route.PROFILES) },
                    onManageTags = { navController.navigate(Route.TAGS) }
                )
            }
            composable(Route.STATS) { StatsScreen() }
            composable(Route.PLANT) { PlantScreen() }
            composable(Route.SETTINGS) {
                SettingsScreen(
                    onManageProfiles = { navController.navigate(Route.PROFILES) },
                    onManageTags = { navController.navigate(Route.TAGS) }
                )
            }
            composable(Route.TAGS) {
                TagsScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.PROFILES) {
                ProfileListScreen(
                    onBack = { navController.popBackStack() },
                    onEditProfile = { id -> navController.navigate(Route.profileEdit(id)) }
                )
            }
            composable(
                route = Route.PROFILE_EDIT,
                arguments = listOf(navArgument("profileId") { type = NavType.StringType })
            ) {
                ProfileEditorScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
