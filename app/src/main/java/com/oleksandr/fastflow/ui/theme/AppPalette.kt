package com.oleksandr.fastflow.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour the app is allowed to draw with (SPEC 5.1).
 *
 * Composables must read colours from here through [LocalAppPalette]; a literal
 * `Color(0x…)` outside this package is a bug.
 */
@Immutable
data class AppPalette(
    val id: PaletteId,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val divider: Color,
    /** Active-fast ring and primary accent. */
    val fasting: Color,
    /** Darker end of the fasting sweep gradient. */
    val fastingGradientEnd: Color,
    /** Goal reached: deliberately a different hue from [fasting]. */
    val success: Color,
    /** Eating-window ring. */
    val eating: Color,
    /** Day finished below the 90 % threshold. */
    val partial: Color,
    /** Day with no fast at all. */
    val missed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
) {
    /** A day credited through the compensation rule reuses [success] plus a ↺ mark. */
    val compensated: Color get() = success

    /** Unfilled part of any progress ring. */
    fun trackOf(ring: Color): Color = ring.copy(alpha = TRACK_ALPHA)

    /** Background of a tinted (secondary) capsule button. */
    val tintedButtonBackground: Color get() = fasting.copy(alpha = TINTED_BUTTON_ALPHA)

    companion object {
        const val TRACK_ALPHA = 0.15f
        const val TINTED_BUTTON_ALPHA = 0.18f

        /** Soft glow drawn at the head of an active arc. */
        const val GLOW_ALPHA = 0.35f
    }
}

/** Palette choice persisted in DataStore (SPEC 4.1). */
enum class PaletteId {
    MINT,
    SYSTEM,
}

val MintPalette = AppPalette(
    id = PaletteId.MINT,
    background = Color(0xFF0B0F14),
    surface = Color(0xFF131A22),
    surfaceVariant = Color(0xFF1B2430),
    divider = Color(0xFF26303C),
    fasting = Color(0xFF63E6BE),
    fastingGradientEnd = Color(0xFF38D9A9),
    success = Color(0xFFC0EB75),
    eating = Color(0xFFFFC078),
    partial = Color(0xFFFFD43B),
    missed = Color(0xFFFF8787).copy(alpha = 0.8f),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF8B98A8),
    textTertiary = Color(0xFF48566A),
)

val SystemPalette = AppPalette(
    id = PaletteId.SYSTEM,
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    divider = Color(0xFF38383A),
    fasting = Color(0xFF0A84FF),
    fastingGradientEnd = Color(0xFF5E5CE6),
    success = Color(0xFF30D158),
    eating = Color(0xFFFF9F0A),
    partial = Color(0xFFFFD60A),
    missed = Color(0xFFFF453A).copy(alpha = 0.7f),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFF48484A),
)

fun paletteFor(id: PaletteId): AppPalette = when (id) {
    PaletteId.MINT -> MintPalette
    PaletteId.SYSTEM -> SystemPalette
}

val LocalAppPalette = staticCompositionLocalOf { MintPalette }
