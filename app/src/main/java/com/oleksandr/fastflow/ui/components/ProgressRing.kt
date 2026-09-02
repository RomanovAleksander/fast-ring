package com.oleksandr.fastflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.ui.theme.AppPalette
import com.oleksandr.fastflow.ui.theme.LocalAppPalette
import com.oleksandr.fastflow.ui.theme.Motion
import kotlin.math.cos
import kotlin.math.sin

/**
 * The double ring from SPEC 5.2.
 *
 * The outer ring tracks the fast, the inner one the eating window (or, during
 * overtime, the run to the next milestone). Progress is animated linearly over
 * exactly one second so the head glides instead of stepping each tick.
 */
@Composable
fun DualProgressRing(
    outerProgress: Float,
    outerColor: Color,
    innerProgress: Float,
    innerColor: Color,
    showInnerRing: Boolean,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = true,
    diameter: Dp = 300.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalAppPalette.current

    val animatedOuter by animateFloatAsState(
        targetValue = outerProgress.coerceIn(0f, 1f),
        animationSpec = if (animationsEnabled) Motion.ringProgress else tween(0),
        label = "outerProgress",
    )
    val animatedInner by animateFloatAsState(
        targetValue = innerProgress.coerceIn(0f, 1f),
        animationSpec = if (animationsEnabled) Motion.ringProgress else tween(0),
        label = "innerProgress",
    )
    // Reaching the goal is announced by colour, not only by motion (SPEC 5.1).
    val animatedOuterColor by animateColorAsState(
        targetValue = outerColor,
        animationSpec = tween(Motion.COLOR_SHIFT_MILLIS),
        label = "outerColor",
    )
    val animatedInnerColor by animateColorAsState(
        targetValue = innerColor,
        animationSpec = tween(Motion.EATING_RING_FILL_MILLIS),
        label = "innerColor",
    )

    // A faint highlight travels the track so the ring never looks frozen.
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimmerAngle by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.SHIMMER_SWEEP_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerAngle",
    )

    val innerFraction by animateFloatAsState(
        targetValue = if (showInnerRing) 1f else 0f,
        animationSpec = tween(Motion.EATING_RING_FILL_MILLIS),
        label = "innerVisibility",
    )

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerStroke = (if (showInnerRing) OUTER_STROKE_DP else EXTENDED_STROKE_DP).dp.toPx()
            val innerStroke = INNER_STROKE_DP.dp.toPx()
            val gap = RING_GAP_DP.dp.toPx()

            val outerRadius = (size.minDimension - outerStroke) / 2f
            val innerRadius = outerRadius - outerStroke / 2f - gap - innerStroke / 2f

            drawRing(
                radius = outerRadius,
                strokeWidth = outerStroke,
                progress = animatedOuter,
                color = animatedOuterColor,
                gradientEnd = palette.fastingGradientEnd,
                shimmerAngle = if (animationsEnabled) shimmerAngle else null,
            )
            drawTicks(outerRadius, outerStroke, animatedOuter, animatedOuterColor, palette)

            if (innerFraction > 0.01f && innerRadius > innerStroke) {
                drawRing(
                    radius = innerRadius,
                    strokeWidth = innerStroke,
                    progress = animatedInner,
                    color = animatedInnerColor.copy(alpha = innerFraction),
                    gradientEnd = animatedInnerColor,
                    shimmerAngle = null,
                )
            }
        }
        content()
    }
}

/** One ring: track, swept arc, and a glow riding the arc's head. */
private fun DrawScope.drawRing(
    radius: Float,
    strokeWidth: Float,
    progress: Float,
    color: Color,
    gradientEnd: Color,
    shimmerAngle: Float?,
) {
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2, radius * 2)

    // The track is a stroked full circle, not a filled disc.
    drawArc(
        color = color.copy(alpha = AppPalette.TRACK_ALPHA),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = stroke,
    )

    if (shimmerAngle != null) {
        drawArc(
            color = color.copy(alpha = SHIMMER_ALPHA),
            startAngle = shimmerAngle - 90f,
            sweepAngle = SHIMMER_SWEEP_DEGREES,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }

    if (progress <= 0f) return

    val sweep = progress * 360f
    drawArc(
        brush = Brush.sweepGradient(
            listOf(color, gradientEnd, color),
            center = center,
        ),
        startAngle = START_ANGLE,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = stroke,
    )

    // Glow at the head, so the movement reads at a glance (SPEC 5.2).
    val headRadians = Math.toRadians((START_ANGLE + sweep).toDouble())
    val head = Offset(
        x = center.x + radius * cos(headRadians).toFloat(),
        y = center.y + radius * sin(headRadians).toFloat(),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = AppPalette.GLOW_ALPHA), Color.Transparent),
            center = head,
            radius = strokeWidth * GLOW_RADIUS_FACTOR,
        ),
        radius = strokeWidth * GLOW_RADIUS_FACTOR,
        center = head,
    )
}

/** Six-degree ticks around the outer ring; passed ones are lit. */
private fun DrawScope.drawTicks(
    radius: Float,
    strokeWidth: Float,
    progress: Float,
    color: Color,
    palette: AppPalette,
) {
    val tickOuter = radius + strokeWidth / 2f + TICK_OFFSET_DP.dp.toPx()
    val tickInner = tickOuter - TICK_LENGTH_DP.dp.toPx()
    val passedDegrees = progress * 360f

    var degrees = 0f
    while (degrees < 360f) {
        val radians = Math.toRadians((START_ANGLE + degrees).toDouble())
        val cosine = cos(radians).toFloat()
        val sine = sin(radians).toFloat()
        val lit = degrees <= passedDegrees
        drawLine(
            color = if (lit) color.copy(alpha = 0.6f) else palette.textTertiary.copy(alpha = 0.35f),
            start = Offset(center.x + tickInner * cosine, center.y + tickInner * sine),
            end = Offset(center.x + tickOuter * cosine, center.y + tickOuter * sine),
            strokeWidth = TICK_WIDTH_DP.dp.toPx(),
            cap = StrokeCap.Round,
        )
        degrees += TICK_STEP_DEGREES
    }
}

/** Twelve o'clock: Canvas angles start at three o'clock. */
private const val START_ANGLE = -90f
private const val OUTER_STROKE_DP = 22
private const val INNER_STROKE_DP = 22
private const val EXTENDED_STROKE_DP = 28
private const val RING_GAP_DP = 10
private const val TICK_STEP_DEGREES = 6f
private const val TICK_LENGTH_DP = 5
private const val TICK_OFFSET_DP = 6
private const val TICK_WIDTH_DP = 1
private const val SHIMMER_ALPHA = 0.08f
private const val SHIMMER_SWEEP_DEGREES = 26f
private const val GLOW_RADIUS_FACTOR = 1.6f
