package com.oleksandr.fastflow.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.ui.components.PlaceholderScreen

@Composable
fun SettingsScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.screen_settings_title),
        body = stringResource(R.string.placeholder_settings),
    )
}
