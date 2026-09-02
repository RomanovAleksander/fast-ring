package com.oleksandr.fastflow.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.ui.components.PlaceholderScreen

@Composable
fun StatsScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.screen_stats_title),
        body = stringResource(R.string.placeholder_stats),
    )
}
