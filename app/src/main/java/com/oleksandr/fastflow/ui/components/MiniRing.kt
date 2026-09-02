package com.oleksandr.fastflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oleksandr.fastflow.domain.model.DayInfo
import com.oleksandr.fastflow.domain.model.DayStatus
import com.oleksandr.fastflow.ui.theme.AppPalette
import com.oleksandr.fastflow.ui.theme.LocalAppPalette

/**
 * The small status ring used in the week strip, the calendar and history.
 *
 * A ring rather than a dot, so the fill also shows how much of the goal the
 * day reached (SPEC 5.3).
 */
@Composable
fun MiniRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    strokeWidth: Dp = 3.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val radius = (this.size.minDimension - strokeWidth.toPx()) / 2f
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)

        drawArc(
            color = color.copy(alpha = AppPalette.TRACK_ALPHA),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
        val sweep = progress.coerceIn(0f, 1f) * 360f
        if (sweep > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }
    }
}

/** Colour a day is drawn in (SPEC 3.4 and 5.1). */
@Composable
fun colorForDay(info: DayInfo): Color {
    val palette = LocalAppPalette.current
    return when (info.status) {
        DayStatus.SUCCESS -> palette.success
        DayStatus.PARTIAL -> palette.partial
        DayStatus.ACTIVE -> palette.fasting
        DayStatus.MISSED -> palette.missed
        DayStatus.NONE, DayStatus.REST -> palette.textTertiary
    }
}
