package com.oleksandr.fastflow.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.oleksandr.fastflow.ui.theme.paletteFor

/**
 * The 1x1 tile: the ring alone.
 *
 * A single cell has no room for `HH:MM`, so the centre carries whole hours and
 * the ring carries the rest — fasting progress while a fast runs, the eating
 * window counting down after it.
 */
class FastFlowSmallWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetStateLoader.load(context)
        provideContent { Content(state) }
    }

    @Composable
    private fun Content(state: WidgetState) {
        val palette = paletteFor(state.palette)
        val ringColor: Color = ringColorFor(state.phase, palette)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(palette.background)
                .cornerRadius(18.dp)
                .padding(4.dp)
                .clickable(actionRunCallback<ToggleFastAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(
                    RingBitmap.draw(
                        sizePx = RING_PX,
                        progress = state.progress,
                        ringColor = ringColor.toArgb(),
                        trackColor = palette.trackOf(ringColor).toArgb(),
                        strokeWidthPx = RING_STROKE_PX,
                    ),
                ),
                contentDescription = state.label,
                modifier = GlanceModifier.size(RING_DP.dp),
            )
            Text(
                text = state.hours,
                style = TextStyle(
                    color = ColorProvider(palette.textPrimary),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }

    private companion object {
        const val RING_DP = 52
        const val RING_PX = 156
        const val RING_STROKE_PX = 14f
    }
}
