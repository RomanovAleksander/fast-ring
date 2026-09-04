package com.oleksandr.fastflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.ui.theme.LocalAppPalette

/** One bar: how long a fast ran, and whether it counted. */
data class DurationBar(val millis: Long, val success: Boolean)

/**
 * Thin capsule bars with a dashed goal line (SPEC 5.3).
 *
 * Drawn on a Canvas — the spec rules out chart libraries.
 */
@Composable
fun DurationBarChart(
    bars: List<DurationBar>,
    goalMillis: Long,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    if (bars.isEmpty()) return

    // Scale to whichever is taller: the longest bar or the goal line.
    val maxMillis = maxOf(bars.maxOf { it.millis }, goalMillis).coerceAtLeast(1L)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
    ) {
        val barWidth = size.width / (bars.size * 2f)
        val gap = barWidth
        val bottom = size.height

        bars.forEachIndexed { index, bar ->
            val x = index * (barWidth + gap) + gap / 2f
            val barHeight = (bar.millis.toFloat() / maxMillis) * size.height
            drawLine(
                color = if (bar.success) palette.success else palette.partial,
                start = Offset(x + barWidth / 2f, bottom),
                end = Offset(x + barWidth / 2f, bottom - barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }

        val goalY = bottom - (goalMillis.toFloat() / maxMillis) * size.height
        drawLine(
            color = palette.textSecondary.copy(alpha = 0.6f),
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
        )
    }
}
