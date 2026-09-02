package com.oleksandr.fastflow.ui.theme

import android.app.Activity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * The app theme.
 *
 * Dark only by design (SPEC 5.1): `isSystemInDarkTheme()` is deliberately not
 * consulted, and the status bar always uses light icons. Switching palettes
 * crossfades so the change is visible but not jarring (SPEC 3.5).
 */
@Composable
fun FastFlowTheme(
    paletteId: PaletteId = PaletteId.MINT,
    content: @Composable () -> Unit,
) {
    Crossfade(
        targetState = paletteFor(paletteId),
        animationSpec = tween(Motion.CROSSFADE_MILLIS),
        label = "palette",
    ) { palette ->
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = false
            }
        }

        CompositionLocalProvider(LocalAppPalette provides palette) {
            MaterialTheme(
                colorScheme = palette.toColorScheme(),
                typography = AppTypography,
                shapes = AppShapes,
                content = content,
            )
        }
    }
}

/**
 * Material components still read [MaterialTheme.colorScheme]; mapping the
 * palette onto it keeps stray defaults (ripples, sheet scrims) on-brand even
 * though app code paints from [LocalAppPalette].
 */
private fun AppPalette.toColorScheme() = darkColorScheme(
    primary = fasting,
    onPrimary = background,
    primaryContainer = fasting.copy(alpha = AppPalette.TINTED_BUTTON_ALPHA),
    onPrimaryContainer = fasting,
    secondary = eating,
    onSecondary = background,
    tertiary = success,
    onTertiary = background,
    background = background,
    onBackground = textPrimary,
    surface = surface,
    onSurface = textPrimary,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = textSecondary,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceVariant,
    outline = divider,
    outlineVariant = divider,
    error = missed,
    onError = background,
    scrim = background,
)
