package com.oleksandr.fastflow.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.R
import com.oleksandr.fastflow.ui.components.CapsuleButton
import com.oleksandr.fastflow.ui.components.CapsuleStyle
import com.oleksandr.fastflow.ui.components.DualProgressRing
import com.oleksandr.fastflow.ui.theme.AppTypography
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.Motion
import com.oleksandr.fastflow.ui.theme.rememberAnimationsEnabled

/**
 * Three-page introduction that also collects the permissions notifications
 * depend on (SPEC 5.3).
 *
 * The battery page exists because Samsung's "Sleeping apps" will otherwise
 * kill the receivers a 36-hour timer relies on (SPEC 8).
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current
    var page by remember { mutableIntStateOf(0) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { page = 2 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        DualProgressRing(
            outerProgress = when (page) { 0 -> 0.35f; 1 -> 0.7f; else -> 1f },
            outerColor = if (page == 2) palette.success else palette.fasting,
            innerProgress = 0f,
            innerColor = palette.eating,
            showInnerRing = false,
            animationsEnabled = rememberAnimationsEnabled(),
            diameter = 180.dp,
        ) {}

        Spacer(Modifier.height(40.dp))

        AnimatedContent(
            targetState = page,
            transitionSpec = {
                fadeIn(tween(Motion.CROSSFADE_MILLIS)) togetherWith
                    fadeOut(tween(Motion.CROSSFADE_MILLIS))
            },
            label = "onboardingPage",
        ) { current ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(current.titleRes()),
                    style = AppTypography.displaySmall,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(current.bodyRes()),
                    style = AppTypography.bodyLarge,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        PageDots(count = PAGE_COUNT, selected = page)

        Spacer(Modifier.height(20.dp))

        CapsuleButton(
            text = stringResource(page.actionRes()),
            onClick = {
                when (page) {
                    0 -> page = 1

                    1 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Below Android 13 notifications need no runtime grant.
                        page = 2
                    }

                    else -> {
                        openBatterySettings(context)
                        onFinish()
                    }
                }
            },
            style = CapsuleStyle.FILLED,
        )

        Spacer(Modifier.height(12.dp))

        if (page > 0) {
            CapsuleButton(
                text = stringResource(R.string.action_done),
                onClick = onFinish,
                style = CapsuleStyle.TINTED,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PageDots(count: Int, selected: Int) {
    val palette = LocalAppPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = if (index == selected) palette.fasting else palette.textTertiary,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

/**
 * Opens the battery screen for the app.
 *
 * Falls back to the app's own settings page: some OEMs do not expose the
 * standard battery-optimisation intent.
 */
private fun openBatterySettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    val fallback = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    runCatching { context.startActivity(intent) }
        .onFailure { runCatching { context.startActivity(fallback) } }
}

private const val PAGE_COUNT = 3

private fun Int.titleRes(): Int = when (this) {
    0 -> R.string.onboarding_1_title
    1 -> R.string.onboarding_2_title
    else -> R.string.onboarding_3_title
}

private fun Int.bodyRes(): Int = when (this) {
    0 -> R.string.onboarding_1_body
    1 -> R.string.onboarding_2_body
    else -> R.string.onboarding_3_body
}

private fun Int.actionRes(): Int = when (this) {
    0 -> R.string.action_next
    1 -> R.string.onboarding_2_action
    else -> R.string.onboarding_3_action
}
