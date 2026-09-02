package com.oleksandr.fastflow.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.oleksandr.fastflow.R
import kotlinx.serialization.Serializable

/** Type-safe Navigation Compose routes. */
@Serializable
data object TimerRoute

@Serializable
data object HistoryRoute

@Serializable
data object StatsRoute

@Serializable
data object SettingsRoute

/** The four entries of the bottom tab bar, in order (SPEC 5.3). */
enum class TopLevelTab(
    val route: Any,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    TIMER(TimerRoute, R.string.tab_timer, R.drawable.ic_tab_timer),
    HISTORY(HistoryRoute, R.string.tab_history, R.drawable.ic_tab_history),
    STATS(StatsRoute, R.string.tab_stats, R.drawable.ic_tab_stats),
    SETTINGS(SettingsRoute, R.string.tab_settings, R.drawable.ic_tab_settings),
}
