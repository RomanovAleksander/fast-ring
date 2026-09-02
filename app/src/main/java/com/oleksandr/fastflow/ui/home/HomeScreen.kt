package com.oleksandr.fastflow.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.ui.components.PlaceholderScreen

@Composable
fun HomeScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.app_name),
        body = stringResource(R.string.state_idle),
    )
}
