package com.oleksandr.fastflow.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Durations and curves from SPEC 5.4, in one place so screens stay consistent. */
object Motion {
    const val RING_TICK_MILLIS = 1000
    const val COLOR_SHIFT_MILLIS = 500
    const val EATING_RING_FILL_MILLIS = 800
    const val BUTTON_COLOR_MILLIS = 300
    const val CROSSFADE_MILLIS = 300
    const val TAB_CROSSFADE_MILLIS = 200
    const val DIGIT_FLIP_MILLIS = 200
    const val STAT_COUNT_MILLIS = 600
    const val CALENDAR_STAGGER_MILLIS = 15
    const val SHIMMER_SWEEP_MILLIS = 4000

    /** Ring progress: linear, so the head glides instead of stepping each second. */
    val ringProgress = tween<Float>(RING_TICK_MILLIS, easing = LinearEasing)

    /** Goal reached: closes with a slight overshoot (SPEC 5.2). */
    val goalOvershoot = spring<Float>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)

    val statCount = tween<Int>(STAT_COUNT_MILLIS, easing = FastOutSlowInEasing)
}

/**
 * True when the user asked the system to remove animations.
 *
 * SPEC 5.2 requires every endless animation (shimmer, glow pulse) to stop in
 * that case; one-shot transitions still run.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale != 0f
    }
}
