package com.oleksandr.fastflow.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.ui.components.PlaceholderScreen

@Composable
fun HistoryScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.screen_history_title),
        body = stringResource(R.string.placeholder_history),
    )
}
