package com.oleksandr.fastflow.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oleksandr.fastflow.ui.history.HistoryScreen
import com.oleksandr.fastflow.ui.home.HomeScreen
import com.oleksandr.fastflow.ui.onboarding.OnboardingScreen
import com.oleksandr.fastflow.ui.settings.SettingsScreen
import com.oleksandr.fastflow.ui.stats.StatsScreen
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.Motion

@Composable
fun FastFlowApp(
    onboardingDone: Boolean = true,
    onOnboardingComplete: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    // Permissions come first: without them the timer's notifications never
    // arrive, which is the one thing the app cannot compromise on (SPEC 3.3).
    if (!onboardingDone) {
        OnboardingScreen(onFinish = onOnboardingComplete)
        return
    }

    val palette = LocalAppPalette.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        containerColor = palette.background,
        // The app draws its own insets so the ring can sit under the status bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            IosTabBar(
                tabs = TopLevelTab.entries,
                isSelected = { tab ->
                    currentDestination?.hasRoute(tab.route::class) == true
                },
                onSelect = { tab -> navController.navigateToTab(tab) },
            )
        },
    ) { innerPadding ->
        // Tabs crossfade in place: an iOS tab bar never slides (SPEC 5.4).
        val fade = tween<Float>(Motion.TAB_CROSSFADE_MILLIS)
        NavHost(
            navController = navController,
            startDestination = TimerRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { fadeIn(fade) },
            exitTransition = { fadeOut(fade) },
            popEnterTransition = { fadeIn(fade) },
            popExitTransition = { fadeOut(fade) },
        ) {
            composable<TimerRoute> { HomeScreen() }
            composable<HistoryRoute> { HistoryScreen() }
            composable<StatsRoute> { StatsScreen() }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}

/** Switches tabs without stacking duplicates, keeping each tab's own state. */
private fun NavHostController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * iOS-style tab bar: icon over label, no pill indicator, no ripple.
 *
 * SPEC 5.1 asks for a blurred translucent background. Compose has no backdrop
 * blur primitive and third-party blur libraries are ruled out by CLAUDE.md, so
 * this uses the specified fallback — a 90 %-opaque background with a hairline
 * top divider.
 */
@Composable
private fun IosTabBar(
    tabs: List<TopLevelTab>,
    isSelected: (TopLevelTab) -> Boolean,
    onSelect: (TopLevelTab) -> Unit,
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background.copy(alpha = 0.9f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(palette.divider),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(49.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = isSelected(tab)
                val tint = if (selected) palette.fasting else palette.textSecondary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .selectable(
                            selected = selected,
                            onClick = { if (!selected) onSelect(tab) },
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(25.dp),
                    )
                    Text(
                        text = stringResource(tab.labelRes),
                        color = tint,
                        style = AppTypography.labelSmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
